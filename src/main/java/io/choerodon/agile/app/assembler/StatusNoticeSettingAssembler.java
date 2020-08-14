package io.choerodon.agile.app.assembler;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.choerodon.agile.api.vo.ObjectSchemeFieldVO;
import io.choerodon.agile.api.vo.StatusNoticeSettingVO;
import io.choerodon.agile.app.service.ObjectSchemeFieldService;
import io.choerodon.agile.infra.dto.StatusNoticeSettingDTO;
import io.choerodon.agile.infra.dto.UserDTO;
import io.choerodon.agile.infra.enums.StatusNoticeUserType;
import io.choerodon.agile.infra.feign.BaseFeignClient;
import io.choerodon.agile.infra.utils.ConvertUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hzero.core.base.BaseConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author jiaxu.cui@hand-china.com 2020/8/14 下午3:04
 */
@Component
public class StatusNoticeSettingAssembler {
    @Autowired
    private BaseFeignClient baseFeignClient;
    @Autowired
    private ObjectSchemeFieldService objectSchemeFieldService;

    public List<StatusNoticeSettingVO> statusNoticeDto2Vo(Long projectId, Long issueTypeId,
                                                          List<StatusNoticeSettingDTO> list, String applyType) {
        Map<Long, List<StatusNoticeSettingDTO>> group =
                list.stream().collect(Collectors.groupingBy(StatusNoticeSettingDTO::getStatusId));
        return group.entrySet().stream().map(entry -> {
            StatusNoticeSettingVO settingVO = new StatusNoticeSettingVO(issueTypeId, projectId, entry.getKey());
            entry.getValue().forEach(item -> settingVO.addUserWithNotice(item.getUserType(), item.getUserId()));
            settingVO.setNoticeTypeList(Stream.of(StringUtils.split(entry.getValue().stream().map(StatusNoticeSettingDTO::getNoticeType)
                    .findFirst().orElse(""), BaseConstants.Symbol.COMMA)).collect(Collectors.toList()));
            this.addUserInfo(settingVO, applyType);
            return settingVO;
        }).collect(Collectors.toList());
    }

    public void addUserInfo(StatusNoticeSettingVO statusNoticeSettingVO, String schemeCode){
        if (CollectionUtils.isNotEmpty(statusNoticeSettingVO.getUserIdList())){
            List<UserDTO> userDTOList = baseFeignClient.listUsersByIds(statusNoticeSettingVO.getUserIdList().toArray(new Long[0]), true).getBody();
            statusNoticeSettingVO.setUserList(userDTOList);
        }
        Set<String> userTypeList = new HashSet<>(statusNoticeSettingVO.getUserTypeList());
        userTypeList.removeAll(Arrays.asList(StatusNoticeUserType.BASE_USER_TYPE_LIST));
        if (CollectionUtils.isNotEmpty(userTypeList)){
            statusNoticeSettingVO.getUserTypeList().removeAll(userTypeList);
            List<ObjectSchemeFieldVO> objectSchemeFieldDTOS =
                    objectSchemeFieldService.selectMemberList(ConvertUtil.getOrganizationId(statusNoticeSettingVO.getProjectId()),
                    statusNoticeSettingVO.getProjectId(),  schemeCode, null, new ArrayList<>(userTypeList));
            statusNoticeSettingVO.setMemberList(objectSchemeFieldDTOS);
        }
    }
}
