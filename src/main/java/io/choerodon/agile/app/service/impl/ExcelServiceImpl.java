package io.choerodon.agile.app.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.choerodon.agile.infra.enums.ExcelImportTemplateColumn;
import io.choerodon.core.domain.Page;
import io.choerodon.agile.api.vo.*;
import io.choerodon.agile.infra.dto.IssueTypeLinkDTO;
import io.choerodon.agile.infra.dto.PredefinedDTO;
import io.choerodon.agile.app.service.*;
import io.choerodon.agile.infra.dto.*;
import io.choerodon.agile.infra.feign.BaseFeignClient;
import io.choerodon.agile.infra.mapper.*;
import io.choerodon.agile.infra.utils.*;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.oauth.DetailsHelper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hzero.boot.file.FileClient;
import org.hzero.boot.message.MessageClient;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.*;

/**
 * Created by HuangFuqiang@choerodon.io on 2019/2/25.
 * Email: fuqianghuang01@gmail.com
 */
@Service
public class ExcelServiceImpl implements ExcelService {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ExcelServiceImpl.class);

    protected static final String[] FIELDS_NAME =
            {"问题类型*", "所属史诗", "模块", "冲刺", "概述*", "子任务概述(仅子任务生效)", "描述", "经办人", "报告人",
                    "优先级*", "预估时间(小时)", "版本", "故事点", "史诗名称(仅问题类型为史诗时生效)"};

    protected static final String BACKETNAME = "agile-service";
    protected static final String SUB_TASK = "sub_task";
    protected static final String UPLOAD_FILE = "upload_file";
    protected static final String APPLY_TYPE_AGILE = "agile";
    protected static final String CANCELED = "canceled";
    protected static final String DOING = "doing";
    protected static final String SUCCESS = "success";
    protected static final String FAILED = "failed";
    protected static final String WEBSOCKET_IMPORT_CODE = "agile-import-issues";
    protected static final String FEATURE = "feature";
    protected static final String FILE_NAME = "error.xlsx";
    protected static final String MULTIPART_NAME = "file";
    protected static final String ORIGINAL_FILE_NAME = ".xlsx";

    protected static final String VERSION_PLANNING = "version_planning";

    protected static final String RELATION_TYPE_FIX = "fix";
    protected static final String IMPORT_TEMPLATE_NAME = "导入模板";

    protected static final String EPIC_CN = "史诗";

    protected static final String STORY_CN = "故事";

    protected static final String BUG_CN = "缺陷";

    protected static final String TASK_CN = "任务";

    protected static final String SUB_TASK_CN = "子任务";

    @Autowired
    protected StateMachineClientService stateMachineClientService;

    @Autowired
    private MessageClient messageClient;

    @Autowired
    private FileOperationHistoryMapper fileOperationHistoryMapper;

    @Autowired
    protected ProductVersionMapper productVersionMapper;

    @Autowired
    private IssueService issueService;

    @Autowired
    private IssueMapper issueMapper;

    @Autowired
    protected IssueComponentMapper issueComponentMapper;

    @Autowired
    protected SprintMapper sprintMapper;
    @Autowired
    private ProjectConfigService projectConfigService;
    @Autowired
    private PriorityService priorityService;

    @Autowired
    protected BaseFeignClient baseFeignClient;

    @Autowired
    private FileClient fileClient;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void download(Long projectId, Long organizationId, HttpServletRequest request, HttpServletResponse response) {
        List<PredefinedDTO> predefinedList = getPredefinedList(organizationId, projectId, false);
        //所属史诗预定义值
        predefinedList.add(getEpicPredefined(projectId));

        Workbook wb = new XSSFWorkbook();
        // create guide sheet
        ExcelUtil.createGuideSheet(wb, ExcelUtil.initGuideSheet(), false);
        Sheet sheet = wb.createSheet(IMPORT_TEMPLATE_NAME);
        CellStyle style = CatalogExcelUtil.getHeadStyle(wb);
        Map<Integer,Integer> widthMap = new HashMap<>();
        widthMap.put(ExcelImportTemplateColumn.Issue.EPIC_COL, 8000);
        widthMap.put(ExcelImportTemplateColumn.Issue.SUB_TASK_COL, 8000);
        widthMap.put(ExcelImportTemplateColumn.Issue.EPIC_NAME_COL, 8000);
        ExcelUtil.generateHeaders(sheet, style, Arrays.asList(FIELDS_NAME), widthMap);

        try {
            //填充预定义值
            fillInPredefinedValues(wb, sheet, predefinedList);
            wb.write(response.getOutputStream());
        } catch (Exception e) {
            LOGGER.info("exception: {}", e);
        }
    }

    protected PredefinedDTO getEpicPredefined(Long projectId) {
        List<String> values = new ArrayList<>(getEpicMap(projectId).keySet());
        values.sort(String.CASE_INSENSITIVE_ORDER);
        return new PredefinedDTO(
                values,
                1,
                500,
                ExcelImportTemplateColumn.Issue.EPIC_SHEET.getCol(),
                ExcelImportTemplateColumn.Issue.EPIC_SHEET.getCol(),
                ExcelImportTemplateColumn.Issue.EPIC_SHEET.getName(),
                ExcelImportTemplateColumn.Issue.EPIC_SHEET.getIndex()
        );
    }

    protected Map<String, Long> getEpicMap(Long projectId) {
        Map<String, Long> epicMap = new HashMap<>();
        List<EpicDataVO> epics = issueService.listEpic(projectId);
        epics.forEach(e -> {
            String summary = e.getSummary();
            if (ObjectUtils.isEmpty(epicMap.get(summary))) {
                epicMap.put(summary, e.getIssueId());
            }
        });
        return epicMap;
    }

    protected void fillInPredefinedValues(Workbook wb, Sheet sheet, List<PredefinedDTO> predefinedList) {
        for (PredefinedDTO predefined : predefinedList) {
            wb = ExcelUtil
                    .dropDownList2007(
                            wb,
                            sheet,
                            predefined.values(),
                            predefined.startRow(),
                            predefined.endRow(),
                            predefined.startCol(),
                            predefined.endCol(),
                            predefined.hidden(),
                            predefined.hiddenSheetIndex());
        }
    }

    protected List<PredefinedDTO> getPredefinedList(Long organizationId, Long projectId, boolean withFeature) {
        List<PredefinedDTO> predefinedList = new ArrayList<>();
        List<PriorityVO> priorityVOList = priorityService.queryByOrganizationIdList(organizationId);
        List<IssueTypeVO> issueTypeVOList = projectConfigService.queryIssueTypesByProjectId(projectId, APPLY_TYPE_AGILE);
        List<ProductVersionCommonDTO> productVersionCommonDTOList = productVersionMapper.listByProjectId(projectId);
        List<IssueComponentDTO> issueComponentDTOList = issueComponentMapper.selectByProjectId(projectId);
        List<SprintDTO> sprintDTOList = sprintMapper.selectNotDoneByProjectId(projectId);

        List<String> priorityList = new ArrayList<>();
        for (PriorityVO priorityVO : priorityVOList) {
            if (priorityVO.getEnable()) {
                priorityList.add(priorityVO.getName());
            }
        }
        predefinedList.add(
                new PredefinedDTO(
                        priorityList,
                        1,
                        500,
                        ExcelImportTemplateColumn.Issue.PRIORITY_SHEET.getCol(),
                        ExcelImportTemplateColumn.Issue.PRIORITY_SHEET.getCol(),
                        ExcelImportTemplateColumn.Issue.PRIORITY_SHEET.getName(),
                        ExcelImportTemplateColumn.Issue.PRIORITY_SHEET.getIndex()
                ));

        List<String> issueTypeList = new ArrayList<>();
        for (IssueTypeVO issueTypeVO : issueTypeVOList) {
            String typeCode = issueTypeVO.getTypeCode();
            if (withFeature && "issue_epic".equals(typeCode)) {
                continue;
            }
            if (!SUB_TASK.equals(typeCode) && !FEATURE.equals(typeCode)) {
                issueTypeList.add(issueTypeVO.getName());
            }
        }
        predefinedList.add(
                new PredefinedDTO(
                        issueTypeList,
                        1,
                        500,
                        ExcelImportTemplateColumn.Issue.ISSUE_TYPE_SHEET.getCol(),
                        ExcelImportTemplateColumn.Issue.ISSUE_TYPE_SHEET.getCol(),
                        ExcelImportTemplateColumn.Issue.ISSUE_TYPE_SHEET.getName(),
                        ExcelImportTemplateColumn.Issue.ISSUE_TYPE_SHEET.getIndex()
                ));

        List<String> versionList = new ArrayList<>();
        for (ProductVersionCommonDTO productVersionCommonDTO : productVersionCommonDTOList) {
            if (VERSION_PLANNING.equals(productVersionCommonDTO.getStatusCode())) {
                versionList.add(productVersionCommonDTO.getName());
            }
        }
        predefinedList.add(
                new PredefinedDTO(
                        versionList,
                        1,
                        500,
                        ExcelImportTemplateColumn.Issue.FIX_VERSION_SHEET.getCol(),
                        ExcelImportTemplateColumn.Issue.FIX_VERSION_SHEET.getCol(),
                        ExcelImportTemplateColumn.Issue.FIX_VERSION_SHEET.getName(),
                        ExcelImportTemplateColumn.Issue.FIX_VERSION_SHEET.getIndex()
                ));

        List<String> componentList = new ArrayList<>();
        for (IssueComponentDTO issueComponentDTO : issueComponentDTOList) {
            componentList.add(issueComponentDTO.getName());
        }
        predefinedList.add(
                new PredefinedDTO(
                        componentList,
                        1,
                        500,
                        ExcelImportTemplateColumn.Issue.COMPONENT_SHEET.getCol(),
                        ExcelImportTemplateColumn.Issue.COMPONENT_SHEET.getCol(),
                        ExcelImportTemplateColumn.Issue.COMPONENT_SHEET.getName(),
                        ExcelImportTemplateColumn.Issue.COMPONENT_SHEET.getIndex()
                ));

        List<String> sprintList = new ArrayList<>();
        for (SprintDTO sprintDTO : sprintDTOList) {
            sprintList.add(sprintDTO.getSprintName());
        }
        predefinedList.add(
                new PredefinedDTO(
                        sprintList,
                        1,
                        500,
                        ExcelImportTemplateColumn.Issue.SPRINT_SHEET.getCol(),
                        ExcelImportTemplateColumn.Issue.SPRINT_SHEET.getCol(),
                        ExcelImportTemplateColumn.Issue.SPRINT_SHEET.getName(),
                        ExcelImportTemplateColumn.Issue.SPRINT_SHEET.getIndex()
                ));

        List<String> users = new ArrayList<>(getManagers(projectId).keySet());
        predefinedList.add(
                new PredefinedDTO(
                        users,
                        1,
                        500,
                        ExcelImportTemplateColumn.Issue.MANAGER_SHEET.getCol(),
                        ExcelImportTemplateColumn.Issue.MANAGER_SHEET.getCol(),
                        ExcelImportTemplateColumn.Issue.MANAGER_SHEET.getName(),
                        ExcelImportTemplateColumn.Issue.MANAGER_SHEET.getIndex()
                ));
        predefinedList.add(
                new PredefinedDTO(
                        users,
                        1,
                        500,
                        ExcelImportTemplateColumn.Issue.REPORTER_SHEET.getCol(),
                        ExcelImportTemplateColumn.Issue.REPORTER_SHEET.getCol(),
                        ExcelImportTemplateColumn.Issue.REPORTER_SHEET.getName(),
                        ExcelImportTemplateColumn.Issue.REPORTER_SHEET.getIndex()
                ));
        return predefinedList;
    }

    protected Map<String, Long> getManagers(Long projectId) {
        Map<String, Long> managerMap = new HashMap<>();
        ResponseEntity<Page<UserDTO>> response = baseFeignClient.listUsersByProjectId(projectId, 1, 0);
        List<UserDTO> users = response.getBody().getContent();
        users.forEach(u -> {
            if (u.getEnabled()) {
                String realName = u.getRealName();
                String loginName = u.getLoginName();
                String name = realName + "（" + loginName + "）";
                managerMap.put(name, u.getId());
            }
        });
        return managerMap;
    }

    protected Boolean setIssueCreateInfo(IssueCreateVO issueCreateVO,
                                         Long projectId,
                                         Map<String, IssueTypeVO> issueTypeMap,
                                         Map<String, Long> priorityMap,
                                         Map<String, Long> versionMap,
                                         Long userId,
                                         Map<String, Long> componentMap,
                                         Map<String, Long> sprintMap,
                                         Map<String, Long> managerMap,
                                         Integer rowNum,
                                         Sheet sheet,
                                         Map<Integer, Integer> sonParentMap,
                                         IssueTypeVO subTask,
                                         Map<String, Long> theSecondColumnMap) {
        issueCreateVO.setProjectId(projectId);
        Row row = sheet.getRow(rowNum);
        //经办人
        setManager(issueCreateVO, managerMap, row);
        //报告人
        setReporter(issueCreateVO, managerMap, row);
        //优先级
        String priorityName = row.getCell(ExcelImportTemplateColumn.Issue.PRIORITY_COL).toString();
        Long priorityId = priorityMap.get(priorityName);
        if (ObjectUtils.isEmpty(priorityId)) {
            return false;
        } else {
            issueCreateVO.setPriorityCode("priority" + priorityId);
            issueCreateVO.setPriorityId(priorityId);
        }
        //预估时间
        setRemainTime(issueCreateVO, row);
        //版本
        setVersion(issueCreateVO, versionMap, row);
        //描述
        setDescription(issueCreateVO, row);

        String typeName = getTypeName(row);
        if (isSubTask(row)) {
            //子任务是任务类型，无需设置故事点和史诗名
            String summary = row.getCell(ExcelImportTemplateColumn.Issue.SUB_TASK_COL).toString();
            if (!StringUtils.hasText(summary)) {
                throw new CommonException("error.summary.null");
            }
            issueCreateVO.setSummary(summary);
            issueCreateVO.setTypeCode(subTask.getTypeCode());
            issueCreateVO.setIssueTypeId(subTask.getId());
            //子任务的所属史诗模块和冲刺，保持与父节点统一
            Row parentRow = sheet.getRow(sonParentMap.get(rowNum));
            setBelongsEpic(issueCreateVO, parentRow, theSecondColumnMap, typeName);
            setComponent(issueCreateVO, parentRow, componentMap);
            setSprint(issueCreateVO, parentRow, sprintMap);
        } else {
            String summary = row.getCell(ExcelImportTemplateColumn.Issue.SUMMARY_COL).toString();
            if (!StringUtils.hasText(summary)) {
                throw new CommonException("error.summary.null");
            }
            issueCreateVO.setSummary(summary);
            IssueTypeVO issueType = issueTypeMap.get(typeName);
            if (issueType == null) {
                return false;
            }
            issueCreateVO.setTypeCode(issueType.getTypeCode());
            issueCreateVO.setIssueTypeId(issueType.getId());
            if (EPIC_CN.equals(typeName)) {
                //默认名称和概要相同
                String epicName = row.getCell(ExcelImportTemplateColumn.Issue.EPIC_NAME_COL).toString();
                issueCreateVO.setSummary(epicName);
                issueCreateVO.setEpicName(epicName);
            } else {
                if (STORY_CN.equals(typeName)) {
                    Cell storyPointCell = row.getCell(ExcelImportTemplateColumn.Issue.STORY_POINT_COL);
                    if (!isCellEmpty(storyPointCell)) {
                        issueCreateVO.setStoryPoints(new BigDecimal(storyPointCell.toString()));
                    }
                }
                setBelongsEpic(issueCreateVO, row, theSecondColumnMap, typeName);
            }
            setComponent(issueCreateVO, row, componentMap);
            setSprint(issueCreateVO, row, sprintMap);
        }
        return true;
    }

    protected void setDescription(IssueCreateVO issueCreateVO, Row row) {
        Cell descriptionCell = row.getCell(ExcelImportTemplateColumn.Issue.DESCRIPTION_COL);
        if (!isCellEmpty(descriptionCell)) {
            String description = descriptionCell.toString();
            if (StringUtils.hasText(description)) {
                issueCreateVO.setDescription("[{\"insert\":\"" + StringUtil.replaceChar(description) + "\\n\"}]");
            }
        }
    }

    protected void setVersion(IssueCreateVO issueCreateVO, Map<String, Long> versionMap, Row row) {
        Cell versionCell = row.getCell(ExcelImportTemplateColumn.Issue.FIX_VERSION_COL);
        if (!isCellEmpty(versionCell)) {
            String version = versionCell.toString();
            if (StringUtils.hasText(version)) {
                List<VersionIssueRelVO> versionIssueRelList = new ArrayList<>();
                VersionIssueRelVO versionIssueRelVO = new VersionIssueRelVO();
                versionIssueRelVO.setVersionId(versionMap.get(version));
                versionIssueRelVO.setRelationType(RELATION_TYPE_FIX);
                versionIssueRelList.add(versionIssueRelVO);
                issueCreateVO.setVersionIssueRelVOList(versionIssueRelList);
            }
        }
    }

    protected void setRemainTime(IssueCreateVO issueCreateVO, Row row) {
        Cell remainTimeCell = row.getCell(ExcelImportTemplateColumn.Issue.REMAIN_TIME_COL);
        if (!isCellEmpty(remainTimeCell)) {
            issueCreateVO.setRemainingTime(new BigDecimal(remainTimeCell.toString()));
        }
    }

    protected void setManager(IssueCreateVO issueCreateVO, Map<String, Long> managerMap, Row row) {
        Cell cell = row.getCell(ExcelImportTemplateColumn.Issue.MANAGER_COL);
        if (!isCellEmpty(cell)) {
            String manager = cell.toString();
            if (StringUtils.hasText(manager)) {
                Long assigneeId = managerMap.get(manager);
                issueCreateVO.setAssigneeId(assigneeId);
            }
        }
    }

    protected void setReporter(IssueCreateVO issueCreateVO, Map<String, Long> managerMap, Row row) {
        Cell cell = row.getCell(ExcelImportTemplateColumn.Issue.REPORTER_COL);
        if (!isCellEmpty(cell)) {
            String manager = cell.toString();
            if (StringUtils.hasText(manager)) {
                Long reporterId = managerMap.get(manager);
                issueCreateVO.setReporterId(reporterId);
            }
        }
    }

    protected void setSprint(IssueCreateVO issueCreateVO, Row row, Map<String, Long> sprintMap) {
        Cell sprintCell = row.getCell(ExcelImportTemplateColumn.Issue.SPRINT_COL);
        if (!isCellEmpty(sprintCell)) {
            String sprint = sprintCell.toString();
            if (StringUtils.hasText(sprint)) {
                issueCreateVO.setSprintId(sprintMap.get(sprint));
            }
        }
    }

    protected void setComponent(IssueCreateVO issueCreateVO, Row row, Map<String, Long> componentMap) {
        Cell componentCell = row.getCell(ExcelImportTemplateColumn.Issue.COMPONENT_COL);
        if (!isCellEmpty(componentCell)) {
            String value = componentCell.toString();
            if (StringUtils.hasText(value)) {
                ComponentIssueRelVO componentIssueRelVO = new ComponentIssueRelVO();
                componentIssueRelVO.setComponentId(componentMap.get(value));
                issueCreateVO.setComponentIssueRelVOList(Arrays.asList(componentIssueRelVO));
            }
        }
    }

    protected void setBelongsEpic(IssueCreateVO issueCreateVO, Row row,
                                  Map<String, Long> theSecondColumnMap,
                                  String typeName) {
        Cell cell = row.getCell(ExcelImportTemplateColumn.Issue.EPIC_COL);
        if (!isCellEmpty(cell)) {
            String belongsEpic = cell.toString();
            //子任务不设置史诗
            if (StringUtils.hasText(belongsEpic) && !SUB_TASK_CN.equals(typeName)) {
                issueCreateVO.setEpicId(theSecondColumnMap.get(belongsEpic));
            }
        }
    }

    protected void updateFinalRecode(FileOperationHistoryDTO fileOperationHistoryDTO, Long successcount, Long failCount, String status) {
        FileOperationHistoryDTO update = new FileOperationHistoryDTO();
        update.setId(fileOperationHistoryDTO.getId());
        update.setSuccessCount(successcount);
        update.setFailCount(failCount);
        update.setStatus(status);
        update.setFileUrl(fileOperationHistoryDTO.getFileUrl());
        update.setObjectVersionNumber(fileOperationHistoryDTO.getObjectVersionNumber());
        if (fileOperationHistoryMapper.updateByPrimaryKeySelective(update) != 1) {
            throw new CommonException("error.FileOperationHistoryDTO.update");
        }
        FileOperationHistoryDTO result = fileOperationHistoryMapper.selectByPrimaryKey(update.getId());
        sendProcess(result, result.getUserId(), 1.0);
    }

    protected IssueTypeVO setIssueTypeAndPriorityMap(Long organizationId,
                                                     Long projectId,
                                                     Map<String, IssueTypeVO> issueTypeMap,
                                                     Map<String, Long> priorityMap,
                                                     List<String> issueTypeList,
                                                     List<String> priorityList,
                                                     boolean withFeature) {
        IssueTypeVO subTask = null;
        List<PriorityVO> priorityVOList = priorityService.queryByOrganizationIdList(organizationId);
        List<IssueTypeVO> issueTypeVOList = projectConfigService.queryIssueTypesByProjectId(projectId, APPLY_TYPE_AGILE);
        for (PriorityVO priorityVO : priorityVOList) {
            if (priorityVO.getEnable()) {
                priorityMap.put(priorityVO.getName(), priorityVO.getId());
                priorityList.add(priorityVO.getName());
            }
        }
        for (IssueTypeVO issueTypeVO : issueTypeVOList) {
            if (SUB_TASK.equals(issueTypeVO.getTypeCode())) {
                subTask = issueTypeVO;
            }
            //有特性列跳过史诗类型
            if (withFeature && "issue_epic".equals(issueTypeVO.getTypeCode())) {
                continue;
            }
            if (!SUB_TASK.equals(issueTypeVO.getTypeCode()) && !FEATURE.equals(issueTypeVO.getTypeCode())) {
                issueTypeMap.put(issueTypeVO.getName(), issueTypeVO);
            }
        }
        issueTypeList.addAll(issueTypeMap.keySet());
        return subTask;
    }

    protected void sendProcess(FileOperationHistoryDTO fileOperationHistoryDTO, Long userId, Double process) {
        fileOperationHistoryDTO.setProcess(process);
        String message = null;
        try {
            message = objectMapper.writeValueAsString(fileOperationHistoryDTO);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        messageClient.sendByUserId(userId, WEBSOCKET_IMPORT_CODE, message);
    }

    protected String uploadErrorExcel(Workbook errorWorkbook, Long organizationId) {
        // 上传错误的excel
        MultipartFile multipartFile = new MultipartExcelUtil(MULTIPART_NAME, ORIGINAL_FILE_NAME, errorWorkbook);
        return fileClient.uploadFile(organizationId, BACKETNAME, null, FILE_NAME, multipartFile);
    }

    protected Boolean checkEpicNameExist(Long projectId, String epicName) {
        IssueDTO issueDTO = new IssueDTO();
        issueDTO.setProjectId(projectId);
        issueDTO.setEpicName(epicName);
        List<IssueDTO> issueDTOList = issueMapper.select(issueDTO);
        return issueDTOList == null || issueDTOList.isEmpty();
    }

    protected Map<Integer, String> checkRule(Long projectId, Sheet sheet,
                                             List<String> issueTypeList,
                                             List<String> priorityList,
                                             List<String> versionList,
                                             List<String> componentList,
                                             List<String> sprintList,
                                             int rowNum,
                                             Set<Integer> illegalRow,
                                             Set<String> theSecondColumn,
                                             List<String> managers) {
        Row row = sheet.getRow(rowNum);
        Map<Integer, String> errorMessage = new HashMap<>();
        // 经办人,非必填
        checkUser(managers, row, errorMessage, ExcelImportTemplateColumn.Issue.MANAGER_COL, "经办人输入错误");
        checkUser(managers, row, errorMessage, ExcelImportTemplateColumn.Issue.REPORTER_COL, "报告人输入错误");
        //优先级
        checkPriority(priorityList, row, errorMessage);
        //预估时间
        checkRemainTime(row, errorMessage);
        //版本
        checkVersion(versionList, row, errorMessage);
        //故事点
        checkStoryPoint(row, errorMessage);

        if (isSubTask(row)) {
            //子任务只校验子任务概述列
            String subTaskSummary = row.getCell(5).toString();
            if (illegalRow.contains(rowNum)) {
                errorMessage.put(ExcelImportTemplateColumn.Issue.ISSUE_TYPE_COL, "子任务必须有父节点");
            } else if (subTaskSummary.length() > 44) {
                errorMessage.put(ExcelImportTemplateColumn.Issue.SUB_TASK_COL, "子任务概要过长");
            }
        } else {
            Cell issueTypeCell = row.getCell(0);
            //问题类型
            if (isCellEmpty(issueTypeCell)) {
                errorMessage.put(ExcelImportTemplateColumn.Issue.ISSUE_TYPE_COL, "问题类型不能为空");
            } else if (!issueTypeList.contains(issueTypeCell.toString())) {
                errorMessage.put(ExcelImportTemplateColumn.Issue.ISSUE_TYPE_COL, "问题类型输入错误");
            } else if (EPIC_CN.equals(issueTypeCell.toString())) {
                //如果是史诗的话，判断是否重复和字段长度
                Cell epicNameCell = row.getCell(ExcelImportTemplateColumn.Issue.EPIC_NAME_COL);
                if (isCellEmpty(epicNameCell)) {
                    errorMessage.put(ExcelImportTemplateColumn.Issue.EPIC_NAME_COL, "史诗名称不能为空");
                } else {
                    String epicName = epicNameCell.toString().trim();
                    if (epicName.length() > 20) {
                        errorMessage.put(ExcelImportTemplateColumn.Issue.EPIC_NAME_COL, "史诗名称过长");
                    } else if (!checkEpicNameExist(projectId, epicName)) {
                        errorMessage.put(ExcelImportTemplateColumn.Issue.EPIC_NAME_COL, "史诗名称重复");
                    }
                }
            }
            //检查第二列，史诗
            checkSecondColumn(theSecondColumn, row, errorMessage);
            //模块
            checkComponent(componentList, row, errorMessage);
            //冲刺
            checkSprint(sprintList, row, errorMessage);
            Cell summaryCell = row.getCell(ExcelImportTemplateColumn.Issue.SUMMARY_COL);
            if (isCellEmpty(summaryCell)) {
                errorMessage.put(ExcelImportTemplateColumn.Issue.SUMMARY_COL, "概要不能为空");
            } else if (summaryCell.toString().length() > 44) {
                errorMessage.put(ExcelImportTemplateColumn.Issue.SUMMARY_COL, "概要过长");
            }
        }
        return errorMessage;
    }

    protected void checkSprint(List<String> sprintList, Row row, Map<Integer, String> errorMessage) {
        Cell sprintCell = row.getCell(ExcelImportTemplateColumn.Issue.SPRINT_COL);
        if (!isCellEmpty(sprintCell) && !sprintList.contains(sprintCell.toString())) {
            errorMessage.put(ExcelImportTemplateColumn.Issue.SPRINT_COL, "请输入正确的冲刺");
        }
    }

    protected void checkComponent(List<String> componentList, Row row, Map<Integer, String> errorMessage) {
        Cell componentCell = row.getCell(ExcelImportTemplateColumn.Issue.COMPONENT_COL);
        if (!isCellEmpty(componentCell) && !componentList.contains(componentCell.toString())) {
            errorMessage.put(ExcelImportTemplateColumn.Issue.COMPONENT_COL, "请输入正确的模块");
        }
    }

    protected void checkSecondColumn(Set<String> theSecondColumn, Row row, Map<Integer, String> errorMessage) {
        Cell secondColumnCell = row.getCell(ExcelImportTemplateColumn.Issue.EPIC_COL);
        if (!isCellEmpty(secondColumnCell) && !theSecondColumn.contains(secondColumnCell.toString())) {
            errorMessage.put(ExcelImportTemplateColumn.Issue.EPIC_COL, "所属史诗输入错误");
        }
    }

    protected void checkStoryPoint(Row row, Map<Integer, String> errorMessage) {
        Cell storyPointCell = row.getCell(ExcelImportTemplateColumn.Issue.STORY_POINT_COL);
        if (!isCellEmpty(storyPointCell)) {
            String storyPointStr = storyPointCell.toString().trim();
            if (storyPointStr.length() > 3) {
                errorMessage.put(ExcelImportTemplateColumn.Issue.STORY_POINT_COL, "请输入正确的位数");
            } else if (!NumberUtil.isNumeric(storyPointStr)) {
                errorMessage.put(ExcelImportTemplateColumn.Issue.STORY_POINT_COL, "请输入数字");
            } else {
                if (NumberUtil.isInteger(storyPointStr) || NumberUtil.canParseInteger(storyPointStr)) {
                    if (storyPointStr.trim().length() > 3) {
                        errorMessage.put(ExcelImportTemplateColumn.Issue.STORY_POINT_COL, "最大支持3位整数");
                    } else if (storyPointStr.trim().length() > 1 && "0".equals(storyPointStr.trim().substring(0, 0))) {
                        errorMessage.put(ExcelImportTemplateColumn.Issue.STORY_POINT_COL, "请输入正确的整数");
                    }
                } else if (!"0.5".equals(storyPointStr)) {
                    errorMessage.put(ExcelImportTemplateColumn.Issue.STORY_POINT_COL, "小数只支持0.5");
                }
            }
        }
    }

    protected void checkVersion(List<String> versionList, Row row, Map<Integer, String> errorMessage) {
        Cell versionCell = row.getCell(ExcelImportTemplateColumn.Issue.FIX_VERSION_COL);
        if (!isCellEmpty(versionCell)) {
            if (!versionList.contains(versionCell.toString())) {
                errorMessage.put(ExcelImportTemplateColumn.Issue.FIX_VERSION_COL, "请输入正确的版本");
            }
        }
    }

    protected void checkRemainTime(Row row, Map<Integer, String> errorMessage) {
        Cell remainTimeCell = row.getCell(ExcelImportTemplateColumn.Issue.REMAIN_TIME_COL);
        if (!isCellEmpty(remainTimeCell)) {
            String remainTime = remainTimeCell.toString().trim();
            if (remainTime.length() > 3) {
                errorMessage.put(ExcelImportTemplateColumn.Issue.REMAIN_TIME_COL, "请输入正确的位数");
            } else if (!NumberUtil.isNumeric(remainTime)) {
                errorMessage.put(ExcelImportTemplateColumn.Issue.REMAIN_TIME_COL, "请输入数字");
            } else {
                if (NumberUtil.isInteger(remainTime) || NumberUtil.canParseInteger(remainTime)) {
                    if (remainTime.length() > 3) {
                        errorMessage.put(ExcelImportTemplateColumn.Issue.REMAIN_TIME_COL, "最大支持3位整数");
                    } else if (remainTime.length() > 1 && "0".equals(remainTime.substring(0, 0))) {
                        errorMessage.put(ExcelImportTemplateColumn.Issue.REMAIN_TIME_COL, "请输入正确的整数");
                    }
                } else if (!"0.5".equals(remainTime)) {
                    errorMessage.put(ExcelImportTemplateColumn.Issue.REMAIN_TIME_COL, "小数只支持0.5");
                }
            }
        }
    }

    protected void checkPriority(List<String> priorityList, Row row, Map<Integer, String> errorMessage) {
        Cell priorityCell = row.getCell(ExcelImportTemplateColumn.Issue.PRIORITY_COL);
        if (isCellEmpty(priorityCell)) {
            errorMessage.put(ExcelImportTemplateColumn.Issue.PRIORITY_COL, "优先级不能为空");
        } else if (!priorityList.contains(priorityCell.toString())) {
            errorMessage.put(ExcelImportTemplateColumn.Issue.PRIORITY_COL, "优先级输入错误");
        }
    }

    protected void checkUser(List<String> managers, Row row, Map<Integer, String> errorMessage,
                             int col, String msg) {
        Cell managerCell = row.getCell(col);
        if (!isCellEmpty(managerCell)) {
            String manager = managerCell.toString();
            if (!managers.contains(manager)) {
                errorMessage.put(col, msg);
            }
        }
    }

    protected boolean isSubTask(Row row) {
        return SUB_TASK_CN.equals(getTypeName(row));
    }

    protected Boolean checkCanceled(Long projectId, Long fileOperationHistoryId, List<Long> importedIssueIds) {
        FileOperationHistoryDTO checkCanceledDO = fileOperationHistoryMapper.selectByPrimaryKey(fileOperationHistoryId);
        if (UPLOAD_FILE.equals(checkCanceledDO.getAction()) && CANCELED.equals(checkCanceledDO.getStatus())) {
            if (!importedIssueIds.isEmpty()) {
                LOGGER.info(importedIssueIds.toString());
                issueService.batchDeleteIssuesAgile(projectId, importedIssueIds);
            }
            return true;
        }
        return false;
    }

    /**
     * @param sheet
     * @param columnNum 数据页总共有多少列数据
     * @return
     */
    protected Integer getRealRowCount(Sheet sheet, int columnNum) {
        Integer count = 0;
        for (int r = 1; r <= sheet.getPhysicalNumberOfRows(); r++) {
            Row row = sheet.getRow(r);
            //row为空跳过
            if (isSkip(row, columnNum)) {
                continue;
            }
            count++;
        }
        return count;
    }

    protected boolean isSkip(Row row, int columnNum) {
        if (row == null) {
            return true;
        }
        //所有列都为空才跳过
        boolean skip = true;
        for (int i = 0; i < columnNum; i++) {
            Cell cell = row.getCell(i);
            skip = skip && isCellEmpty(cell);

        }
        return skip;
    }

    protected boolean isCellEmpty(Cell cell) {
        return cell == null || cell.toString().equals("") || cell.getCellType() == XSSFCell.CELL_TYPE_BLANK;
    }

    @Async
    @Override
    public void batchImport(Long projectId, Long organizationId, Long userId, Workbook workbook) {
        String status = DOING;
        FileOperationHistoryDTO res = initFileOperationHistory(projectId, userId, status);
        validateWorkbook(projectId, userId, workbook, res, FIELDS_NAME);

        Sheet sheet = workbook.getSheetAt(1);
        // 获取所有非空行
        int columnNum = FIELDS_NAME.length;
        Integer allRowCount = getRealRowCount(sheet, columnNum);
        // 查询组织下的优先级与问题类型
        Map<String, IssueTypeVO> issueTypeMap = new HashMap<>();
        Map<String, Long> priorityMap = new HashMap<>();
        List<String> issueTypeList = new ArrayList<>();
        List<String> priorityList = new ArrayList<>();
        IssueTypeVO subTask = setIssueTypeAndPriorityMap(organizationId, projectId, issueTypeMap, priorityMap, issueTypeList, priorityList, false);
        Long failCount = 0L;
        Long successCount = 0L;
        Integer processNum = 0;
        List<Integer> errorRows = new ArrayList<>();
        Map<Integer, List<Integer>> errorMapList = new HashMap<>();
        Map<String, Long> versionMap = new HashMap<>();
        Map<String, Long> componentMap = new HashMap<>();
        Map<String, Long> sprintMap = new HashMap<>();
        List<ProductVersionCommonDTO> productVersionCommonDTOList = productVersionMapper.listByProjectId(projectId);
        List<IssueComponentDTO> issueComponentDTOList = issueComponentMapper.selectByProjectId(projectId);
        List<SprintDTO> sprintDTOList = sprintMapper.selectNotDoneByProjectId(projectId);
        List<String> versionList = new ArrayList<>();
        for (ProductVersionCommonDTO productVersionCommonDTO : productVersionCommonDTOList) {
            if (VERSION_PLANNING.equals(productVersionCommonDTO.getStatusCode())) {
                versionMap.put(productVersionCommonDTO.getName(), productVersionCommonDTO.getVersionId());
                versionList.add(productVersionCommonDTO.getName());
            }
        }
        List<String> componentList = new ArrayList<>();
        for (IssueComponentDTO issueComponentDTO : issueComponentDTOList) {
            componentList.add(issueComponentDTO.getName());
            componentMap.put(issueComponentDTO.getName(), issueComponentDTO.getComponentId());
        }
        List<String> sprintList = new ArrayList<>();
        for (SprintDTO sprintDTO : sprintDTOList) {
            sprintList.add(sprintDTO.getSprintName());
            sprintMap.put(sprintDTO.getSprintName(), sprintDTO.getSprintId());
        }
        //第二列为所属史诗
        Map<String, Long> theSecondColumnMap = getEpicMap(projectId);

        Map<String, Long> managerMap = getManagers(projectId);
        List<String> managers = new ArrayList<>(managerMap.keySet());

        List<Long> importedIssueIds = new ArrayList<>();

        Map<Integer, String> allIssueType = new LinkedHashMap<>();
        List<IssueTypeLinkDTO> issueTypeLinks = getAllIssueTypeLinks(allRowCount, sheet, columnNum, allIssueType);
        Map<Integer, Set<Integer>> parentSonMap = getParentSonMap(issueTypeLinks);
        Map<Integer, Integer> sonParentMap = getSonParentMap(parentSonMap);
        //获取无父节点的子任务
        Set<Integer> illegalRow = getIllegalRow(allIssueType, sonParentMap);

        for (int r = 1; r <= allRowCount; r++) {
            if (checkCanceled(projectId, res.getId(), importedIssueIds)) {
                return;
            }
            Row row = sheet.getRow(r);
            if (isSkip(row, columnNum)) {
                continue;
            }
            for (int w = 0; w < columnNum; w++) {
                if (row.getCell(w) != null) {
                    row.getCell(w).setCellType(XSSFCell.CELL_TYPE_STRING);
                }
            }

            String typeName = allIssueType.get(r);
            //有子节点的故事和任务，要和子节点一块校验，有一个不合法，则全为错误的
            Set<Integer> set = parentSonMap.get(r);
            Boolean hasSonNodes = (set != null && !set.isEmpty());
            if ((STORY_CN.equals(typeName)
                    || TASK_CN.equals(typeName)
                    || BUG_CN.equals(typeName))
                    && hasSonNodes) {
                Map<String, Object> returnMap = batchCheck(projectId, sheet, issueTypeList, priorityList,
                        versionList, issueTypeMap, componentList, sprintList, r, illegalRow, set, columnNum,
                        theSecondColumnMap.keySet(), managers);
                Map<Integer, Map<Integer, String>> errorMaps = (Map<Integer, Map<Integer, String>>) returnMap.get("errorMap");
                set = (Set<Integer>) returnMap.get("sonSet");
                if (!errorMaps.isEmpty()) {
                    int size = errorMaps.size();
                    failCount = failCount + size;
                    for (Map.Entry<Integer, Map<Integer, String>> entry : errorMaps.entrySet()) {
                        int rowNum = entry.getKey();
                        Map<Integer, String> errorMap = entry.getValue();
                        processErrorMap(errorMapList, rowNum, sheet.getRow(rowNum), errorMap, errorRows);
                    }
                    res.setFailCount(failCount);
                    processNum = processNum + size;
                    sendProcess(res, userId, processNum * 1.0 / allRowCount);
                    //设置for循环的指针为子节点的最大行数
                    r = Collections.max(set);
                    continue;
                }

                Set<Long> insertIds = batchInsert(projectId, r, issueTypeMap, priorityMap, versionMap,
                        userId, componentMap, sprintMap, sheet, set, managerMap, sonParentMap, subTask, theSecondColumnMap);
                if (insertIds.isEmpty()) {
                    failCount = failCount + set.size() + 1;
                    errorRows.add(r);
                    errorRows.addAll(set);
                } else {
                    importedIssueIds.addAll(insertIds);
                    successCount = successCount + insertIds.size();
                }
                r = Collections.max(set);
            } else {
                Map<Integer, String> errorMap = checkRule(projectId, sheet, issueTypeList, priorityList,
                        versionList, componentList, sprintList, r, illegalRow, theSecondColumnMap.keySet(), managers);
                if (!errorMap.isEmpty()) {
                    failCount++;
                    processErrorMap(errorMapList, r, row, errorMap, errorRows);
                    res.setFailCount(failCount);
                    processNum++;
                    sendProcess(res, userId, processNum * 1.0 / allRowCount);
                    continue;
                }
                IssueCreateVO issueCreateVO = new IssueCreateVO();
                Boolean ok = setIssueCreateInfo(issueCreateVO, projectId, issueTypeMap, priorityMap,
                        versionMap, userId, componentMap, sprintMap, managerMap, r, sheet, sonParentMap,
                        subTask, theSecondColumnMap);

                IssueVO result = null;
                if (ok) {
                    result = stateMachineClientService.createIssue(issueCreateVO, APPLY_TYPE_AGILE);
                }
                if (result == null) {
                    failCount++;
                    errorRows.add(row.getRowNum());
                } else {
                    importedIssueIds.add(result.getIssueId());
                    successCount++;
                }
            }
            processNum++;
            res.setFailCount(failCount);
            res.setSuccessCount(successCount);
            sendProcess(res, userId, processNum * 1.0 / allRowCount);
        }


        if (!errorRows.isEmpty()) {
            LOGGER.info("导入数据有误");
            PredefinedDTO theSecondColumnPredefined = getEpicPredefined(projectId);
            Workbook result = ExcelUtil.generateExcelAwesome(workbook, errorRows,
                    errorMapList, FIELDS_NAME, priorityList, issueTypeList, versionList,
                    IMPORT_TEMPLATE_NAME, componentList, sprintList, managers,
                    theSecondColumnPredefined, false);
            String errorWorkBookUrl = uploadErrorExcel(result, organizationId);
            res.setFileUrl(errorWorkBookUrl);
            status = FAILED;
        } else {
            status = SUCCESS;
        }
        updateFinalRecode(res, successCount, failCount, status);
    }

    protected Set<Long> batchInsert(Long projectId, int rowNum, Map<String, IssueTypeVO> issueTypeMap,
                                    Map<String, Long> priorityMap, Map<String, Long> versionMap,
                                    Long userId, Map<String, Long> componentMap, Map<String, Long> sprintMap,
                                    Sheet sheet, Set<Integer> set, Map<String, Long> managerMap,
                                    Map<Integer, Integer> sonParentMap, IssueTypeVO subTask,
                                    Map<String, Long> theSecondColumnMap) {
        Set<Long> issueIds = new HashSet<>();
        //插入父节点
        IssueCreateVO issueCreateVO = new IssueCreateVO();

        Boolean ok = setIssueCreateInfo(issueCreateVO, projectId, issueTypeMap, priorityMap,
                versionMap, userId, componentMap, sprintMap, managerMap, rowNum, sheet, sonParentMap, subTask, theSecondColumnMap);
        IssueVO parent = null;
        if (ok) {
            parent = stateMachineClientService.createIssue(issueCreateVO, APPLY_TYPE_AGILE);
        }
        if (parent == null) {
            return issueIds;
        }
        Long parentId = parent.getIssueId();
        issueIds.add(parentId);
        //处理子节点
        set.forEach(s -> {
            IssueCreateVO issueCreate = new IssueCreateVO();
            Boolean success = setIssueCreateInfo(issueCreate, projectId, issueTypeMap, priorityMap,
                    versionMap, userId, componentMap, sprintMap, managerMap, s, sheet, sonParentMap, subTask, theSecondColumnMap);
            if (success) {
                String typeCode = issueCreate.getTypeCode();
                if (SUB_TASK.equals(typeCode)) {
                    issueCreate.setParentIssueId(parentId);
                }
                if (issueTypeMap.get(BUG_CN).getTypeCode().equals(typeCode)) {
                    issueCreate.setRelateIssueId(parentId);
                }
                IssueVO result = stateMachineClientService.createIssue(issueCreate, APPLY_TYPE_AGILE);
                if (result != null) {
                    issueIds.add(result.getIssueId());
                }
            }
        });
        if (set.size() + 1 == issueIds.size()) {
            return issueIds;
        } else {
            return new HashSet<>();
        }
    }

    protected Map<String, Object> batchCheck(Long projectId, Sheet sheet, List<String> issueTypeList,
                                             List<String> priorityList, List<String> versionList, Map<String, IssueTypeVO> issueTypeMap,
                                             List<String> componentList, List<String> sprintList, int rowNum,
                                             Set<Integer> illegalRow, Set<Integer> sonSet, int columnNum,
                                             Set<String> theSecondColumn, List<String> managers) {
        //key为row,value为错误信息
        Map<Integer, Map<Integer, String>> map = new HashMap<>();
        //key为列，value为错误详情，先判断父节点
        Map<Integer, String> errorMap = checkRule(projectId, sheet, issueTypeList, priorityList,
                versionList, componentList, sprintList, rowNum, illegalRow, theSecondColumn, managers);
        Set<Integer> newSet = new HashSet<>();
        if (!errorMap.isEmpty()) {
            map.put(rowNum, errorMap);
            newSet.addAll(sonSet);
        } else {
            sonSet.forEach(r -> {
                Row row = sheet.getRow(r);
                if (isSkip(row, columnNum)) {
                    return;
                }
                for (int w = 0; w < FIELDS_NAME.length; w++) {
                    if (row.getCell(w) != null) {
                        row.getCell(w).setCellType(XSSFCell.CELL_TYPE_STRING);
                    }
                }
                newSet.add(r);
                Map<Integer, String> error = checkRule(projectId, sheet, issueTypeList, priorityList,
                        versionList, componentList, sprintList, r, illegalRow, theSecondColumn, managers);
                if (!error.isEmpty()) {
                    map.put(r, error);
                }
            });
        }
        //如果有一行有问题，全部置为失败
        if (!map.isEmpty()) {
            fillInErrorMap(map, rowNum);
            newSet.forEach(n -> fillInErrorMap(map, n));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("errorMap", map);
        result.put("sonSet", newSet);

        return result;
    }

    protected void fillInErrorMap(Map<Integer, Map<Integer, String>> map, int rowNum) {
        Map<Integer, String> error = map.get(rowNum);
        if (ObjectUtils.isEmpty(error)) {
            error = new HashMap<>();
            map.put(rowNum, error);
        }
        error.put(5, "父子结构中有错误数据");
    }

    protected Set<Integer> getIllegalRow(Map<Integer, String> allIssueType, Map<Integer, Integer> sonParentMap) {
        Set<Integer> set = new HashSet<>();
        for (Map.Entry<Integer, String> entry : allIssueType.entrySet()) {
            Integer key = entry.getKey();
            String value = entry.getValue();
            if (SUB_TASK_CN.equals(value)) {
                //无父节点
                if (sonParentMap.get(key) == null) {
                    set.add(key);
                }
            }
        }
        return set;
    }

    protected Map<Integer, Integer> getSonParentMap(Map<Integer, Set<Integer>> parentSonMap) {
        Map<Integer, Integer> map = new HashMap<>();
        for (Map.Entry<Integer, Set<Integer>> entry : parentSonMap.entrySet()) {
            Integer key = entry.getKey();
            Set<Integer> value = entry.getValue();
            value.forEach(i -> map.put(i, key));
        }
        return map;
    }

    protected Map<Integer, Set<Integer>> getParentSonMap(List<IssueTypeLinkDTO> issueTypeLinks) {
        Map<Integer, Set<Integer>> map = new HashMap<>();
        for (IssueTypeLinkDTO issueTypeLink : issueTypeLinks) {
            Integer row = issueTypeLink.getRow();
            String type = issueTypeLink.getType();
            //故事下只有子任务
            if (STORY_CN.equals(type)) {
                storyRecursive(map, issueTypeLink, row);
            }
            //任务或缺陷下的子任务
            if (TASK_CN.equals(type) || BUG_CN.equals(type)) {
                taskRecursive(map, issueTypeLink, row);
            }
        }
        return map;
    }

    private void taskRecursive(Map<Integer, Set<Integer>> map, IssueTypeLinkDTO issueTypeLink, Integer row) {
        if (issueTypeLink.hasNext()) {
            IssueTypeLinkDTO next = issueTypeLink.getNext();
            String nextType = next.getType();
            Integer nextRow = next.getRow();
            if (SUB_TASK_CN.equals(nextType)) {
                processSonRow(map, row, nextRow);
                taskRecursive(map, next, row);
            }
        }
    }

    private void processSonRow(Map<Integer, Set<Integer>> map, Integer row, Integer nextRow) {
        Set<Integer> set = map.get(row);
        if (set == null) {
            set = new HashSet<>();
            set.add(nextRow);
            map.put(row, set);
        } else {
            set.add(nextRow);
        }
    }

    private void storyRecursive(Map<Integer, Set<Integer>> map, IssueTypeLinkDTO issueTypeLink,
                                Integer row) {
        if (issueTypeLink.hasNext()) {
            IssueTypeLinkDTO next = issueTypeLink.getNext();
            String nextType = next.getType();
            Integer nextRow = next.getRow();
            if (SUB_TASK_CN.equals(nextType)) {
                processSonRow(map, row, nextRow);
                storyRecursive(map, next, row);
            }
        }
    }

    protected List<IssueTypeLinkDTO> getAllIssueTypeLinks(Integer allRowCount, Sheet sheet, int columnNum, Map<Integer, String> allIssueType) {
        List<IssueTypeLinkDTO> issueTypeLinks = new ArrayList<>();
        for (int i = 1; i <= allRowCount; i++) {
            int size = issueTypeLinks.size();
            IssueTypeLinkDTO lastIssueTypeLink = null;
            if (size > 0) {
                lastIssueTypeLink = issueTypeLinks.get(size - 1);
            }
            Row row = sheet.getRow(i);
            if (isSkip(row, columnNum)) {
                continue;
            }
            String type = getTypeName(row);
            if (type == null) {
                continue;
            }
            IssueTypeLinkDTO issueTypeLink = new IssueTypeLinkDTO(i, type);
            issueTypeLinks.add(issueTypeLink);
            if (lastIssueTypeLink != null) {
                lastIssueTypeLink.setNext(issueTypeLink);
            }
            allIssueType.put(i, type);
        }
        return issueTypeLinks;
    }

    protected String getTypeName(Row row) {
        Cell issueTypeCell = row.getCell(0);
        Cell subTaskCell = row.getCell(5);
        if (isCellEmpty(issueTypeCell) && !isCellEmpty(subTaskCell)) {
            return SUB_TASK_CN;
        } else if (!isCellEmpty(issueTypeCell)) {
            return issueTypeCell.toString();
        } else {
            return null;
        }
    }

    protected void processErrorMap(Map<Integer, List<Integer>> errorMapList,
                                   int r, Row row, Map<Integer, String> errorMap,
                                   List<Integer> errorRows) {
        Iterator<Map.Entry<Integer, String>> entries = errorMap.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<Integer, String> entry = entries.next();
            Integer key = entry.getKey();
            String value = entry.getValue();
            if (row.getCell(key) == null) {
                row.createCell(key).setCellValue("(" + value + ")");
            } else {
                row.getCell(key).setCellValue(row.getCell(key).toString() + " (" + value + ")");
            }

            List<Integer> cList = errorMapList.get(r);
            if (cList == null) {
                cList = new ArrayList<>();
            }
            cList.add(key);
            errorMapList.put(r, cList);
        }
        errorRows.add(row.getRowNum());
    }

    protected void validateWorkbook(Long projectId, Long userId, Workbook workbook, FileOperationHistoryDTO res,
                                    String[] headers) {
        if (workbook.getActiveSheetIndex() < 1
                || workbook.getSheetAt(1) == null
                || workbook.getSheetAt(1).getSheetName() == null
                || !IMPORT_TEMPLATE_NAME.equals(workbook.getSheetAt(1).getSheetName())
                || isOldExcel(workbook, headers)) {
            if (fileOperationHistoryMapper.updateByPrimaryKeySelective(new FileOperationHistoryDTO(projectId, res.getId(), UPLOAD_FILE, "template_error", res.getObjectVersionNumber())) != 1) {
                throw new CommonException("error.FileOperationHistoryDTO.update");
            }
            FileOperationHistoryDTO errorImport = fileOperationHistoryMapper.selectByPrimaryKey(res.getId());
            sendProcess(errorImport, userId, 0.0);
            throw new CommonException("error.sheet.import");
        }
    }

    private boolean isOldExcel(Workbook workbook, String[] headers) {
        //判断是否为旧模版
        Sheet sheet = workbook.getSheetAt(1);
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            return true;
        }
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i];
            Cell cell = headerRow.getCell(i);
            if (isCellEmpty(cell)) {
                return true;
            }
            if (!header.equals(cell.toString())) {
                return true;
            }
        }
        return false;
    }

    protected FileOperationHistoryDTO initFileOperationHistory(Long projectId, Long userId, String status) {
        FileOperationHistoryDTO fileOperationHistoryDTO = new FileOperationHistoryDTO(projectId, userId, UPLOAD_FILE, 0L, 0L, status);
        if (fileOperationHistoryMapper.insert(fileOperationHistoryDTO) != 1) {
            throw new CommonException("error.FileOperationHistoryDTO.insert");
        }
        FileOperationHistoryDTO res = fileOperationHistoryMapper.selectByPrimaryKey(fileOperationHistoryDTO.getId());
        sendProcess(res, userId, 0.0);
        return res;
    }


    @Override
    public void cancelImport(Long projectId, Long id, Long objectVersionNumber) {
        FileOperationHistoryDTO fileOperationHistoryDTO = new FileOperationHistoryDTO();
        fileOperationHistoryDTO.setId(id);
        fileOperationHistoryDTO.setStatus(CANCELED);
        fileOperationHistoryDTO.setObjectVersionNumber(objectVersionNumber);
        if (fileOperationHistoryMapper.updateByPrimaryKeySelective(fileOperationHistoryDTO) != 1) {
            throw new CommonException("error.FileOperationHistoryDTO.update");
        }
    }


    @Override
    public FileOperationHistoryVO queryLatestRecode(Long projectId) {
        Long userId = DetailsHelper.getUserDetails().getUserId();
        FileOperationHistoryDTO result = fileOperationHistoryMapper.queryLatestRecode(projectId, userId);
        return result == null ? new FileOperationHistoryVO() : modelMapper.map(result, FileOperationHistoryVO.class);
    }
}
