package io.choerodon.agile.app.service.impl;

import io.choerodon.agile.api.vo.StatusTransferSettingCreateVO;
import io.choerodon.agile.api.vo.StatusTransferSettingVO;
import io.choerodon.agile.api.vo.UserVO;
import io.choerodon.agile.app.assembler.StatusTransferSettingAssembler;
import io.choerodon.agile.app.service.ProjectConfigService;
import io.choerodon.agile.app.service.StatusTransferSettingService;
import io.choerodon.agile.app.service.UserService;
import io.choerodon.agile.infra.dto.StatusDTO;
import io.choerodon.agile.infra.dto.StatusTransferSettingDTO;
import io.choerodon.agile.infra.dto.UserDTO;
import io.choerodon.agile.infra.feign.BaseFeignClient;
import io.choerodon.agile.infra.mapper.StatusMapper;
import io.choerodon.agile.infra.mapper.StatusTransferSettingMapper;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.oauth.DetailsHelper;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author zhaotianxin
 * @date 2020-08-12 10:09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class StatusTransferSettingServiceImpl implements StatusTransferSettingService {
    private static final String SPECIFIER = "specifier";
    private static final String PROJECT_OWNER = "projectOwner";
    @Autowired
    private StatusTransferSettingMapper statusTransferSettingMapper;
    @Autowired
    private UserService userService;
    @Autowired
    private StatusTransferSettingAssembler statusTransferSettingAssembler;
    @Autowired
    private StatusMapper statusMapper;
    @Autowired
    private BaseFeignClient baseFeignClient;
    @Autowired
    private ProjectConfigService projectConfigService;

    @Override
    public void createOrUpdate(Long projectId, Long issueTypeId, Long statusId,Long objectVersionNumber,String applyType,List<StatusTransferSettingCreateVO> list) {
        List<StatusTransferSettingDTO> query = query(projectId, issueTypeId, statusId);
        if (!CollectionUtils.isEmpty(query)) {
            delete(projectId, issueTypeId, statusId);
        }
        if (!CollectionUtils.isEmpty(list)) {
            for (StatusTransferSettingCreateVO settingCreateVO : list) {
                if (SPECIFIER.equals(settingCreateVO.getType()) && !CollectionUtils.isEmpty(settingCreateVO.getUserIds())) {
                    for (Long userId : settingCreateVO.getUserIds()) {
                        StatusTransferSettingDTO statusTransferSettingDTO = new StatusTransferSettingDTO(issueTypeId, statusId, projectId, settingCreateVO.getType());
                        statusTransferSettingDTO.setUserId(userId);
                        baseInsert(statusTransferSettingDTO);
                    }
                } else {
                    StatusTransferSettingDTO statusTransferSettingDTO = new StatusTransferSettingDTO(issueTypeId, statusId, projectId, settingCreateVO.getType());
                    baseInsert(statusTransferSettingDTO);
                }
            }
            projectConfigService.updateNodeObjectVersionNumber(projectId,issueTypeId,statusId,objectVersionNumber,applyType);
        }
    }

    @Override
    public List<StatusTransferSettingDTO> query(Long projectId, Long issueTypeId, Long statusId) {
        StatusTransferSettingDTO statusTransferSettingDTO = new StatusTransferSettingDTO();
        statusTransferSettingDTO.setStatusId(statusId);
        statusTransferSettingDTO.setProjectId(projectId);
        statusTransferSettingDTO.setIssueTypeId(issueTypeId);
        return statusTransferSettingMapper.select(statusTransferSettingDTO);
    }

    @Override
    public void delete(Long projectId, Long issueTypeId, Long statusId) {
        StatusTransferSettingDTO statusTransferSettingDTO = new StatusTransferSettingDTO();
        statusTransferSettingDTO.setStatusId(statusId);
        statusTransferSettingDTO.setProjectId(projectId);
        statusTransferSettingDTO.setIssueTypeId(issueTypeId);
        statusTransferSettingMapper.delete(statusTransferSettingDTO);
    }

    @Override
    public List<StatusTransferSettingVO> listByStatusIds(Long projectId, Long issueTypeId, List<Long> statusIds) {
        if (CollectionUtils.isEmpty(statusIds)) {
            throw new CommonException("error.statusIds.null");
        }
        List<StatusTransferSettingDTO> dtos = statusTransferSettingMapper.listByStatusId(projectId,issueTypeId,statusIds);
        if(CollectionUtils.isEmpty(dtos)){
           return new ArrayList<>();
        }
        Set<Long> userIds = dtos.stream().filter(v -> !ObjectUtils.isEmpty(v.getUserId())).map(StatusTransferSettingDTO::getUserId).collect(Collectors.toSet());
        Map<Long,UserDTO> userDTOMap = new HashMap<>();
        if(!CollectionUtils.isEmpty(userIds)){
            List<UserDTO> userDTOS = userService.listUsersByIds(userIds.toArray(new Long[userIds.size()]));
            userDTOMap.putAll(userDTOS.stream().collect(Collectors.toMap(UserDTO::getId, Function.identity())));
        }
        return statusTransferSettingAssembler.listDTOToVO(dtos,userDTOMap);
    }

    @Override
    public void checkStatusTransferSetting(Long projectId, Long issueTypeId, Long endStatusId) {
        List<StatusTransferSettingDTO> query = query(projectId, issueTypeId, endStatusId);
        if (CollectionUtils.isEmpty(query)) {
            return;
        }
        // 获取当前的用户
        Long userId = DetailsHelper.getUserDetails().getUserId();
        Set<Long> userIds = new HashSet<>();
        for (StatusTransferSettingDTO statusTransferSettingDTO : query) {
            if (!PROJECT_OWNER.equals(statusTransferSettingDTO.getUserType())) {
                List<UserVO> body = baseFeignClient.listProjectOwnerById(projectId).getBody();
                if (!CollectionUtils.isEmpty(body)) {
                    userIds.addAll(body.stream().map(UserVO::getId).collect(Collectors.toSet()));
                }
            } else {
                userIds.add(statusTransferSettingDTO.getUserId());
            }
        }
        if (!userIds.contains(userId)) {
            throw new CommonException("error.no.permission.to.switch");
        }
    }

    private void baseInsert(StatusTransferSettingDTO statusTransferSettingDTO) {
        if (statusTransferSettingMapper.insertSelective(statusTransferSettingDTO) != 1) {
            throw new CommonException("error.insert.status.transfer.setting");
        }
    }
}
