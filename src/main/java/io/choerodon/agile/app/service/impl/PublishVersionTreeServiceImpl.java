package io.choerodon.agile.app.service.impl;

import io.choerodon.agile.api.vo.PublishVersionVO;
import io.choerodon.agile.api.vo.VersionTreeVO;
import io.choerodon.agile.app.service.PublishVersionTreeService;
import io.choerodon.agile.infra.dto.PublishVersionDTO;
import io.choerodon.agile.infra.dto.PublishVersionTreeClosureDTO;
import io.choerodon.agile.infra.mapper.PublishVersionMapper;
import io.choerodon.agile.infra.mapper.PublishVersionTreeClosureMapper;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.oauth.DetailsHelper;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author superlee
 * @since 2021-03-19
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class PublishVersionTreeServiceImpl implements PublishVersionTreeService {

    @Autowired
    private PublishVersionTreeClosureMapper publishVersionTreeClosureMapper;
    @Autowired
    private PublishVersionMapper publishVersionMapper;
    @Autowired
    private ModelMapper modelMapper;

    @Override
    public List<VersionTreeVO> tree(Set<Long> projectIds,
                                    Long organizationId,
                                    Set<Long> rootIds) {
        Set<PublishVersionTreeClosureDTO> versionTreeClosureSet =
                publishVersionTreeClosureMapper.selectDescendants(projectIds, organizationId, rootIds, null);
        Set<Long> childrenIds =
                versionTreeClosureSet
                        .stream()
                        .map(PublishVersionTreeClosureDTO::getDescendantId)
                        .collect(Collectors.toSet());
        Map<Long, PublishVersionDTO> publishVersionMap = new HashMap<>();
        if (!childrenIds.isEmpty()) {
            publishVersionMap.putAll(
                    publishVersionMapper.selectByIds(StringUtils.join(childrenIds, ","))
                            .stream()
                            .collect(Collectors.toMap(PublishVersionDTO::getId, Function.identity()))
            );
        }
        List<VersionTreeVO> result = new ArrayList<>();
        rootIds.forEach(x -> result.add(toTree(versionTreeClosureSet, publishVersionMap, x)));
        return result;
    }


    @Override
    public void add(Long projectId,
                    Long organizationId,
                    VersionTreeVO versionTreeVO) {
        Long operator = DetailsHelper.getUserDetails().getUserId();
        Long parentId = versionTreeVO.getId();
        Set<Long> childrenIds = new HashSet<>();
        validateData(versionTreeVO, childrenIds);
        Set<Long> ancestorIds =
                publishVersionTreeClosureMapper.selectAncestors(projectId, organizationId, parentId).stream()
                        .map(PublishVersionTreeClosureDTO::getAncestorId)
                        .collect(Collectors.toSet());
        Set<PublishVersionTreeClosureDTO> descendants = new HashSet<>();
        if (!childrenIds.isEmpty()) {
            descendants.addAll(publishVersionTreeClosureMapper.selectDescendants(new HashSet<>(Arrays.asList(projectId)), organizationId, childrenIds, null));
            validateCircularDependency(descendants, ancestorIds);
        }
        Set<PublishVersionTreeClosureDTO> descendantSet = buildDescendantByAncestor(descendants, ancestorIds, parentId);
        insertListIfNotExisted(operator, descendantSet, projectId, organizationId);
    }

    @Override
    public void delete(Long projectId,
                       Long organizationId,
                       VersionTreeVO versionTreeVO) {
        Long parentId = versionTreeVO.getId();
        Set<Long> childrenIds = new HashSet<>();
        validateData(versionTreeVO, childrenIds);
        Set<Long> ancestorIds =
                publishVersionTreeClosureMapper.selectAncestors(projectId, organizationId, parentId).stream()
                        .map(PublishVersionTreeClosureDTO::getAncestorId)
                        .collect(Collectors.toSet());
        Set<PublishVersionTreeClosureDTO> descendants = new HashSet<>();
        if (!childrenIds.isEmpty()) {
            descendants.addAll(buildDeleteList(projectId, organizationId, childrenIds, ancestorIds, parentId));
        }
        if (!descendants.isEmpty()) {
            publishVersionTreeClosureMapper.batchDelete(descendants, projectId, organizationId);
        }
    }

    @Override
    public List<PublishVersionVO> availablePublishVersion(Long projectId, Long organizationId, Long rootId) {
        PublishVersionDTO publishVersionDTO = new PublishVersionDTO();
        publishVersionDTO.setProjectId(projectId);
        publishVersionDTO.setOrganizationId(organizationId);
        List<PublishVersionDTO> list = publishVersionMapper.select(publishVersionDTO);
        Set<Long> ancestorIds =
                publishVersionTreeClosureMapper.selectAncestors(projectId, organizationId, rootId)
                        .stream().map(PublishVersionTreeClosureDTO::getAncestorId).collect(Collectors.toSet());
        Set<Long> directDescendantIds =
                publishVersionTreeClosureMapper.selectDescendants(new HashSet<>(Arrays.asList(projectId)), organizationId, new HashSet<>(Arrays.asList(rootId)), null)
                        .stream()
                        .map(PublishVersionTreeClosureDTO::getDescendantId)
                        .collect(Collectors.toSet());
        Set<Long> ignoredIds = new HashSet<>(ancestorIds);
        ignoredIds.addAll(directDescendantIds);
        List<PublishVersionVO> result = new ArrayList<>();
        list.forEach(x -> {
            if (!ignoredIds.contains(x.getId())) {
                PublishVersionVO vo = new PublishVersionVO();
                BeanUtils.copyProperties(x, vo);
                result.add(vo);
            }
        });
        return result;
    }

    @Override
    public List<PublishVersionVO> directDescendants(Long projectId, Long organizationId, Long rootId) {
        PublishVersionTreeClosureDTO dto = new PublishVersionTreeClosureDTO();
        dto.setProjectId(projectId);
        dto.setOrganizationId(organizationId);
        dto.setAncestorId(rootId);
        dto.setDescendantParent(rootId);
        Set<Long> publishVersionIds =
                publishVersionTreeClosureMapper
                        .select(dto)
                        .stream()
                        .map(PublishVersionTreeClosureDTO::getDescendantId)
                        .collect(Collectors.toSet());
        if (publishVersionIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<PublishVersionDTO> list = publishVersionMapper.selectByIds(StringUtils.join(publishVersionIds, ","));
        return modelMapper.map(list, new TypeToken<List<PublishVersionVO>>() {}.getType());
    }

    private List<PublishVersionTreeClosureDTO> buildDeleteList(Long projectId,
                                                               Long organizationId,
                                                               Set<Long> childrenIds,
                                                               Set<Long> ancestorIds,
                                                               Long parentId) {
        Set<PublishVersionTreeClosureDTO> descendants =
                publishVersionTreeClosureMapper.selectDescendants(new HashSet<>(Arrays.asList(projectId)), organizationId, childrenIds, null);
        Set<PublishVersionTreeClosureDTO> descendantSet = buildDescendantByAncestor(descendants, ancestorIds, parentId);
        //祖先节点下所有不包含自己及自己祖先的节点
        Set<Long> nodeWithoutAncestorIds =
                publishVersionTreeClosureMapper
                        .selectDescendants(new HashSet<>(Arrays.asList(projectId)), organizationId, ancestorIds, null)
                        .stream()
                        .filter(x -> !ancestorIds.contains(x.getDescendantId()))
                        .map(PublishVersionTreeClosureDTO::getDescendantId)
                        .collect(Collectors.toSet());
        Set<PublishVersionTreeClosureDTO> input = new HashSet<>();
        descendantSet.forEach(x -> {
            PublishVersionTreeClosureDTO dto = new PublishVersionTreeClosureDTO();
            BeanUtils.copyProperties(x, dto);
            dto.setId(null);
            dto.setAncestorId(null);
            input.add(dto);
        });
        //校验这些后代数据是否存在于非ancestorIds的节点下
        Set<PublishVersionTreeClosureDTO> ignoredList =
                publishVersionTreeClosureMapper.selectAncestorsByIds(input, nodeWithoutAncestorIds, projectId, organizationId);
        List<PublishVersionTreeClosureDTO> result = new ArrayList<>();
        descendantSet.forEach(x -> {
            boolean notContains = true;
            for (PublishVersionTreeClosureDTO dto : ignoredList) {
                notContains =
                        notContains
                                && !(Objects.equals(x.getDescendantId(), dto.getDescendantId())
                                && Objects.equals(x.getDescendantParent(), dto.getDescendantParent()));
            }
            if (notContains) {
                result.add(x);
            }
        });
        return result;
    }

    private void insertListIfNotExisted(Long operator,
                                        Set<PublishVersionTreeClosureDTO> descendantSet,
                                        Long projectId,
                                        Long organizationId) {
        Set<PublishVersionTreeClosureDTO> existedList = publishVersionTreeClosureMapper.selectInList(descendantSet, projectId, organizationId);
        List<PublishVersionTreeClosureDTO> insertList = new ArrayList<>();
        descendantSet.forEach(x -> {
            if (!existedList.contains(x)) {
                insertList.add(x);
            }
        });
        if (!insertList.isEmpty()) {
            publishVersionTreeClosureMapper.batchInsert(new HashSet<>(insertList), operator);
        }
    }

    private Set<PublishVersionTreeClosureDTO> buildDescendantByAncestor(Set<PublishVersionTreeClosureDTO> descendants,
                                                                        Set<Long> ancestorIds,
                                                                        Long parentId) {
        Set<PublishVersionTreeClosureDTO> result = new HashSet<>();
        descendants.forEach(x ->
                ancestorIds.forEach(y -> {
                    Long descendantParent = x.getDescendantParent();
                    PublishVersionTreeClosureDTO dto = new PublishVersionTreeClosureDTO();
                    BeanUtils.copyProperties(x, dto);
                    dto.setId(null);
                    dto.setAncestorId(y);
                    if (Objects.equals(0L, descendantParent)) {
                        dto.setDescendantParent(parentId);
                    }
                    result.add(dto);
                }));
        return result;
    }

    private void validateCircularDependency(Set<PublishVersionTreeClosureDTO> descendants,
                                            Set<Long> ancestorIds) {
        Set<Long> descendantIds =
                descendants.stream().map(PublishVersionTreeClosureDTO::getDescendantId).collect(Collectors.toSet());
        ancestorIds.forEach(x -> {
            if (descendantIds.contains(x)) {
                throw new CommonException("error.version.tree.circular.dependency");
            }
        });
    }

    private void validateData(VersionTreeVO versionTreeVO,
                              Set<Long> childrenIds) {
        Long parentId = versionTreeVO.getId();
        PublishVersionDTO parent = publishVersionMapper.selectByPrimaryKey(parentId);
        if (parent == null) {
            throw new CommonException("error.parent.node.not.existed");
        }
        List<VersionTreeVO> childrenList = versionTreeVO.getChildren();
        if (ObjectUtils.isEmpty(childrenList)) {
            throw new CommonException("error.children.node.empty");
        }
        //去重
        Set<VersionTreeVO> children = new HashSet<>(childrenList);
        children.forEach(x -> {
            Long id = x.getId();
            if (id == null) {
                throw new CommonException("error.children.node.id.null");
            }
            childrenIds.add(id);
        });
        Set<Long> existedIds = new HashSet<>();
        if (!childrenIds.isEmpty()) {
            existedIds.addAll(publishVersionMapper
                    .selectByIds(StringUtils.join(childrenIds, ","))
                    .stream()
                    .map(PublishVersionDTO::getId)
                    .collect(Collectors.toSet()));
        }
        List<Long> illegalIds = new ArrayList<>();
        if (!Objects.equals(childrenIds.size(), existedIds.size())) {
            childrenIds.forEach(x -> {
                if (!existedIds.contains(x)) {
                    illegalIds.add(x);
                }
            });
        }
        if (!illegalIds.isEmpty()) {
            throw new CommonException("error.illegal.child.node.id." + StringUtils.join(illegalIds, ","));
        }
    }

    private VersionTreeVO toTree(Set<PublishVersionTreeClosureDTO> versionTreeClosureSet,
                                 Map<Long, PublishVersionDTO> publishVersionMap,
                                 Long rootId) {
        PublishVersionDTO publishVersionDTO = publishVersionMap.get(rootId);
        if (publishVersionDTO == null) {
            throw new CommonException("error.program.version.not.existed." + rootId);
        }
        VersionTreeVO root = convertToVersionTree(publishVersionDTO, rootId);
        processChildNodes(root, versionTreeClosureSet, publishVersionMap);
        return root;
    }

    private void processChildNodes(VersionTreeVO root,
                                   Set<PublishVersionTreeClosureDTO> versionTreeClosureSet,
                                   Map<Long, PublishVersionDTO> publishVersionMap) {
        Long rootId = root.getId();
        List<VersionTreeVO> children = root.getChildren();
        versionTreeClosureSet.forEach(x -> {
            Long parentId = x.getDescendantParent();
            Long childId = x.getDescendantId();
            if (Objects.equals(rootId, parentId)) {
                VersionTreeVO child = convertToVersionTree(publishVersionMap.get(childId), childId);
                children.add(child);
                processChildNodes(child, versionTreeClosureSet, publishVersionMap);
            }
        });
    }

    private VersionTreeVO convertToVersionTree(PublishVersionDTO publishVersionDTO, Long id) {
        VersionTreeVO versionTreeVO = new VersionTreeVO();
        versionTreeVO.setType("app");
        versionTreeVO.setChildren(new ArrayList<>());
        versionTreeVO.setId(id);
        if (publishVersionDTO == null) {
            return versionTreeVO;
        }
        String name = publishVersionDTO.getArtifactId();
        versionTreeVO.setName(name);
        versionTreeVO.setVersion(publishVersionDTO.getVersion());
        versionTreeVO.setVersionAlias(publishVersionDTO.getVersionAlias());
        return versionTreeVO;
    }
}
