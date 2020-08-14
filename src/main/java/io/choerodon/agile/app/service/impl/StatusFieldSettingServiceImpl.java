package io.choerodon.agile.app.service.impl;

import io.choerodon.agile.api.vo.*;
import io.choerodon.agile.app.service.FieldValueService;
import io.choerodon.agile.app.service.IssueService;
import io.choerodon.agile.app.service.ProjectConfigService;
import io.choerodon.agile.app.service.StatusFieldSettingService;
import io.choerodon.agile.infra.dto.*;
import io.choerodon.agile.infra.enums.FieldCode;
import io.choerodon.agile.infra.enums.FieldType;
import io.choerodon.agile.infra.feign.BaseFeignClient;
import io.choerodon.agile.infra.mapper.*;
import io.choerodon.agile.infra.utils.ConvertUtil;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.oauth.DetailsHelper;
import org.apache.commons.lang.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author zhaotianxin
 * @date 2020-08-13 14:51
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class StatusFieldSettingServiceImpl implements StatusFieldSettingService {
    private static final String[] FILTER_FIELD_TYPE = {"checkbox", "multiple", "member", "radio", "single"};
    protected static final Map<String, String> FIELD_CODE = new LinkedHashMap<>();
    private static final String CLEAR = "clear";
    @Autowired
    private StatusFieldSettingMapper statusFieldSettingMapper;
    @Autowired
    private StatusFieldValueSettingMapper statusFieldValueSettingMapper;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private ProjectConfigService projectConfigService;
    @Autowired
    private IssueComponentMapper issueComponentMapper;
    @Autowired
    private IssueLabelMapper issueLabelMapper;
    @Autowired
    private ProductVersionMapper productVersionMapper;
    @Autowired
    private BaseFeignClient baseFeignClient;
    @Autowired
    private PriorityMapper priorityMapper;
    @Autowired
    private FieldOptionMapper fieldOptionMapper;
    @Autowired
    private IssueMapper issueMapper;
    @Autowired
    private FieldValueMapper fieldValueMapper;
    @Autowired
    private IssueService issueService;
    @Autowired
    private FieldValueService fieldValueService;
    static {
        FIELD_CODE.put(FieldCode.ASSIGNEE, "assigneeId");
        FIELD_CODE.put(FieldCode.REPORTER, "reporterId");
        FIELD_CODE.put(FieldCode.COMPONENT, "componentIssueRelVOList");
        FIELD_CODE.put(FieldCode.LABEL, "labelIssueRelVOList");
        FIELD_CODE.put(FieldCode.FIX_VERSION, "versionIssueRelVOList");
        FIELD_CODE.put(FieldCode.INFLUENCE_VERSION, "versionIssueRelVOList");
        FIELD_CODE.put(FieldCode.DESCRIPTION, "description");
        FIELD_CODE.put(FieldCode.STORY_POINTS, "storyPoints");
        FIELD_CODE.put(FieldCode.REMAINING_TIME, "remainingTime");
        FIELD_CODE.put(FieldCode.PRIORITY, "priorityId");
        FIELD_CODE.put(FieldCode.EPIC, "epicId");
        FIELD_CODE.put(FieldCode.CREATION_DATE, "creationDate");
        FIELD_CODE.put(FieldCode.LAST_UPDATE_DATE, "lastUpdateDate");
    }
    @Override
    public List<StatusFieldSettingVO> createOrUpdate(Long project, Long issueType, Long statusId, Long objectVersionNumber, String applyType, List<StatusFieldSettingVO> list) {
        List<StatusFieldSettingDTO> statusFieldSettingDTOS = listFieldSetting(project, issueType, statusId);
        if (!CollectionUtils.isEmpty(statusFieldSettingDTOS)) {
            deleteStatusFieldSetting(statusFieldSettingDTOS);
        }
        // 遍历
        for (StatusFieldSettingVO statusFieldSettingVO : list) {
            StatusFieldSettingDTO map = modelMapper.map(statusFieldSettingVO, StatusFieldSettingDTO.class);
            map.setProjectId(project);
            map.setStatusId(statusId);
            map.setIssueTypeId(issueType);
            baseInsert(map);
            // 插入field值
            List<StatusFieldValueSettingDTO> fieldValueList = statusFieldSettingVO.getFieldValueList();
            if (!CollectionUtils.isEmpty(fieldValueList)) {
                insertStatusFieldValue(project, map.getId(), fieldValueList);
            }
        }
        // 更新node
        projectConfigService.updateNodeObjectVersionNumber(project, issueType, statusId, objectVersionNumber, applyType);
        return list(project, issueType, statusId);
    }

    @Override
    public List<StatusFieldSettingDTO> listFieldSetting(Long project, Long issueType, Long statusId) {
        StatusFieldSettingDTO statusFieldSettingDTO = new StatusFieldSettingDTO();
        statusFieldSettingDTO.setIssueTypeId(issueType);
        statusFieldSettingDTO.setStatusId(statusId);
        statusFieldSettingDTO.setProjectId(project);
        return statusFieldSettingMapper.select(statusFieldSettingDTO);
    }

    @Override
    public List<StatusFieldSettingVO> list(Long projectId, Long issueType, Long statusId) {
        List<StatusFieldSettingDTO> statusFieldSettingDTOS = listFieldSetting(projectId, issueType, statusId);
        if (CollectionUtils.isEmpty(statusFieldSettingDTOS)) {
            return new ArrayList<>();
        }
        List<StatusFieldSettingVO> list = new ArrayList<>();
        for (StatusFieldSettingDTO statusFieldSettingDTO : statusFieldSettingDTOS) {
            StatusFieldSettingVO map = modelMapper.map(statusFieldSettingDTO, StatusFieldSettingVO.class);
            map.setFieldValueList(listFieldValueSetting(projectId, map.getId()));
            list.add(map);
        }
        return list;
    }

    @Override
    public List<StatusFieldSettingVO> listByStatusIds(Long projectId, Long issueType, List<Long> statusIds) {
        List<StatusFieldSettingVO> list = statusFieldSettingMapper.listByStatusIds(projectId, issueType, statusIds);
        list.forEach(statusFieldSettingVO -> {
            String fieldType = statusFieldSettingVO.getFieldType();
            List<String> fieldTypes = Arrays.asList(FILTER_FIELD_TYPE);
            List<StatusFieldValueSettingDTO> statusFieldValueSettingDTOS = listFieldValueSetting(projectId, statusFieldSettingVO.getId());
            if (!fieldTypes.contains(fieldType)) {
                statusFieldSettingVO.setFieldValueList(statusFieldValueSettingDTOS);
                return;
            }
            if (!Objects.equals("specifier", statusFieldValueSettingDTOS.get(0).getOperateType())) {
                statusFieldSettingVO.setFieldValueList(statusFieldValueSettingDTOS);
                return;
            }
            if ("member".equals(statusFieldSettingVO.getFieldType())) {
                // 查询用户信息
                List<Long> userIds = statusFieldValueSettingDTOS.stream().map(StatusFieldValueSettingDTO::getUserId).collect(Collectors.toList());
                List<UserDTO> body = baseFeignClient.listUsersByIds(userIds.toArray(new Long[userIds.size()]), false).getBody();
                if (CollectionUtils.isEmpty(body)) {
                    statusFieldSettingVO.setFieldValueList(statusFieldValueSettingDTOS);
                    return;
                }
                Map<Long, UserDTO> userDTOMap = body.stream().collect(Collectors.toMap(UserDTO::getId, Function.identity()));
                statusFieldValueSettingDTOS.forEach(v -> v.setName(ObjectUtils.isEmpty(userDTOMap.get(v.getUserId())) ? null : userDTOMap.get(v.getUserId()).getRealName()));
                statusFieldSettingVO.setFieldValueList(statusFieldValueSettingDTOS);
                return;
            }
            handlerFieldValue(statusFieldSettingVO, statusFieldValueSettingDTOS);
            statusFieldSettingVO.setFieldValueList(statusFieldValueSettingDTOS);
        });
        return list;
    }

    @Override
    public void handlerSettingToUpdateIssue(Long projectId, Long issueId) {
        IssueDTO issueDTO = issueMapper.selectByPrimaryKey(issueId);
        List<StatusFieldSettingVO> list = statusFieldSettingMapper.listByStatusIds(projectId, issueDTO.getIssueTypeId(), Arrays.asList(issueDTO.getStatusId()));
        IssueUpdateVO issueUpdateVO = new IssueUpdateVO();
        List<PageFieldViewUpdateVO> customField = new ArrayList<>();
        List<String> field = new ArrayList<>();
        Map<String,List<VersionIssueRelVO>> versionMap = new HashMap<>();
        Class aClass = issueUpdateVO.getClass();
        list.forEach(v -> {
            List<StatusFieldValueSettingDTO> statusFieldValueSettingDTOS = listFieldValueSetting(projectId, v.getId());
            if (Boolean.TRUE.equals(v.getSystem())) {
                Boolean isVersion = FieldCode.FIX_VERSION.equals(v.getFieldCode()) || FieldCode.INFLUENCE_VERSION.equals(v.getFieldCode());
                if(Boolean.TRUE.equals(isVersion)){
                    handlerVersion(versionMap,v,statusFieldValueSettingDTOS);
                }
                else {
                    handlerPredefinedValue(issueUpdateVO,aClass, field, issueDTO, v, statusFieldValueSettingDTOS);
                }
            } else {
                PageFieldViewUpdateVO pageFieldViewUpdateVO = new PageFieldViewUpdateVO();
                pageFieldViewUpdateVO.setFieldType(v.getFieldType());
                pageFieldViewUpdateVO.setFieldId(v.getFieldId());
                setCustomFieldValue(issueDTO, v, pageFieldViewUpdateVO, statusFieldValueSettingDTOS);
                customField.add(pageFieldViewUpdateVO);
            }
        });
        // 执行更新
        updateIssue(issueDTO,field,issueUpdateVO,customField,versionMap);
    }
    private void updateIssue(IssueDTO issueDTO,List<String> field,IssueUpdateVO issueUpdateVO,List<PageFieldViewUpdateVO> customField,Map<String,List<VersionIssueRelVO>> versionMap){
        Long organizationId = ConvertUtil.getOrganizationId(issueDTO.getProjectId());
        Long objectVersionNumber = issueDTO.getObjectVersionNumber();
        if (!CollectionUtils.isEmpty(field)) {
            issueUpdateVO.setIssueId(issueDTO.getIssueId());
            issueUpdateVO.setObjectVersionNumber(objectVersionNumber);
            issueService.updateIssue(issueDTO.getProjectId(), issueUpdateVO, field);
            objectVersionNumber += 1;
        }
        // 单独更新版本
        if (!CollectionUtils.isEmpty(versionMap)) {
            for (Map.Entry<String, List<VersionIssueRelVO>> entry :versionMap.entrySet()) {
                IssueUpdateVO issueUpdateVO1 = new IssueUpdateVO();
                issueUpdateVO1.setIssueId(issueDTO.getIssueId());
                issueUpdateVO1.setObjectVersionNumber(objectVersionNumber);
                issueUpdateVO1.setVersionType(entry.getKey());
                issueUpdateVO1.setVersionIssueRelVOList(entry.getValue());
                issueService.updateIssue(issueDTO.getProjectId(), issueUpdateVO1, new ArrayList<>());
                objectVersionNumber += 1;
            }
        }
        if (!CollectionUtils.isEmpty(customField)) {
            for (PageFieldViewUpdateVO pageFieldViewUpdateVO : customField) {
                fieldValueService.updateFieldValue(organizationId, issueDTO.getProjectId(), issueDTO.getIssueId(), pageFieldViewUpdateVO.getFieldId(), "agile_issue", pageFieldViewUpdateVO);
            }
        }
    }

    private void handlerVersion(Map<String, List<VersionIssueRelVO>> versionMap, StatusFieldSettingVO statusFieldSettingVO, List<StatusFieldValueSettingDTO> statusFieldValueSettingDTOS) {
        Boolean isVersion = FieldCode.FIX_VERSION.equals(statusFieldSettingVO.getFieldCode()) || FieldCode.INFLUENCE_VERSION.equals(statusFieldSettingVO.getFieldCode());
        if (Boolean.TRUE.equals(isVersion)) {
            List<VersionIssueRelVO> versionIssueRelVOS = new ArrayList<>();
            if (!CLEAR.equals(statusFieldValueSettingDTOS.get(0).getOperateType())) {
                versionIssueRelVOS = statusFieldValueSettingDTOS.stream().map(settingDTO -> {
                    VersionIssueRelVO versionIssueRelVO = new VersionIssueRelVO();
                    versionIssueRelVO.setVersionId(settingDTO.getOptionId());
                    return versionIssueRelVO;
                }).collect(Collectors.toList());
            }
            String versionType = FieldCode.FIX_VERSION.equals(statusFieldSettingVO.getFieldCode()) ? "fix" : "influence";
            versionMap.put(versionType,versionIssueRelVOS);
        }
    }

    private void handlerPredefinedValue(IssueUpdateVO issueUpdateVO,Class aClass, List<String> fieldList, IssueDTO issueDTO, StatusFieldSettingVO v, List<StatusFieldValueSettingDTO> statusFieldValueSettingDTOS) {
        String fieldCode = v.getFieldCode();
        String fieldName = FIELD_CODE.get(fieldCode);
        try {
            Field field = aClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            StatusFieldValueSettingDTO statusFieldValueSettingDTO = statusFieldValueSettingDTOS.get(0);
            fieldList.add(fieldName);
            if (CLEAR.equals(statusFieldValueSettingDTO.getOperateType())) {
                field.set(issueUpdateVO, null);
                return;
            }
            handlerFieldName(issueUpdateVO, statusFieldValueSettingDTOS, issueDTO, statusFieldValueSettingDTO, v, field);
        } catch (Exception e) {
            throw new CommonException("error.transform.object");
        }
    }

    private void handlerFieldName(IssueUpdateVO issueUpdateVO, List<StatusFieldValueSettingDTO> statusFieldValueSettingDTOS, IssueDTO issueDTO, StatusFieldValueSettingDTO fieldValueSettingDTO, StatusFieldSettingVO v, Field field) throws IllegalAccessException {
        switch (v.getFieldCode()) {
            case FieldCode.ASSIGNEE:
            case FieldCode.REPORTER:
                field.set(issueUpdateVO, handlerMember(fieldValueSettingDTO, issueDTO));
                break;
            case FieldCode.CREATION_DATE:
            case FieldCode.LAST_UPDATE_DATE:
                field.set(issueUpdateVO, handlerTimeField(fieldValueSettingDTO));
                break;
            case FieldCode.EPIC:
            case FieldCode.PRIORITY:
                field.set(issueUpdateVO, fieldValueSettingDTO.getOptionId());
                break;
            case FieldCode.REMAINING_TIME:
                BigDecimal bigDecimal = issueDTO.getRemainingTime();
                field.set(issueUpdateVO, handlerPredefinedNumber(fieldValueSettingDTO, bigDecimal));
                break;
            case FieldCode.STORY_POINTS:
                BigDecimal storyPoints = issueDTO.getStoryPoints();
                field.set(issueUpdateVO, handlerPredefinedNumber(fieldValueSettingDTO, storyPoints));
                break;
            case FieldCode.DESCRIPTION:
                field.set(issueUpdateVO, fieldValueSettingDTO.getTextValue());
                break;
            case FieldCode.COMPONENT:
                List<ComponentIssueRelVO> componentIssueRelVOS = statusFieldValueSettingDTOS.stream().map(settingDTO -> {
                    ComponentIssueRelVO componentIssueRelVO = new ComponentIssueRelVO();
                    componentIssueRelVO.setComponentId(settingDTO.getOptionId());
                    return componentIssueRelVO;
                }).collect(Collectors.toList());
                field.set(issueUpdateVO, componentIssueRelVOS);
                break;
            case FieldCode.LABEL:
                List<LabelIssueRelVO> labelIssueRelVOS = statusFieldValueSettingDTOS.stream().map(settingDTO -> {
                    LabelIssueRelVO labelIssueRelVO = new LabelIssueRelVO();
                    labelIssueRelVO.setLabelId(settingDTO.getOptionId());
                    return labelIssueRelVO;
                }).collect(Collectors.toList());
                field.set(issueUpdateVO, labelIssueRelVOS);
                break;
            default:
                break;
        }
    }

    private BigDecimal handlerPredefinedNumber(StatusFieldValueSettingDTO statusFieldValueSettingDTO, BigDecimal oldValue) {
        BigDecimal bigDecimal = null;
        if ("add".equals(statusFieldValueSettingDTO.getOperateType())) {
            BigDecimal numberAddValue = statusFieldValueSettingDTO.getNumberAddValue();
            if (ObjectUtils.isEmpty(oldValue)) {
                return numberAddValue;
            }
            BigDecimal add = oldValue.add(numberAddValue);
            bigDecimal = add;
            return add;
        } else {
            bigDecimal = statusFieldValueSettingDTO.getNumberValue();
        }
        return bigDecimal;
    }

    private void setCustomFieldValue(IssueDTO issueDTO, StatusFieldSettingVO v, PageFieldViewUpdateVO pageFieldViewUpdateVO, List<StatusFieldValueSettingDTO> statusFieldValueSettingDTOS) {
        if (CLEAR.equals(statusFieldValueSettingDTOS.get(0).getOperateType())) {
            return;
        }
        StatusFieldValueSettingDTO statusFieldValueSettingDTO = statusFieldValueSettingDTOS.get(0);
        switch (v.getFieldType()) {
            case FieldType.CHECKBOX:
            case FieldType.MULTIPLE:
                pageFieldViewUpdateVO.setValue(statusFieldValueSettingDTOS.stream().map(StatusFieldValueSettingDTO::getOptionId).collect(Collectors.toList()));
                break;
            case FieldType.RADIO:
            case FieldType.SINGLE:
                pageFieldViewUpdateVO.setValue(statusFieldValueSettingDTO.getOptionId());
                break;
            case FieldType.DATE:
            case FieldType.DATETIME:
            case FieldType.TIME:
                pageFieldViewUpdateVO.setValue(handlerTimeField(statusFieldValueSettingDTO));
                break;
            case FieldType.MEMBER:
                pageFieldViewUpdateVO.setValue(handlerMember(statusFieldValueSettingDTO, issueDTO));
                break;
            case FieldType.TEXT:
                pageFieldViewUpdateVO.setValue(statusFieldValueSettingDTO.getTextValue());
                break;
            case FieldType.INPUT:
                pageFieldViewUpdateVO.setValue(statusFieldValueSettingDTO.getStringValue());
                break;
            case FieldType.NUMBER:
                pageFieldViewUpdateVO.setValue(handlerNumber(v, statusFieldValueSettingDTO, issueDTO));
                break;
            default:
                break;
        }
    }

    private BigDecimal handlerNumber(StatusFieldSettingVO v, StatusFieldValueSettingDTO statusFieldValueSettingDTO, IssueDTO issueDTO) {
        BigDecimal bigDecimal = null;
        if ("add".equals(statusFieldValueSettingDTO.getOperateType())) {
            List<FieldValueDTO> fieldValueDTOS = fieldValueMapper.queryList(v.getProjectId(), issueDTO.getIssueId(), "agile_issue", v.getFieldId());
            BigDecimal numberAddValue = statusFieldValueSettingDTO.getNumberAddValue();
            if (CollectionUtils.isEmpty(fieldValueDTOS)) {
                return numberAddValue;
            }
            String numberValue = fieldValueDTOS.get(0).getNumberValue();
            BigDecimal oldValue = BigDecimal.valueOf(Long.valueOf(numberValue));
            BigDecimal add = oldValue.add(numberAddValue);
            bigDecimal = add;
            return add;
        } else {
            bigDecimal = statusFieldValueSettingDTO.getNumberValue();
        }
        return bigDecimal;
    }

    private Long handlerMember(StatusFieldValueSettingDTO statusFieldValueSettingDTO, IssueDTO issueDTO) {
        Long userId = null;
        if ("operator".equals(statusFieldValueSettingDTO.getOperateType())) {
            userId = DetailsHelper.getUserDetails().getUserId();
        } else if ("creator".equals(statusFieldValueSettingDTO.getOperateType())) {
            userId = issueDTO.getCreatedBy();
        } else if ("reportor".equals(statusFieldValueSettingDTO.getOperateType())) {
            userId = issueDTO.getReporterId();
        } else {
            userId = statusFieldValueSettingDTO.getUserId();
        }
        return userId;
    }

    private Date handlerTimeField(StatusFieldValueSettingDTO statusFieldValueSettingDTO) {
        Date date = null;
        if ("add".equals(statusFieldValueSettingDTO.getOperateType())) {
            BigDecimal dateAddValue = statusFieldValueSettingDTO.getDateAddValue();
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, dateAddValue.intValue());
            date = cal.getTime();
        } else if ("current_time".equals(statusFieldValueSettingDTO.getOperateType())) {
            date = new Date();
        } else {
            date = statusFieldValueSettingDTO.getDateValue();
        }
        return date;
    }

    private void handlerFieldValue(StatusFieldSettingVO statusFieldSettingVO, List<StatusFieldValueSettingDTO> statusFieldValueSettingDTOS) {
        List<Long> ids = statusFieldValueSettingDTOS.stream().map(StatusFieldValueSettingDTO::getOptionId).collect(Collectors.toList());
        if (Boolean.TRUE.equals(statusFieldSettingVO.getSystem())) {
            switch (statusFieldSettingVO.getFieldCode()) {
                case "component":
                    List<IssueComponentDTO> issueComponentDTOS = issueComponentMapper.selectByIds(StringUtils.join(ids, ","));
                    if (CollectionUtils.isEmpty(issueComponentDTOS)) {
                        break;
                    }
                    Map<Long, IssueComponentDTO> collect = issueComponentDTOS.stream().collect(Collectors.toMap(IssueComponentDTO::getComponentId, Function.identity()));
                    statusFieldValueSettingDTOS.forEach(v -> v.setName(ObjectUtils.isEmpty(collect.get(v.getOptionId())) ? null : collect.get(v.getOptionId()).getName()));
                    break;
                case "label":
                    List<IssueLabelDTO> issueLabelDTOS = issueLabelMapper.selectByIds(StringUtils.join(ids, ","));
                    if (CollectionUtils.isEmpty(issueLabelDTOS)) {
                        break;
                    }
                    Map<Long, IssueLabelDTO> labelDTOMap = issueLabelDTOS.stream().collect(Collectors.toMap(IssueLabelDTO::getLabelId, Function.identity()));
                    statusFieldValueSettingDTOS.forEach(v -> v.setName(ObjectUtils.isEmpty(labelDTOMap.get(v.getOptionId())) ? null : labelDTOMap.get(v.getOptionId()).getLabelName()));
                    break;
                case "influenceVersion":
                case "fixVersion":
                    List<ProductVersionDTO> productVersionDTOS = productVersionMapper.selectByIds(StringUtils.join(ids, ","));
                    if (CollectionUtils.isEmpty(productVersionDTOS)) {
                        break;
                    }
                    Map<Long, ProductVersionDTO> versionDTOMap = productVersionDTOS.stream().collect(Collectors.toMap(ProductVersionDTO::getVersionId, Function.identity()));
                    statusFieldValueSettingDTOS.forEach(v -> v.setName(ObjectUtils.isEmpty(versionDTOMap.get(v.getOptionId())) ? null : versionDTOMap.get(v.getOptionId()).getName()));
                    break;
                case "priority":
                    List<PriorityDTO> priorityDTOS = priorityMapper.selectByIds(StringUtils.join(ids, ","));
                    if (CollectionUtils.isEmpty(priorityDTOS)) {
                        break;
                    }
                    Map<Long, PriorityDTO> priorityDTOMap = priorityDTOS.stream().collect(Collectors.toMap(PriorityDTO::getId, Function.identity()));
                    statusFieldValueSettingDTOS.forEach(v -> v.setName(ObjectUtils.isEmpty(priorityDTOMap.get(v.getOptionId())) ? null : priorityDTOMap.get(v.getOptionId()).getName()));
                    break;
                case "epic":
                    List<IssueDTO> issueDTOS = issueMapper.selectByIds(StringUtils.join(ids, ","));
                    if (CollectionUtils.isEmpty(issueDTOS)) {
                        break;
                    }
                    Map<Long, IssueDTO> issueDTOMap = issueDTOS.stream().collect(Collectors.toMap(IssueDTO::getIssueId, Function.identity()));
                    statusFieldValueSettingDTOS.forEach(v -> v.setName(ObjectUtils.isEmpty(issueDTOMap.get(v.getOptionId())) ? null : issueDTOMap.get(v.getOptionId()).getSummary()));
                    break;
                default:
                    break;
            }
        } else {
            List<FieldOptionDTO> fieldOptionDTOS = fieldOptionMapper.selectByIds(StringUtils.join(ids, ","));
            if (CollectionUtils.isEmpty(fieldOptionDTOS)) {
                return;
            }
            Map<Long, FieldOptionDTO> fieldOptionDTOMap = fieldOptionDTOS.stream().collect(Collectors.toMap(FieldOptionDTO::getId, Function.identity()));
            statusFieldValueSettingDTOS.forEach(v -> v.setName(ObjectUtils.isEmpty(fieldOptionDTOMap.get(v.getOptionId())) ? null : fieldOptionDTOMap.get(v.getOptionId()).getValue()));
        }
    }

    private List<StatusFieldValueSettingDTO> listFieldValueSetting(Long projectId, Long fieldSettingId) {
        StatusFieldValueSettingDTO statusFieldValueSettingDTO = new StatusFieldValueSettingDTO();
        statusFieldValueSettingDTO.setProjectId(projectId);
        statusFieldValueSettingDTO.setStatusFieldSettingId(fieldSettingId);
        return statusFieldValueSettingMapper.select(statusFieldValueSettingDTO);
    }

    private void deleteStatusFieldSetting(List<StatusFieldSettingDTO> statusFieldSettingDTOS) {
        for (StatusFieldSettingDTO statusFieldSettingDTO : statusFieldSettingDTOS) {
            StatusFieldValueSettingDTO statusFieldValueSettingDTO = new StatusFieldValueSettingDTO();
            statusFieldValueSettingDTO.setProjectId(statusFieldSettingDTO.getProjectId());
            statusFieldValueSettingDTO.setStatusFieldSettingId(statusFieldSettingDTO.getId());
            statusFieldValueSettingMapper.delete(statusFieldValueSettingDTO);
            statusFieldSettingMapper.deleteByPrimaryKey(statusFieldSettingDTO.getId());
        }
    }

    private void insertStatusFieldValue(Long projectId, Long fieldSettingId, List<StatusFieldValueSettingDTO> fieldValueList) {
        for (StatusFieldValueSettingDTO statusFieldValueSettingDTO : fieldValueList) {
            statusFieldValueSettingDTO.setStatusFieldSettingId(fieldSettingId);
            statusFieldValueSettingDTO.setProjectId(projectId);
            baseInertFieldValue(statusFieldValueSettingDTO);
        }
    }

    private void baseInertFieldValue(StatusFieldValueSettingDTO statusFieldValueSettingDTO) {
        if (statusFieldValueSettingMapper.insert(statusFieldValueSettingDTO) != 1) {
            throw new CommonException("error.insert.status.field.value.setting");
        }
    }

    private void baseInsert(StatusFieldSettingDTO map) {
        if (statusFieldSettingMapper.insert(map) != 1) {
            throw new CommonException("error.insert.status.field.setting");
        }
    }
}
