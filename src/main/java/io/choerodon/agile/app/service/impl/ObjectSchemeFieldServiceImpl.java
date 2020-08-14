package io.choerodon.agile.app.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import io.choerodon.agile.infra.dto.*;
import io.choerodon.agile.infra.enums.*;
import io.choerodon.agile.infra.mapper.*;
import io.choerodon.agile.infra.utils.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.choerodon.agile.api.vo.*;
import io.choerodon.agile.app.service.*;
import io.choerodon.core.exception.CommonException;
import org.springframework.util.ObjectUtils;


/**
 * @author shinan.chen
 * @since 2019/3/29
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ObjectSchemeFieldServiceImpl implements ObjectSchemeFieldService {
    private static final String ERROR_FIELD_ILLEGAL = "error.field.illegal";
    private static final String ERROR_FIELD_CREATE = "error.field.create";
    private static final String ERROR_FIELD_NOTFOUND = "error.field.notFound";
    private static final String ERROR_FIELD_UPDATE = "error.field.update";
    private static final String ERROR_SCHEMECODE_ILLEGAL = "error.schemeCode.illegal";
    private static final String ERROR_FIELDTYPE_ILLEGAL = "error.fieldType.illegal";
    private static final String ERROR_FIELD_NAMEEXIST = "error.field.nameExist";
    private static final String ERROR_FIELD_CODEEXIST = "error.field.codeExist";
    @Autowired
    private ObjectSchemeFieldMapper objectSchemeFieldMapper;
    @Autowired
    private ObjectSchemeMapper objectSchemeMapper;
    @Autowired
    private FieldOptionService fieldOptionService;
    @Autowired
    private PageFieldService pageFieldService;
    @Autowired
    private FieldValueService fieldValueService;
    @Autowired
    private LookupValueMapper lookupValueMapper;
    @Autowired
    private FieldDataLogService fieldDataLogService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private ObjectSchemeFieldExtendMapper objectSchemeFieldExtendMapper;
    @Autowired
    private IssueTypeService issueTypeService;
    @Autowired
    private IssueTypeFieldMapper issueTypeFieldMapper;

    @Override
    public ObjectSchemeFieldDTO baseCreate(ObjectSchemeFieldDTO field,
                                           String[] contexts,
                                           String issueTypeForRank) {
        Long organizationId = field.getOrganizationId();
        Long projectId = field.getProjectId();
        field.setSystem(false);
        field.setRequired(false);
        if (objectSchemeFieldMapper.insert(field) != 1) {
            throw new CommonException(ERROR_FIELD_CREATE);
        }
        //  创建object_scheme_field_extend
        Map<String, Long> issueTypeMap = issueTypeService.queryIssueTypeMap(organizationId);
        if (ObjectSchemeFieldContext.isGlobal(contexts)) {
            getInsertExtendList(organizationId, projectId, field, ObjectSchemeFieldContext.ISSUE_TYPES, issueTypeMap, issueTypeForRank);
        } else {
            getInsertExtendList(organizationId, projectId, field, contexts, issueTypeMap, issueTypeForRank);
        }
        return objectSchemeFieldMapper.selectByPrimaryKey(field.getId());
    }

    private void getInsertExtendList(Long organizationId,
                                     Long projectId,
                                     ObjectSchemeFieldDTO field,
                                     String[] contexts,
                                     Map<String, Long> issueTypeMap,
                                     String issueTypeForRank) {
        String minRank = null;
        Long fieldId = field.getId();
        String rank = field.getRank();
        for (String ctx : contexts) {
            ObjectSchemeFieldExtendDTO dto = new ObjectSchemeFieldExtendDTO();
            dto.setIssueType(ctx);
            dto.setProjectId(projectId);
            dto.setOrganizationId(organizationId);
            dto.setFieldId(fieldId);
            Long issueTypeId = issueTypeMap.get(ctx);
            if (objectSchemeFieldExtendMapper.select(dto).isEmpty()
                    && !ObjectUtils.isEmpty(issueTypeId)) {
                dto.setRequired(true);
                dto.setCreated(true);
                dto.setEdited(true);
                dto.setIssueTypeId(issueTypeId);
                if (Objects.equals(ctx, issueTypeForRank)
                        && !StringUtils.isEmpty(rank)) {
                    dto.setRank(rank);
                } else {
                    minRank = getMinRank(organizationId, projectId, ctx, minRank);
                    dto.setRank(minRank);
                }
                objectSchemeFieldExtendMapper.insert(dto);
            }
        }
    }

    private String getMinRank(Long organizationId, Long projectId, String ctx, String minRank) {
        if (ObjectUtils.isEmpty(minRank)) {
            String rank = objectSchemeFieldExtendMapper.selectMinRank(organizationId, projectId, ctx);
            if (ObjectUtils.isEmpty(rank)) {
                minRank =  RankUtil.mid();
            } else {
                minRank = rank;
            }
        }
        return RankUtil.genPre(minRank);
    }

    @Override
    public void baseUpdate(ObjectSchemeFieldDTO field) {
        if (objectSchemeFieldMapper.updateByPrimaryKeySelective(field) != 1) {
            throw new CommonException(ERROR_FIELD_UPDATE);
        }
    }

    @Override
    public ObjectSchemeFieldDTO baseQueryById(Long organizationId, Long projectId, Long fieldId) {
        ObjectSchemeFieldDTO field = selectOneByFieldId(organizationId, projectId, fieldId);
        if (!field.getOrganizationId().equals(organizationId) && !field.getOrganizationId().equals(0L)) {
            throw new CommonException(ERROR_FIELD_ILLEGAL);
        }
        if (field.getProjectId() != null && !field.getProjectId().equals(projectId) && !field.getProjectId().equals(0L)) {
            throw new CommonException(ERROR_FIELD_ILLEGAL);
        }
        return field;
    }

    private ObjectSchemeFieldDTO selectOneByFieldId(Long organizationId,
                                                    Long projectId,
                                                    Long fieldId) {
        List<ObjectSchemeFieldDTO> dtoList =
                objectSchemeFieldMapper.selectByOptions(organizationId, projectId, ObjectSchemeCode.AGILE_ISSUE, fieldId, null);
        if (dtoList.isEmpty()) {
            throw new CommonException(ERROR_FIELD_NOTFOUND);
        } else {
            return dtoList.get(0);
        }
    }

    @Override
    public List<ObjectSchemeFieldDTO> listQuery(Long organizationId, Long projectId, ObjectSchemeFieldSearchVO searchDTO) {
        return objectSchemeFieldMapper.listQuery(organizationId, projectId, searchDTO);
    }

    @Override
    public ObjectSchemeFieldDTO queryByFieldCode(Long organizationId, Long projectId, String fieldCode) {
        return objectSchemeFieldMapper.queryByFieldCode(organizationId, projectId, fieldCode);
    }

    @Override
    public Map<String, Object> listQuery(Long organizationId, Long projectId, String schemeCode) {
        Map<String, Object> result = new HashMap<>(2);
        if (!EnumUtil.contain(ObjectSchemeCode.class, schemeCode)) {
            throw new CommonException(ERROR_SCHEMECODE_ILLEGAL);
        }
        createSystemFieldIfNotExisted(organizationId);
        List<ObjectSchemeFieldDTO> fields = objectSchemeFieldMapper.selectByOptions(organizationId, projectId, schemeCode, null, null);
        List<ObjectSchemeFieldVO> fieldViews = new ArrayList<>();
        fields.forEach(f -> {
            ObjectSchemeFieldVO vo = modelMapper.map(f, ObjectSchemeFieldVO.class);
            List<ObjectSchemeFieldExtendDTO> extendList = f.getExtendFields();
            List<String> issueTypes = new ArrayList<>();
            List<String> issueTypeNames = new ArrayList<>();
            String requiredScope =
                    processIssueTyeAndRequiredScope(issueTypes, issueTypeNames, true, extendList);
            vo.setContext(String.join(",", issueTypes));
            vo.setContextName(String.join(",", issueTypeNames));
            vo.setRequiredScope(requiredScope);
            fieldViews.add(vo);
        });
        ObjectSchemeDTO select = new ObjectSchemeDTO();
        select.setSchemeCode(schemeCode);
        result.put("name", objectSchemeMapper.selectOne(select).getName());
        result.put("content", fieldViews);
        return result;
    }

    private void createSystemFieldIfNotExisted(Long organizationId) {
        if (objectSchemeFieldExtendMapper.selectExtendField(null,organizationId, null, null).isEmpty()) {
            ObjectSchemeFieldDTO dto = new ObjectSchemeFieldDTO();
            dto.setSystem(true);
            List<ObjectSchemeFieldDTO> systemFields = objectSchemeFieldMapper.select(dto);
            systemFields.forEach(field -> {
                String context = field.getContext();
                List<IssueTypeDTO> issueTypes = convertContextToIssueTypes(context, organizationId);
                String code = field.getCode();
                Boolean required = field.getRequired();
                Boolean created = Optional.ofNullable(InitPageFieldE.AgileIssueCreateE.getDisplayByCode(code)).orElse(false);
                Boolean edited = Optional.ofNullable(InitPageFieldE.AgileIssueEditE.getDisplayByCode(code)).orElse(false);
                issueTypes.forEach(issueType -> {
                    ObjectSchemeFieldExtendDTO extendField = new ObjectSchemeFieldExtendDTO();
                    extendField.setFieldId(field.getId());
                    extendField.setOrganizationId(organizationId);
                    extendField.setIssueType(issueType.getTypeCode());
                    extendField.setIssueTypeId(issueType.getId());
                    extendField.setRequired(required);
                    extendField.setCreated(created);
                    extendField.setEdited(edited);
                    extendField.setRank(getMinRank(organizationId, null, issueType.getTypeCode(), null));
                    objectSchemeFieldExtendMapper.insertSelective(extendField);
                });

            });
        }
    }

    private List<IssueTypeDTO> convertContextToIssueTypes(String context, Long organizationId) {
        List<IssueTypeDTO> result = new ArrayList<>();
        Map<String, Long> issueTypeMap = issueTypeService.queryIssueTypeMap(organizationId);
        String[] contextArray = context.split(",");
        if (ObjectSchemeFieldContext.isGlobal(contextArray)) {
            for (Map.Entry<String, Long> entry : issueTypeMap.entrySet()) {
                IssueTypeDTO dto = new IssueTypeDTO();
                dto.setId(entry.getValue());
                dto.setTypeCode(entry.getKey());
                result.add(dto);
            }
        } else {
            for (String ctx : contextArray) {
                Long id = issueTypeMap.get(ctx);
                if (ObjectUtils.isEmpty(id)) {
                    continue;
                }
                IssueTypeDTO dto = new IssueTypeDTO();
                dto.setId(id);
                dto.setTypeCode(ctx);
                result.add(dto);
            }
        }
        return result;
    }

    private String processIssueTyeAndRequiredScope(List<String> issueTypes,
                                                 List<String> issueTypeNames,
                                                 boolean resetIssueType,
                                                 List<ObjectSchemeFieldExtendDTO> extendList) {
        boolean allIsRequired = true;
        boolean allIsNotRequired = false;
        for (ObjectSchemeFieldExtendDTO e: extendList) {
            issueTypes.add(e.getIssueType());
            issueTypeNames.add(e.getIssueTypeName());
            allIsRequired = allIsRequired && e.getRequired();
            allIsNotRequired = allIsNotRequired || e.getRequired();
        }
        if (resetIssueType && ObjectSchemeFieldContext.containsAllIssueTypes(issueTypes)) {
            issueTypes.clear();
            issueTypeNames.clear();
            issueTypes.add(ObjectSchemeFieldContext.GLOBAL);
            issueTypeNames.add("全部类型");
        }
        if (allIsRequired) {
            return ObjectSchemeFieldRequiredScope.ALL.name();
        } else if (!allIsNotRequired) {
            return ObjectSchemeFieldRequiredScope.NONE.name();
        } else {
            return ObjectSchemeFieldRequiredScope.PART.name();
        }
    }

    @Override
    public ObjectSchemeFieldDetailVO create(Long organizationId,
                                            Long projectId,
                                            ObjectSchemeFieldCreateVO fieldCreateDTO,
                                            String issueTypeForRank) {
        if (!EnumUtil.contain(FieldType.class, fieldCreateDTO.getFieldType())) {
            throw new CommonException(ERROR_FIELDTYPE_ILLEGAL);
        }
        if (checkName(organizationId, projectId, fieldCreateDTO.getName(), fieldCreateDTO.getSchemeCode())) {
            throw new CommonException(ERROR_FIELD_NAMEEXIST);
        }
        if (checkCode(organizationId, projectId, fieldCreateDTO.getCode(), fieldCreateDTO.getSchemeCode())) {
            throw new CommonException(ERROR_FIELD_CODEEXIST);
        }

        String[] contexts = fieldCreateDTO.getContext();
        if (ObjectUtils.isEmpty(contexts)) {
            throw new CommonException("error.filed.context.empty");
        }
        ObjectSchemeFieldContext.isIllegalContexts(contexts);

        ObjectSchemeFieldDTO field = modelMapper.map(fieldCreateDTO, ObjectSchemeFieldDTO.class);
        field.setContext(Arrays.asList(fieldCreateDTO.getContext()).stream().collect(Collectors.joining(",")));
        field.setOrganizationId(organizationId);
        field.setProjectId(projectId);

        String defaultValue = tryDecryptDefaultValue(field.getDefaultValue());
        if (defaultValue != null) {
            field.setDefaultValue(defaultValue);
        }
        field = baseCreate(field, contexts, issueTypeForRank);

        //处理字段选项
        if (fieldCreateDTO.getFieldOptions() != null) {
            String defaultIds = fieldOptionService.handleFieldOption(organizationId, field.getId(), fieldCreateDTO.getFieldOptions());
            if (defaultIds != null && !"".equals(defaultIds)) {
                field.setDefaultValue(defaultIds);
                objectSchemeFieldMapper.updateOptional(field, "defaultValue");
            }
        }

        return queryById(organizationId, projectId, field.getId());
    }

    @Override
    public ObjectSchemeFieldDetailVO queryById(Long organizationId, Long projectId, Long fieldId) {
        ObjectSchemeFieldDTO field = baseQueryById(organizationId, projectId, fieldId);
        List<ObjectSchemeFieldExtendDTO> extendList = field.getExtendFields();
        List<String> issueTypes = new ArrayList<>();
        List<String> issueTypeNames = new ArrayList<>();
        String requiredScope =
                processIssueTyeAndRequiredScope(issueTypes, issueTypeNames, false, extendList);
        ObjectSchemeFieldDetailVO fieldDetailDTO = modelMapper.map(field, ObjectSchemeFieldDetailVO.class);
        fieldDetailDTO.setContext(issueTypes.toArray(new String[issueTypes.size()]));
        fieldDetailDTO.setRequiredScope(requiredScope);
        //获取字段选项，并设置默认值
        List<FieldOptionVO> fieldOptions = fieldOptionService.queryByFieldId(organizationId, fieldId);
        if (!fieldOptions.isEmpty()) {
            if (!ObjectUtils.isEmpty(field.getDefaultValue())) {
                List<String> defaultIds = Arrays.asList(field.getDefaultValue().split(","));
                fieldOptions.forEach(fieldOption -> {
                    if (defaultIds.contains(fieldOption.getId().toString())) {
                        fieldOption.setIsDefault(true);
                    } else {
                        fieldOption.setIsDefault(false);
                    }
                });
                List<String> encryptList = EncryptionUtils.encryptListToStr(defaultIds);
                fieldDetailDTO.setDefaultValue(StringUtils.join(encryptList.toArray(),","));
            } else {
                fieldOptions.forEach(fieldOption -> {
                    fieldOption.setIsDefault(false);
                });
            }
            fieldDetailDTO.setFieldOptions(fieldOptions);
        }
        FieldValueUtil.handleDefaultValue(fieldDetailDTO);
        return fieldDetailDTO;
    }

    @Override
    public void delete(Long organizationId, Long projectId, Long fieldId) {
        ObjectSchemeFieldDTO field = baseQueryById(organizationId, projectId, fieldId);
        //组织层无法删除项目层
        if (projectId == null && field.getProjectId() != null) {
            throw new CommonException(ERROR_FIELD_ILLEGAL);
        }
        //项目层无法删除组织层
        if (projectId != null && field.getProjectId() == null) {
            throw new CommonException(ERROR_FIELD_ILLEGAL);
        }
        //无法删除系统字段
        if (field.getSystem()) {
            throw new CommonException(ERROR_FIELD_ILLEGAL);
        }

        objectSchemeFieldMapper.cascadeDelete(organizationId, projectId, fieldId);
        //删除字段值
        fieldValueService.deleteByFieldId(fieldId);
        //删除日志
        fieldDataLogService.deleteByFieldId(projectId, fieldId);
    }

    @Override
    public ObjectSchemeFieldDetailVO update(Long organizationId, Long projectId, Long fieldId, ObjectSchemeFieldUpdateVO updateDTO) {
        //处理字段选项
        if (updateDTO.getFieldOptions() != null) {
            String defaultIds = fieldOptionService.handleFieldOption(organizationId, fieldId, updateDTO.getFieldOptions());
            if (defaultIds != null && !"".equals(defaultIds)) {
                updateDTO.setDefaultValue(defaultIds);
            }
        }
        ObjectSchemeFieldDTO update = modelMapper.map(updateDTO, ObjectSchemeFieldDTO.class);
        //处理context
        String[] contexts = updateDTO.getContext();
        updateFieldIssueType(organizationId, projectId, fieldId, contexts, updateDTO.getRequired());
        String defaultValue = tryDecryptDefaultValue(update.getDefaultValue());
        if (defaultValue != null) {
            update.setDefaultValue(defaultValue);
        }
        update.setId(fieldId);
        baseUpdate(update);
        return queryById(organizationId, projectId, fieldId);
    }

    private void updateFieldIssueType(Long organizationId,
                                      Long projectId,
                                      Long fieldId,
                                      String[] contexts,
                                      Boolean required) {
        if (ObjectUtils.isEmpty(contexts)) {
            throw new CommonException("error.field.context.empty");
        }
        ObjectSchemeFieldContext.isIllegalIssueTypes(contexts);
        List<String> contextList = Arrays.asList(contexts);
        ObjectSchemeFieldDTO field =
                selectOneByFieldId(organizationId, projectId, fieldId);
        List<ObjectSchemeFieldExtendDTO> intersection = new ArrayList<>();
        List<ObjectSchemeFieldExtendDTO> deleteList = new ArrayList<>();
        Set<String> insertSet = new HashSet<>();
        filterByIssueType(intersection, deleteList, insertSet, contextList, field);

        dealWithExtendFields(organizationId, projectId, fieldId, required, intersection, deleteList, insertSet);
    }

    private void dealWithExtendFields(Long organizationId, Long projectId, Long fieldId, Boolean required, List<ObjectSchemeFieldExtendDTO> intersection, List<ObjectSchemeFieldExtendDTO> deleteList, Set<String> insertSet) {
        boolean onProjectLevel = (projectId != null);
        Map<String, Long> issueTypeMap = issueTypeService.queryIssueTypeMap(organizationId);
        if (onProjectLevel) {
            deleteList.forEach(d -> objectSchemeFieldExtendMapper.deleteByPrimaryKey(d));
            insertSet.forEach(i -> insertObjectSchemeFieldExtend(organizationId, projectId, fieldId, required, issueTypeMap, i, true, true));
            intersection.forEach(i -> {
                if (!Objects.equals(i.getRequired(), required)) {
                    i.setRequired(required);
                    if (objectSchemeFieldExtendMapper.updateByPrimaryKeySelective(i) != 1) {
                        throw new CommonException("error.extend.field.update");
                    }
                }
            });
        } else {
            //组织层新增或删除，项目层数据同时新增或删除
            deleteList.forEach(d -> {
                String issueType = d.getIssueType();
                ObjectSchemeFieldExtendDTO target = new ObjectSchemeFieldExtendDTO();
                target.setIssueType(issueType);
                target.setOrganizationId(organizationId);
                target.setFieldId(fieldId);
                objectSchemeFieldExtendMapper.delete(target);
            });
            intersection.forEach(i ->
                objectSchemeFieldExtendMapper.batchUpdateRequired(i.getIssueType(), organizationId, fieldId, required)
            );
            //查询该组织下已经配置过的项目，这些项目要级联创建字段类型
            Set<Long> projectIds =
                    objectSchemeFieldExtendMapper.selectProjectIdsByOrganizationId(organizationId);
            insertSet.forEach(i -> {
                insertObjectSchemeFieldExtend(organizationId, null, fieldId, required, issueTypeMap, i, true, true);
                projectIds.forEach(p -> insertObjectSchemeFieldExtend(organizationId, p, fieldId, required, issueTypeMap, i, true, true));
            });
        }
    }

    private void filterByIssueType(List<ObjectSchemeFieldExtendDTO> intersection,
                                   List<ObjectSchemeFieldExtendDTO> deleteList,
                                   Set<String> insertSet,
                                   List<String> contextList,
                                   ObjectSchemeFieldDTO field) {
        List<ObjectSchemeFieldExtendDTO> extendList = field.getExtendFields();
        //交集
        extendList.forEach(e -> {
            if (contextList.contains(e.getIssueType())) {
                intersection.add(e);
            }
        });
        Set<String> intersectionIssueTypes =
                intersection.stream().map(ObjectSchemeFieldExtendDTO::getIssueType).collect(Collectors.toSet());
        //删除的类型
        extendList.forEach(e -> {
            if (!intersectionIssueTypes.contains(e.getIssueType())) {
                deleteList.add(e);
            }
        });
        //插入的类型
        contextList.forEach(c -> {
            if (!intersectionIssueTypes.contains(c)) {
                insertSet.add(c);
            }
        });
    }

    private void insertObjectSchemeFieldExtend(Long organizationId,
                                               Long projectId,
                                               Long fieldId,
                                               Boolean required,
                                               Map<String, Long> issueTypeMap,
                                               String issueType,
                                               Boolean created,
                                               Boolean edited) {
        ObjectSchemeFieldExtendDTO dto = new ObjectSchemeFieldExtendDTO();
        dto.setIssueType(issueType);
        dto.setFieldId(fieldId);
        dto.setOrganizationId(organizationId);

        List<ObjectSchemeFieldExtendDTO> existedList;
        if (ObjectUtils.isEmpty(projectId)) {
            existedList = objectSchemeFieldExtendMapper.selectExtendField(issueType, organizationId, fieldId, null);
        } else {
            dto.setProjectId(projectId);
            existedList = objectSchemeFieldExtendMapper.select(dto);
        }
        if (existedList.isEmpty()) {
            dto.setIssueTypeId(issueTypeMap.get(issueType));
            dto.setRequired(required);
            dto.setCreated(created);
            dto.setEdited(edited);
            dto.setRank(getMinRank(organizationId, projectId, issueType,null));
            objectSchemeFieldExtendMapper.insertSelective(dto);
        } else {
            ObjectSchemeFieldExtendDTO existedExtendField = existedList.get(0);
            existedExtendField.setCreated(Optional.ofNullable(created).orElse(true));
            existedExtendField.setRequired(required);
            existedExtendField.setEdited(Optional.ofNullable(edited).orElse(true));
            if (objectSchemeFieldExtendMapper.updateByPrimaryKeySelective(existedExtendField) != 1) {
                throw new CommonException("error.extend.field.update");
            }
        }
    }

    private String tryDecryptDefaultValue(String defaultValue) {
        try {
            return EncryptionUtils.decrypt(defaultValue);
        } catch (Exception e) {
            //do nothing
        }
        return null;
    }

    @Override
    public Boolean checkName(Long organizationId, Long projectId, String name, String schemeCode) {
        if (!EnumUtil.contain(ObjectSchemeCode.class, schemeCode)) {
            throw new CommonException(ERROR_SCHEMECODE_ILLEGAL);
        }
        ObjectSchemeFieldSearchVO search = new ObjectSchemeFieldSearchVO();
        search.setName(name);
        search.setSchemeCode(schemeCode);
        return !listQuery(organizationId, projectId, search).isEmpty();
    }

    @Override
    public Boolean checkCode(Long organizationId, Long projectId, String code, String schemeCode) {
        if (!EnumUtil.contain(ObjectSchemeCode.class, schemeCode)) {
            throw new CommonException(ERROR_SCHEMECODE_ILLEGAL);
        }
        ObjectSchemeFieldSearchVO search = new ObjectSchemeFieldSearchVO();
        search.setCode(code);
        search.setSchemeCode(schemeCode);
        return !listQuery(organizationId, projectId, search).isEmpty();
    }

    @Override
    public List<AgileIssueHeadVO> getIssueHeadForAgile(Long organizationId, Long projectId, String schemeCode) {
        if (!EnumUtil.contain(ObjectSchemeCode.class, schemeCode)) {
            throw new CommonException(ERROR_SCHEMECODE_ILLEGAL);
        }
        ObjectSchemeFieldSearchVO searchDTO = new ObjectSchemeFieldSearchVO();
        searchDTO.setSchemeCode(schemeCode);
        List<ObjectSchemeFieldDTO> objectSchemeFields = listQuery(organizationId, projectId, searchDTO)
                .stream().filter(objectSchemeField -> !objectSchemeField.getSystem()).collect(Collectors.toList());
        List<AgileIssueHeadVO> agileIssueHeadDTOS = new ArrayList<>();
        objectSchemeFields.forEach(objectSchemeField -> {
            AgileIssueHeadVO agileIssueHeadDTO = new AgileIssueHeadVO();
            agileIssueHeadDTO.setTitle(objectSchemeField.getName());
            agileIssueHeadDTO.setCode(objectSchemeField.getCode());
            agileIssueHeadDTO.setSortId(objectSchemeField.getCode());
            agileIssueHeadDTO.setFieldType(objectSchemeField.getFieldType());
            agileIssueHeadDTOS.add(agileIssueHeadDTO);
        });
        return agileIssueHeadDTOS;
    }

    @Override
    public List<ObjectSchemeFieldDetailVO> queryCustomFieldList(Long projectId) {
        List<ObjectSchemeFieldDetailVO> objectSchemeFieldDetailVOList = objectSchemeFieldMapper.selectCustomFieldList(ConvertUtil.getOrganizationId(projectId),projectId);
        if (objectSchemeFieldDetailVOList != null && !objectSchemeFieldDetailVOList.isEmpty()) {
            return objectSchemeFieldDetailVOList;
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public List<ObjectSchemeFieldDetailVO> listFieldsWithOptionals(Long projectId, Long issueTypeId, Long organizationId) {
        return objectSchemeFieldMapper.selectFieldsWithOptionals(organizationId, projectId, issueTypeId);
    }

    @Override
    public void updateRequired(Long organizationId, Long projectId, Long fieldId, Boolean required) {
        if (ObjectUtils.isEmpty(required)) {
            throw new CommonException("error.field.required.null");
        }
        boolean onProjectLevel = (projectId != null);
        if (onProjectLevel) {
            List<ObjectSchemeFieldExtendDTO> extendList =
                    objectSchemeFieldExtendMapper.selectExtendField(null, organizationId, fieldId, projectId);
            if (extendList.isEmpty()) {
                //项目层暂未配置，查组织层并新建
                extendList = objectSchemeFieldExtendMapper.selectExtendField(null, organizationId, fieldId, null);
            }
            Map<String, Long> issueTypeMap = issueTypeService.queryIssueTypeMap(organizationId);
            extendList.forEach(e ->
                    insertObjectSchemeFieldExtend(organizationId, projectId, fieldId, required, issueTypeMap, e.getIssueType(), e.getCreated(), e.getEdited()));
        } else {
            objectSchemeFieldExtendMapper.batchUpdateRequired(null, organizationId, fieldId, required);
        }
    }

    @Override
    public String queryRank(Long organizationId, Long projectId, AdjustOrderVO adjustOrderVO) {
        String issueType = adjustOrderVO.getIssueType();
        String previousRank = adjustOrderVO.getPreviousRank();
        String nextRank = adjustOrderVO.getNextRank();
        if (!ObjectUtils.isEmpty(previousRank) || !ObjectUtils.isEmpty(nextRank)) {
            if (StringUtils.isEmpty(previousRank)) {
                return RankUtil.genPre(nextRank);
            } else if (StringUtils.isEmpty(nextRank)) {
                return RankUtil.genNext(previousRank);
            } else {
                return RankUtil.between(nextRank, previousRank);
            }
        } else {
            ObjectSchemeFieldExtendDTO target = objectSchemeFieldExtendMapper.selectByPrimaryKey(adjustOrderVO.getOutsetFieldId());
            if (ObjectUtils.isEmpty(target)) {
                throw new CommonException("error.extend.field.not.existed");
            }
            String targetRank = target.getRank();
            if (adjustOrderVO.getBefore()) {
                return RankUtil.genNext(targetRank);
            } else {
                previousRank = objectSchemeFieldExtendMapper.selectPreviousRank(organizationId, projectId, issueType, targetRank);
                if (ObjectUtils.isEmpty(previousRank)) {
                    return RankUtil.genPre(targetRank);
                } else {
                    return RankUtil.between(targetRank, previousRank);
                }
            }
        }
    }

    @Override
    public List<ObjectSchemeFieldVO> selectMemberList(Long organizationId, Long projectId, String schemeCode, Long issueTypeId, List<String> fieldCodeList) {
        List<ObjectSchemeFieldDTO> list =
                objectSchemeFieldMapper.selectMemberByOptions(organizationId, projectId, schemeCode, issueTypeId, fieldCodeList);
        if (CollectionUtils.isEmpty(list)){
            return Collections.emptyList();
        }
        return list.stream().map(f -> modelMapper.map(f, ObjectSchemeFieldVO.class)).collect(Collectors.toList());
    }

    @Override
    public List<ObjectSchemeFieldVO> unselected(Long organizationId, Long projectId, String issueType) {
        return objectSchemeFieldExtendMapper.unselected(organizationId, projectId, issueType);
    }

    @Override
    public ObjectSchemeFieldDTO selectById(Long fieldId) {
        return objectSchemeFieldMapper.selectByPrimaryKey(fieldId);
    }

    @Override
    public void config(Long organizationId, Long projectId, PageConfigUpdateVO pageConfigUpdateVO) {
        String issueType = pageConfigUpdateVO.getIssueType();
        List<PageConfigFieldVO> fields = pageConfigUpdateVO.getFields();
        IssueTypeFieldVO issueTypeFieldVO = pageConfigUpdateVO.getIssueTypeFieldVO();
        Set<Long> deleteIds = pageConfigUpdateVO.getDeleteIds();

        ObjectSchemeFieldContext.isIllegalIssueType(issueType);
        Map<String, Long> issueTypeMap = issueTypeService.queryIssueTypeMap(organizationId);
        if (!ObjectUtils.isEmpty(fields)) {
            updateFieldConfig(organizationId, projectId, issueType, fields, issueTypeMap);
        }
        if (!ObjectUtils.isEmpty(projectId)
                && !ObjectUtils.isEmpty(issueTypeFieldVO)
                && !StringUtils.isEmpty(issueTypeFieldVO.getTemplate())) {
            updateTemplate(projectId, issueType, issueTypeFieldVO, issueTypeMap);
        }
        if (!ObjectUtils.isEmpty(deleteIds)) {
            deleteFieldConfig(organizationId, projectId, deleteIds);
        }
        List<ObjectSchemeFieldCreateVO> createdField = pageConfigUpdateVO.getCreatedFields();
        if (!ObjectUtils.isEmpty(createdField)) {
            createdField.forEach(c -> create(organizationId, projectId, c, issueType));
        }
        Set<Long> addIds = pageConfigUpdateVO.getAddIds();
        if(!ObjectUtils.isEmpty(addIds)) {
            addFieldConfig(organizationId, projectId, addIds, issueType, issueTypeMap);
        }
    }

    private void addFieldConfig(Long organizationId,
                                Long projectId,
                                Set<Long> addIds,
                                String issueType,
                                Map<String, Long> issueTypeMap) {
        addIds.forEach(a -> insertObjectSchemeFieldExtend(organizationId, projectId, a, true, issueTypeMap, issueType, true, true));
    }

    private void deleteFieldConfig(Long organizationId, Long projectId, Set<Long> deleteIds) {
        boolean editOnProjectLevel = (projectId != null);
        if (editOnProjectLevel) {
            //项目层无法删除组织层的字段
            List<ObjectSchemeFieldDTO> objectSchemeFieldList =
                    objectSchemeFieldMapper.selectByExtendIds(deleteIds);
            objectSchemeFieldList.forEach(o -> {
                if (o.getProjectId() == null || Objects.equals(0L, o.getOrganizationId())) {
                    //系统字段或者组织层字段
                    throw new CommonException("error.project.can.not.delete.organization.or.system.field");
                }
            });
            ObjectSchemeFieldExtendDTO example = new ObjectSchemeFieldExtendDTO();
            example.setOrganizationId(organizationId);
            example.setProjectId(projectId);
            deleteIds.forEach(d -> {
                ObjectSchemeFieldExtendDTO extend =
                        objectSchemeFieldExtendMapper.selectByPrimaryKey(d);
                Long fieldId = extend.getFieldId();
                example.setFieldId(fieldId);
                if (objectSchemeFieldExtendMapper.select(example).size() <= 1) {
                    //删除最后一个关联关系时，同时删除字段
                    objectSchemeFieldMapper.deleteByPrimaryKey(fieldId);
                }
                objectSchemeFieldExtendMapper.deleteByPrimaryKey(d);
            });
        } else {
            deleteIds.forEach(d -> {
                ObjectSchemeFieldExtendDTO extend =
                        objectSchemeFieldExtendMapper.selectByPrimaryKey(d);
                Long fieldId = extend.getFieldId();
                if (objectSchemeFieldExtendMapper
                        .selectExtendField(null, organizationId, fieldId, null).size() <= 1) {
                    //删除最后一个关联关系时，同时删除字段
                    objectSchemeFieldMapper.deleteByPrimaryKey(fieldId);
                }
                ObjectSchemeFieldExtendDTO target = new ObjectSchemeFieldExtendDTO();
                target.setOrganizationId(organizationId);
                target.setFieldId(fieldId);
                target.setIssueTypeId(extend.getIssueTypeId());
                target.setIssueType(extend.getIssueType());
                objectSchemeFieldExtendMapper.delete(target);
            });
        }
    }

    @Override
    public PageConfigVO listConfigs(Long organizationId, Long projectId, String issueType) {
        PageConfigVO result = new PageConfigVO();
        List<PageConfigFieldVO> pageConfigFields = objectSchemeFieldExtendMapper.listConfigs(organizationId, projectId, issueType);
        result.setFields(pageConfigFields);
        if (!ObjectUtils.isEmpty(projectId)) {
            Map<String, Long> issueTypeMap = issueTypeService.queryIssueTypeMap(organizationId);
            Long issueTypeId = issueTypeMap.get(issueType);
            if (ObjectUtils.isEmpty(issueTypeId)) {
                throw new CommonException("error.issue.type.not.existed");
            }
            IssueTypeFieldDTO dto = new IssueTypeFieldDTO();
            dto.setIssueTypeId(issueTypeId);
            dto.setProjectId(projectId);
            List<IssueTypeFieldDTO> list = issueTypeFieldMapper.select(dto);
            if (!list.isEmpty()) {
                result.setIssueTypeFieldVO(modelMapper.map(list.get(0), IssueTypeFieldVO.class));
            }
        }
        return result;
    }

    private void updateTemplate(Long projectId,
                                String issueType,
                                IssueTypeFieldVO issueTypeFieldVO,
                                Map<String, Long> issueTypeMap) {
        Long issueTypeId = issueTypeMap.get(issueType);
        if (ObjectUtils.isEmpty(issueTypeId)) {
            throw new CommonException("error.issue.type.not.existed", issueType);
        }
        IssueTypeFieldDTO dto = new IssueTypeFieldDTO();
        dto.setProjectId(projectId);
        dto.setIssueTypeId(issueTypeId);
        List<IssueTypeFieldDTO> result = issueTypeFieldMapper.select(dto);
        if (result.isEmpty()) {
            //create
            dto.setTemplate(issueTypeFieldVO.getTemplate());
            issueTypeFieldMapper.insertSelective(dto);
        } else {
            //update
            Long objectVersionNumber = issueTypeFieldVO.getObjectVersionNumber();
            if (ObjectUtils.isEmpty(objectVersionNumber)) {
                throw new CommonException("error.issueTypeField.objectVersionNumber.null");
            }
            IssueTypeFieldDTO target = result.get(0);
            target.setObjectVersionNumber(objectVersionNumber);
            target.setTemplate(issueTypeFieldVO.getTemplate());
            if (issueTypeFieldMapper.updateByPrimaryKeySelective(target) != 1) {
                throw new CommonException("error.issueTypeField.update");
            }
        }
    }

    private void updateFieldConfig(Long organizationId,
                                   Long projectId,
                                   String issueType,
                                   List<PageConfigFieldVO> fields,
                                   Map<String, Long> issueTypeMap) {
        boolean onProjectLevel = (projectId != null);
        fields.forEach(f -> {
            Long fieldId = f.getFieldId();
            if (ObjectUtils.isEmpty(f.getRequired())
                    || ObjectUtils.isEmpty(f.getCreated())
                    || ObjectUtils.isEmpty(f.getEdited())) {
                throw new CommonException("error.page.config.field.selectBox.empty");
            }
            if (ObjectUtils.isEmpty(f.getObjectVersionNumber())) {
                throw new CommonException("error.page.config.field.objectVersionNumber.null");
            }
            if (onProjectLevel) {
                //查询字段配置是否存在，存在则更新不存在则创建
                ObjectSchemeFieldExtendDTO dto = new ObjectSchemeFieldExtendDTO();
                dto.setIssueType(issueType);
                dto.setOrganizationId(organizationId);
                dto.setFieldId(fieldId);
                dto.setProjectId(projectId);
                List<ObjectSchemeFieldExtendDTO> result = objectSchemeFieldExtendMapper.select(dto);
                Long issueTypeId = issueTypeMap.get(issueType);
                if (result.isEmpty() && !ObjectUtils.isEmpty(issueTypeId)) {
                    dto.setIssueTypeId(issueTypeId);
                    dto.setRequired(f.getRequired());
                    dto.setCreated(f.getCreated());
                    dto.setEdited(f.getEdited());
                    dto.setRank(f.getRank());
                    objectSchemeFieldExtendMapper.insertSelective(dto);
                } else {
                    updateObjectSchemeFieldExtend(f, result);
                }
            } else {
                List<ObjectSchemeFieldExtendDTO> result =
                        objectSchemeFieldExtendMapper.selectExtendField(issueType, organizationId, fieldId, null);
                if (result.isEmpty()) {
                    throw new CommonException("error.page.config.field.not.existed");
                } else {
                    updateObjectSchemeFieldExtend(f, result);
                }
            }
        });
    }

    private void updateObjectSchemeFieldExtend(PageConfigFieldVO field, List<ObjectSchemeFieldExtendDTO> result) {
        ObjectSchemeFieldExtendDTO target = result.get(0);
        target.setRequired(field.getRequired());
        target.setEdited(field.getEdited());
        target.setCreated(field.getCreated());
        target.setRank(field.getRank());
        target.setObjectVersionNumber(field.getObjectVersionNumber());
        if (objectSchemeFieldExtendMapper.updateByPrimaryKeySelective(target) != 1) {
            throw new CommonException("error.page.config.field.update");
        }
    }
}
