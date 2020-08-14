package io.choerodon.agile.app.assembler;

import com.google.common.collect.Lists;

import io.choerodon.agile.api.vo.*;
import io.choerodon.agile.app.service.UserService;
import io.choerodon.agile.infra.enums.IssueTypeCode;
import io.choerodon.agile.infra.enums.SchemeApplyType;
import io.choerodon.agile.infra.enums.StatusType;
import io.choerodon.agile.infra.utils.ConvertUtil;
import io.choerodon.agile.infra.dto.*;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hzero.core.base.BaseConstants;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import rx.Observable;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author dinghuang123@gmail.com
 */
@Component
public class IssueAssembler extends AbstractAssembler {

    @Autowired
    private UserService userService;
    @Autowired
    private SprintNameAssembler sprintNameAssembler;
    @Autowired
    private ModelMapper modelMapper;

    /**
     * issueDetailDO转换到IssueDTO
     *
     * @param issueDetailDTO issueDetailDTO
     * @return IssueVO
     */
    public IssueVO issueDetailDTOToVO(IssueDetailDTO issueDetailDTO, Map<Long, IssueTypeVO> issueTypeDTOMap, Map<Long, StatusVO> statusMapDTOMap, Map<Long, PriorityVO> priorityDTOMap) {
        IssueVO issueVO = new IssueVO();
        BeanUtils.copyProperties(issueDetailDTO, issueVO);
        issueVO.setComponentIssueRelVOList(modelMapper.map(issueDetailDTO.getComponentIssueRelDTOList(), new TypeToken<List<ComponentIssueRelVO>>(){}.getType()));
        issueVO.setActiveSprint(sprintNameAssembler.toTarget(issueDetailDTO.getActiveSprint(), SprintNameVO.class));
        issueVO.setCloseSprint(sprintNameAssembler.toTargetList(issueDetailDTO.getCloseSprint(), SprintNameVO.class));
        issueVO.setVersionIssueRelVOList(modelMapper.map(issueDetailDTO.getVersionIssueRelDTOList(), new TypeToken<List<VersionIssueRelVO>>(){}.getType()));
        issueVO.setLabelIssueRelVOList(modelMapper.map(issueDetailDTO.getLabelIssueRelDTOList(), new TypeToken<List<LabelIssueRelVO>>(){}.getType()));
        issueVO.setIssueAttachmentVOList(modelMapper.map(issueDetailDTO.getIssueAttachmentDTOList(), new TypeToken<List<IssueAttachmentVO>>(){}.getType()));
        issueVO.setIssueCommentVOList(modelMapper.map(issueDetailDTO.getIssueCommentDTOList(), new TypeToken<List<IssueCommentVO>>(){}.getType()));
        issueVO.setSubIssueVOList(issueDoToSubIssueDto(issueDetailDTO.getSubIssueDTOList(), issueTypeDTOMap, statusMapDTOMap, priorityDTOMap));
        issueVO.setSubBugVOList(issueDoToSubIssueDto(issueDetailDTO.getSubBugDOList(), issueTypeDTOMap, statusMapDTOMap, priorityDTOMap));
        issueVO.setPriorityVO(priorityDTOMap.get(issueVO.getPriorityId()));
        issueVO.setIssueTypeVO(issueTypeDTOMap.get(issueVO.getIssueTypeId()));
        issueVO.setStatusVO(statusMapDTOMap.get(issueVO.getStatusId()));
        List<Long> assigneeIdList = new ArrayList<>();
        assigneeIdList.add(issueDetailDTO.getAssigneeId());
        assigneeIdList.add(issueDetailDTO.getReporterId());
        assigneeIdList.add(issueDetailDTO.getCreatedBy());
        Boolean issueCommentCondition = issueVO.getIssueCommentVOList() != null && !issueVO.getIssueCommentVOList().isEmpty();
        if (issueCommentCondition) {
            assigneeIdList.addAll(issueVO.getIssueCommentVOList().stream().map(IssueCommentVO::getUserId).collect(Collectors.toList()));
        }
        Map<Long, UserMessageDTO> userMessageDOMap = userService.queryUsersMap(
                assigneeIdList.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList()), true);
        UserMessageDTO assigneeUserDO = userMessageDOMap.get(issueVO.getAssigneeId());
        UserMessageDTO reporterUserDO = userMessageDOMap.get(issueVO.getReporterId());
        UserMessageDTO createrUserDO = userMessageDOMap.get(issueVO.getCreatedBy());
        String assigneeName = assigneeUserDO != null ? assigneeUserDO.getName() : null;
        String assigneeLoginName = assigneeUserDO != null ? assigneeUserDO.getLoginName() : null;
        String assigneeRealName = assigneeUserDO != null ? assigneeUserDO.getRealName() : null;
        String reporterName = reporterUserDO != null ? reporterUserDO.getName() : null;
        String reporterLoginName = reporterUserDO != null ? reporterUserDO.getLoginName() : null;
        String reporterRealName = reporterUserDO != null ? reporterUserDO.getRealName() : null;
        issueVO.setAssigneeName(assigneeName);
        issueVO.setAssigneeImageUrl(assigneeName != null ? userMessageDOMap.get(issueVO.getAssigneeId()).getImageUrl() : null);
        issueVO.setReporterName(reporterName);
        issueVO.setReporterImageUrl(reporterName != null ? userMessageDOMap.get(issueVO.getReporterId()).getImageUrl() : null);
        issueVO.setCreaterName(createrUserDO != null ? createrUserDO.getName() : null);
        issueVO.setCreaterLoginName(createrUserDO != null ? createrUserDO.getLoginName() : null);
        issueVO.setCreaterRealName(createrUserDO != null ? createrUserDO.getRealName() : null);
        issueVO.setCreaterImageUrl(createrUserDO != null ? createrUserDO.getImageUrl() : null);
        issueVO.setCreaterEmail(createrUserDO != null ? createrUserDO.getEmail() : null);
        issueVO.setAssigneeLoginName(assigneeLoginName);
        issueVO.setAssigneeRealName(assigneeRealName);
        issueVO.setReporterLoginName(reporterLoginName);
        issueVO.setReporterRealName(reporterRealName);
        if (issueCommentCondition) {
            issueVO.getIssueCommentVOList().forEach(issueCommentDTO -> {
                UserMessageDTO commentUser = userMessageDOMap.get(issueCommentDTO.getUserId());
                issueCommentDTO.setUserName(commentUser != null ? commentUser.getName() : null);
                issueCommentDTO.setUserLoginName(commentUser != null ? commentUser.getLoginName() : null);
                issueCommentDTO.setUserRealName(commentUser != null ? commentUser.getRealName() : null);
                issueCommentDTO.setUserImageUrl(commentUser != null ? commentUser.getImageUrl() : null);
            });
        }
        return issueVO;
    }

    /**
     * issueDO转换到IssueListFieldKVDTO
     *
     * @param issueDTOList issueDetailDO
     * @return IssueListFieldKVVO
     */

    public List<IssueListFieldKVVO> issueDoToIssueListFieldKVDTO(List<IssueDTO> issueDTOList, Map<Long, PriorityVO> priorityMap, Map<Long, StatusVO> statusMapDTOMap, Map<Long, IssueTypeVO> issueTypeDTOMap, Map<Long, Map<String, Object>> foundationCodeValue) {
        List<IssueListFieldKVVO> issueListFieldKVDTOList = new ArrayList<>(issueDTOList.size());
        Set<Long> userIds = issueDTOList.stream().filter(issue -> issue.getAssigneeId() != null && !Objects.equals(issue.getAssigneeId(), 0L)).map(IssueDTO::getAssigneeId).collect(Collectors.toSet());
        userIds.addAll(issueDTOList.stream().filter(issue -> issue.getReporterId() != null && !Objects.equals(issue.getReporterId(), 0L)).map(IssueDTO::getReporterId).collect(Collectors.toSet()));
        Map<Long, UserMessageDTO> usersMap = userService.queryUsersMap(Lists.newArrayList(userIds), true);
        issueDTOList.forEach(issueDO -> {
            UserMessageDTO assigneeUserDO = usersMap.get(issueDO.getAssigneeId());
            UserMessageDTO reporterUserDO = usersMap.get(issueDO.getReporterId());
            String assigneeName = assigneeUserDO != null ? assigneeUserDO.getName() : null;
            String assigneeLoginName = assigneeUserDO != null ? assigneeUserDO.getLoginName() : null;
            String assigneeRealName = assigneeUserDO != null ? assigneeUserDO.getRealName() : null;
            String reporterName = reporterUserDO != null ? reporterUserDO.getName() : null;
            String reporterLoginName = reporterUserDO != null ? reporterUserDO.getLoginName() : null;
            String reporterRealName = reporterUserDO != null ? reporterUserDO.getRealName() : null;
            String assigneeImageUrl = assigneeUserDO != null ? assigneeUserDO.getImageUrl() : null;
            String reporterImageUrl = reporterUserDO != null ? reporterUserDO.getImageUrl() : null;
            IssueListFieldKVVO issueListFieldKVVO = toTarget(issueDO, IssueListFieldKVVO.class);
            issueListFieldKVVO.setAssigneeName(assigneeName);
            issueListFieldKVVO.setAssigneeLoginName(assigneeLoginName);
            issueListFieldKVVO.setAssigneeRealName(assigneeRealName);
            issueListFieldKVVO.setReporterName(reporterName);
            issueListFieldKVVO.setReporterLoginName(reporterLoginName);
            issueListFieldKVVO.setReporterRealName(reporterRealName);
            issueListFieldKVVO.setPriorityVO(priorityMap.get(issueDO.getPriorityId()));
            issueListFieldKVVO.setIssueTypeVO(issueTypeDTOMap.get(issueDO.getIssueTypeId()));
            issueListFieldKVVO.setStatusVO(statusMapDTOMap.get(issueDO.getStatusId()));
            issueListFieldKVVO.setAssigneeImageUrl(assigneeImageUrl);
            issueListFieldKVVO.setReporterImageUrl(reporterImageUrl);
            issueListFieldKVVO.setVersionIssueRelVOS(toTargetList(issueDO.getVersionIssueRelDTOS(), VersionIssueRelVO.class));
            issueListFieldKVVO.setIssueComponentBriefVOS(toTargetList(issueDO.getIssueComponentBriefDTOS(), IssueComponentBriefVO.class));
            issueListFieldKVVO.setIssueSprintVOS(toTargetList(issueDO.getIssueSprintDTOS(), IssueSprintVO.class));
            issueListFieldKVVO.setLabelIssueRelVOS(toTargetList(issueDO.getLabelIssueRelDTOS(), LabelIssueRelVO.class));
            issueListFieldKVVO.setFoundationFieldValue(foundationCodeValue.get(issueDO.getIssueId()) != null ? foundationCodeValue.get(issueDO.getIssueId()) : new HashMap<>());
            setParentId(issueListFieldKVVO, issueDO);
            issueListFieldKVDTOList.add(issueListFieldKVVO);
        });
        return issueListFieldKVDTOList;
    }

    /**
     * issueDTO转换为IssueCountVO
     * @param issueList issueList
     * @param priority 在priority内的user优先排序
     * @return IssueCountVO
     */
    public List<IssueCompletedStatusVO> issueDTOToIssueCountVO(List<IssueOverviewVO> issueList, Set<Long> priority){
        Set<Long> userIdList = new HashSet<>();
        rx.Observable.from(priority)
                .mergeWith(Observable.from(issueList.stream().map(IssueOverviewVO::getCreatedBy).collect(Collectors.toSet())))
                .toList().subscribe(userIdList::addAll);
        Map<Long, UserMessageDTO> userMap = userService.queryUsersMap(new ArrayList<>(userIdList), true);
        // 设置提出list
        List<Map.Entry<String, Integer>> createdlist = sortAndConvertCreated(issueList, priority, userMap);
        // 设置已解决Map
        Map<String, Integer> assigneeMap = sortAndConvertAssignee(issueList, userMap);
        List<IssueCompletedStatusVO> result = createdlist.stream()
                .map(entry -> new IssueCompletedStatusVO(entry.getKey(), entry.getValue())).collect(Collectors.toList());
        result.addAll(priority.stream()
                .map(userId -> userMap.get(userId).getRealName())
                .filter(realName -> !createdlist.stream().map(Map.Entry::getKey).collect(Collectors.toSet())
                        .contains(realName)).map(IssueCompletedStatusVO::new)
                .collect(Collectors.toList()));
        // 设置同一工作人的已解决问题数，并移除掉
        for (IssueCompletedStatusVO issue : result) {
            if (assigneeMap.containsKey(issue.getWorker())){
                issue.setCompleted(assigneeMap.get(issue.getWorker()));
                assigneeMap.remove(issue.getWorker());
            }
        }
        // 将剩余的人（即仅解决bug无创建bug的人）加入list
        result.addAll(assigneeMap.entrySet().stream()
                .map(entry -> new IssueCompletedStatusVO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()));
        return result;
    }

    /**
     * 过滤掉未完成的issue再进行排序
     * @param issueList 待排序list
     * @return 坐标点list
     */
    private Map<String, Integer> sortAndConvertAssignee(List<IssueOverviewVO> issueList, Map<Long, UserMessageDTO> userMap) {
        return issueList.stream()
                .filter(issue -> BooleanUtils.isTrue(issue.getCompleted()) && Objects.nonNull(issue.getAssigneeId()))
                .collect(Collectors.groupingBy(IssueOverviewVO::getAssigneeId)).entrySet()
                .stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> new ImmutablePair<>(userMap.get(entry.getKey()).getRealName(), entry.getValue().size()))
                .collect(Collectors.toMap(ImmutablePair::getLeft, ImmutablePair::getRight));
    }


    /**
     * 创建人是经办人或报告人时，排在前面
     * @param issueList 待排序list
     * @param priority 优先set
     * @param userMap 用户map
     * @return 坐标点list
     */
    private List<Map.Entry<String, Integer>> sortAndConvertCreated(List<IssueOverviewVO> issueList, Set<Long> priority, Map<Long, UserMessageDTO> userMap) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(issueList.size());
        Map<Boolean, List<IssueOverviewVO>> group = issueList.stream().collect(Collectors.groupingBy(issue -> priority.contains(issue.getCreatedBy())));
        list.addAll(group.getOrDefault(Boolean.TRUE, Collections.emptyList())
                .stream().collect(Collectors.groupingBy(IssueOverviewVO::getCreatedBy)).entrySet()
                .stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> new ImmutablePair<>(userMap.get(entry.getKey()).getRealName(), entry.getValue().size()))
                .collect(Collectors.toList()));
        list.addAll(group.getOrDefault(Boolean.FALSE, Collections.emptyList())
                .stream().collect(Collectors.groupingBy(IssueOverviewVO::getCreatedBy)).entrySet()
                .stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> new ImmutablePair<>(userMap.get(entry.getKey()).getRealName(), entry.getValue().size()))
                .collect(Collectors.toList()));
        return list;
    }

    private void setParentId(IssueListFieldKVVO issueListFieldKVVO, IssueDTO issue) {
        Long parentId = null;
        Long parentIssueId = issue.getParentIssueId();
        Long relateIssueId = issue.getRelateIssueId();
        if (!ObjectUtils.isEmpty(parentIssueId) && parentIssueId != 0) {
            parentId = parentIssueId;
        }
        if (!ObjectUtils.isEmpty(relateIssueId) && relateIssueId != 0) {
            parentId = relateIssueId;
        }
        issueListFieldKVVO.setParentId(parentId);
    }


    /**
     * issueDO转换到IssueListDTO
     *
     * @param issueDTOList issueDetailDO
     * @return IssueListVO
     */
    public List<IssueListVO> issueDoToIssueListDto(List<IssueDTO> issueDTOList, Map<Long, PriorityVO> priorityMap, Map<Long, StatusVO> statusMapDTOMap, Map<Long, IssueTypeVO> issueTypeDTOMap) {
        List<IssueListVO> issueListDTOList = new ArrayList<>(issueDTOList.size());
        Set<Long> userIds = issueDTOList.stream().filter(issue -> issue.getAssigneeId() != null && !Objects.equals(issue.getAssigneeId(), 0L)).map(IssueDTO::getAssigneeId).collect(Collectors.toSet());
        userIds.addAll(issueDTOList.stream().filter(issue -> issue.getReporterId() != null && !Objects.equals(issue.getReporterId(), 0L)).map(IssueDTO::getReporterId).collect(Collectors.toSet()));
        Map<Long, UserMessageDTO> usersMap = userService.queryUsersMap(Lists.newArrayList(userIds), true);
        issueDTOList.forEach(issueDO -> {
            UserMessageDTO assigneeUserDO = usersMap.get(issueDO.getAssigneeId());
            UserMessageDTO reporterUserDO = usersMap.get(issueDO.getReporterId());
            String assigneeName = assigneeUserDO != null ? assigneeUserDO.getName() : null;
            String assigneeLoginName = assigneeUserDO != null ? assigneeUserDO.getLoginName() : null;
            String assigneeRealName = assigneeUserDO != null ? assigneeUserDO.getRealName() : null;
            String reporterName = reporterUserDO != null ? reporterUserDO.getName() : null;
            String reporterLoginName = reporterUserDO != null ? reporterUserDO.getLoginName() : null;
            String reporterRealName = reporterUserDO != null ? reporterUserDO.getRealName() : null;
            String assigneeImageUrl = assigneeUserDO != null ? assigneeUserDO.getImageUrl() : null;
            String reporterImageUrl = reporterUserDO != null ? reporterUserDO.getImageUrl() : null;
            IssueListVO issueListVO = toTarget(issueDO, IssueListVO.class);
            issueListVO.setAssigneeName(assigneeName);
            issueListVO.setAssigneeLoginName(assigneeLoginName);
            issueListVO.setAssigneeRealName(assigneeRealName);
            issueListVO.setReporterName(reporterName);
            issueListVO.setReporterLoginName(reporterLoginName);
            issueListVO.setReporterRealName(reporterRealName);
            issueListVO.setPriorityVO(priorityMap.get(issueDO.getPriorityId()));
            issueListVO.setIssueTypeVO(issueTypeDTOMap.get(issueDO.getIssueTypeId()));
            issueListVO.setStatusVO(statusMapDTOMap.get(issueDO.getStatusId()));
            issueListVO.setAssigneeImageUrl(assigneeImageUrl);
            issueListVO.setReporterImageUrl(reporterImageUrl);
            issueListVO.setVersionIssueRelVOS(toTargetList(issueDO.getVersionIssueRelDTOS(), VersionIssueRelVO.class));
            issueListVO.setIssueComponentBriefVOS(toTargetList(issueDO.getIssueComponentBriefDTOS(), IssueComponentBriefVO.class));
            issueListVO.setIssueSprintVOS(toTargetList(issueDO.getIssueSprintDTOS(), IssueSprintVO.class));
            issueListVO.setLabelIssueRelVOS(toTargetList(issueDO.getLabelIssueRelDTOS(), LabelIssueRelVO.class));
            issueListDTOList.add(issueListVO);
        });
        return issueListDTOList;
    }

    /**
     * issueDO转换到subIssueDTO
     *
     * @param issueDTOList issueDTOList
     * @return SubIssueDTO
     */
    protected List<IssueSubListVO> issueDoToSubIssueDto(List<IssueDTO> issueDTOList, Map<Long, IssueTypeVO> issueTypeDTOMap, Map<Long, StatusVO> statusMapDTOMap, Map<Long, PriorityVO> priorityDTOMap) {
        List<IssueSubListVO> subIssueVOList = new ArrayList<>(issueDTOList.size());
        List<Long> assigneeIds = issueDTOList.stream().filter(issue -> issue.getAssigneeId() != null && !Objects.equals(issue.getAssigneeId(), 0L)).map(IssueDTO::getAssigneeId).distinct().collect(Collectors.toList());
        Map<Long, UserMessageDTO> usersMap = userService.queryUsersMap(assigneeIds, true);
        issueDTOList.forEach(issueDO -> {
            UserMessageDTO userMessageDTO = usersMap.get(issueDO.getAssigneeId());
            String assigneeName = userMessageDTO != null ? userMessageDTO.getName() : null;
            String imageUrl = userMessageDTO != null ? userMessageDTO.getImageUrl() : null;
            String loginName = userMessageDTO != null ? userMessageDTO.getLoginName() : null;
            String realName = userMessageDTO != null ? userMessageDTO.getRealName() : null;
            IssueSubListVO subIssueDTO = new IssueSubListVO();
            BeanUtils.copyProperties(issueDO, subIssueDTO);
            subIssueDTO.setAssigneeName(assigneeName);
            subIssueDTO.setImageUrl(imageUrl);
            subIssueDTO.setLoginName(loginName);
            subIssueDTO.setRealName(realName);
            subIssueDTO.setPriorityVO(priorityDTOMap.get(issueDO.getPriorityId()));
            subIssueDTO.setIssueTypeVO(issueTypeDTOMap.get(issueDO.getIssueTypeId()));
            subIssueDTO.setStatusVO(statusMapDTOMap.get(issueDO.getStatusId()));
            subIssueVOList.add(subIssueDTO);
        });
        return subIssueVOList;
    }

    /**
     * issueDetailDO转换到IssueSubDTO
     *
     * @param issueDetailDTO issueDetailDTO
     * @return IssueSubVO
     */
    public IssueSubVO issueDetailDoToIssueSubDto(IssueDetailDTO issueDetailDTO) {
        IssueSubVO issueSubVO = new IssueSubVO();
        BeanUtils.copyProperties(issueDetailDTO, issueSubVO);
        issueSubVO.setComponentIssueRelVOList(modelMapper.map(issueDetailDTO.getComponentIssueRelDTOList(), new TypeToken<List<ComponentIssueRelVO>>(){}.getType()));
        issueSubVO.setVersionIssueRelVOList(modelMapper.map(issueDetailDTO.getVersionIssueRelDTOList(), new TypeToken<List<VersionIssueRelVO>>(){}.getType()));
        issueSubVO.setActiveSprint(sprintNameAssembler.toTarget(issueDetailDTO.getActiveSprint(), SprintNameVO.class));
        issueSubVO.setCloseSprint(sprintNameAssembler.toTargetList(issueDetailDTO.getCloseSprint(), SprintNameVO.class));
        issueSubVO.setLabelIssueRelVOList(modelMapper.map(issueDetailDTO.getLabelIssueRelDTOList(), new TypeToken<List<LabelIssueRelVO>>(){}.getType()));
        issueSubVO.setIssueAttachmentVOList(modelMapper.map(issueDetailDTO.getIssueAttachmentDTOList(), new TypeToken<List<IssueAttachmentVO>>(){}.getType()));
        issueSubVO.setIssueCommentVOList(modelMapper.map(issueDetailDTO.getIssueCommentDTOList(), new TypeToken<List<IssueCommentVO>>(){}.getType()));
        List<Long> assigneeIdList = new ArrayList<>();
        assigneeIdList.add(issueDetailDTO.getAssigneeId());
        assigneeIdList.add(issueDetailDTO.getReporterId());
        assigneeIdList.add(issueDetailDTO.getCreatedBy());
        Boolean issueCommentCondition = issueSubVO.getIssueCommentVOList() != null && !issueSubVO.getIssueCommentVOList().isEmpty();
        if (issueCommentCondition) {
            assigneeIdList.addAll(issueSubVO.getIssueCommentVOList().stream().map(IssueCommentVO::getUserId).collect(Collectors.toList()));
        }
        Map<Long, UserMessageDTO> userMessageDOMap = userService.queryUsersMap(
                assigneeIdList.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList()), true);
        String assigneeName = userMessageDOMap.get(issueSubVO.getAssigneeId()) != null ? userMessageDOMap.get(issueSubVO.getAssigneeId()).getName() : null;
        String reporterName = userMessageDOMap.get(issueSubVO.getReporterId()) != null ? userMessageDOMap.get(issueSubVO.getReporterId()).getName() : null;
        String createrName = userMessageDOMap.get(issueSubVO.getCreatedBy()) != null ? userMessageDOMap.get(issueSubVO.getCreatedBy()).getName() : null;
        issueSubVO.setCreaterEmail(userMessageDOMap.get(issueSubVO.getCreatedBy()) != null ? userMessageDOMap.get(issueSubVO.getCreatedBy()).getEmail() : null);
        issueSubVO.setAssigneeName(assigneeName);
        issueSubVO.setAssigneeImageUrl(assigneeName != null ? userMessageDOMap.get(issueSubVO.getAssigneeId()).getImageUrl() : null);
        issueSubVO.setReporterName(reporterName);
        issueSubVO.setReporterImageUrl(reporterName != null ? userMessageDOMap.get(issueSubVO.getReporterId()).getImageUrl() : null);
        issueSubVO.setCreaterName(createrName);
        issueSubVO.setCreaterImageUrl(createrName != null ? userMessageDOMap.get(issueSubVO.getCreatedBy()).getImageUrl() : null);
        if (issueCommentCondition) {
            issueSubVO.getIssueCommentVOList().forEach(issueCommentDTO -> {
                UserMessageDTO commentUser = userMessageDOMap.get(issueCommentDTO.getUserId());
                issueCommentDTO.setUserName(commentUser != null ? commentUser.getName() : null);
                issueCommentDTO.setUserImageUrl(commentUser != null ? commentUser.getImageUrl() : null);
                issueCommentDTO.setUserRealName(commentUser != null ? commentUser.getRealName() : null);
                issueCommentDTO.setUserLoginName(commentUser != null ? commentUser.getLoginName() : null);
            });
        }
        return issueSubVO;
    }

    public List<ExportIssuesVO> exportIssuesDOListToExportIssuesDTO(List<IssueDTO> exportIssues, Long projectId) {
        List<ExportIssuesVO> exportIssuesVOS = new ArrayList<>(exportIssues.size());
        Set<Long> userIds = exportIssues.stream().filter(issue -> issue.getAssigneeId() != null && !Objects.equals(issue.getAssigneeId(), 0L)).map(IssueDTO::getAssigneeId).collect(Collectors.toSet());
        userIds.addAll(exportIssues.stream().filter(issue -> issue.getReporterId() != null && !Objects.equals(issue.getReporterId(), 0L)).map(IssueDTO::getReporterId).collect(Collectors.toSet()));
        Map<Long, UserMessageDTO> usersMap = userService.queryUsersMap(new ArrayList<>(userIds), true);
        Map<Long, IssueTypeVO> issueTypeDTOMap = ConvertUtil.getIssueTypeMap(projectId, SchemeApplyType.AGILE);
        Map<Long, StatusVO> statusMapDTOMap = ConvertUtil.getIssueStatusMap(projectId);
        Map<Long, PriorityVO> priorityDTOMap = ConvertUtil.getIssuePriorityMap(projectId);
        exportIssues.forEach(issue -> {
            String assigneeName = usersMap.get(issue.getAssigneeId()) != null ? usersMap.get(issue.getAssigneeId()).getName() : null;
            String assigneeRealName = usersMap.get(issue.getAssigneeId()) != null ? usersMap.get(issue.getAssigneeId()).getRealName() : null;
            String reporterName = usersMap.get(issue.getReporterId()) != null ? usersMap.get(issue.getReporterId()).getName() : null;
            String reporterRealName = usersMap.get(issue.getReporterId()) != null ? usersMap.get(issue.getReporterId()).getRealName() : null;
            ExportIssuesVO exportIssuesVO = new ExportIssuesVO();
            BeanUtils.copyProperties(issue, exportIssuesVO);
            exportIssuesVO.setSprintName(getActiveSprintName(issue));
            exportIssuesVO.setPriorityName(priorityDTOMap.get(issue.getPriorityId()) == null ? null : priorityDTOMap.get(issue.getPriorityId()).getName());
            exportIssuesVO.setStatusName(statusMapDTOMap.get(issue.getStatusId()) == null ? null : statusMapDTOMap.get(issue.getStatusId()).getName());
            exportIssuesVO.setTypeName(issueTypeDTOMap.get(issue.getIssueTypeId()) == null ? null : issueTypeDTOMap.get(issue.getIssueTypeId()).getName());
            exportIssuesVO.setAssigneeName(assigneeName);
            exportIssuesVO.setAssigneeRealName(assigneeRealName);
            exportIssuesVO.setReporterName(reporterName);
            exportIssuesVO.setReporterRealName(reporterRealName);
            exportIssuesVOS.add(exportIssuesVO);
        });
        return exportIssuesVOS;
    }

    protected String getActiveSprintName(IssueDTO issue) {
        List<IssueSprintDTO>  issueSprintList = issue.getIssueSprintDTOS();
        if (!ObjectUtils.isEmpty(issueSprintList)) {
            for(IssueSprintDTO sprint : issueSprintList) {
                if (!"closed".equals(sprint.getStatusCode())) {
                    return sprint.getSprintName();
                }
            }
        }
        return null;
    }

    public IssueCreateVO issueDtoToIssueCreateDto(IssueDetailDTO issueDetailDTO) {
        IssueCreateVO issueCreateVO = new IssueCreateVO();
        BeanUtils.copyProperties(issueDetailDTO, issueCreateVO);
        issueCreateVO.setSprintId(null);
        issueCreateVO.setRemainingTime(null);
        issueCreateVO.setComponentIssueRelVOList(copyComponentIssueRel(issueDetailDTO.getComponentIssueRelDTOList()));
        issueCreateVO.setVersionIssueRelVOList(copyVersionIssueRel(issueDetailDTO.getVersionIssueRelDTOList()));
        issueCreateVO.setLabelIssueRelVOList(copyLabelIssueRel(issueDetailDTO.getLabelIssueRelDTOList(), issueDetailDTO.getProjectId()));
        return issueCreateVO;
    }

    public IssueSubCreateVO issueDtoToIssueSubCreateDto(IssueDetailDTO issueDetailDTO) {
        IssueSubCreateVO issueSubCreateVO = new IssueSubCreateVO();
        BeanUtils.copyProperties(issueDetailDTO, issueSubCreateVO);
        issueSubCreateVO.setSprintId(null);
        issueSubCreateVO.setRemainingTime(null);
        issueSubCreateVO.setComponentIssueRelVOList(copyComponentIssueRel(issueDetailDTO.getComponentIssueRelDTOList()));
        issueSubCreateVO.setVersionIssueRelVOList(copyVersionIssueRel(issueDetailDTO.getVersionIssueRelDTOList()));
        issueSubCreateVO.setLabelIssueRelVOList(copyLabelIssueRel(issueDetailDTO.getLabelIssueRelDTOList(), issueDetailDTO.getProjectId()));
        return issueSubCreateVO;
    }

    private List<ComponentIssueRelVO> copyComponentIssueRel(List<ComponentIssueRelDTO> componentIssueRelDTOList) {
        List<ComponentIssueRelVO> componentIssueRelVOList = new ArrayList<>(componentIssueRelDTOList.size());
        componentIssueRelDTOList.forEach(componentIssueRelDO -> {
            ComponentIssueRelVO componentIssueRelVO = new ComponentIssueRelVO();
            BeanUtils.copyProperties(componentIssueRelDO, componentIssueRelVO);
            componentIssueRelVO.setIssueId(null);
            componentIssueRelVO.setObjectVersionNumber(null);
            componentIssueRelVOList.add(componentIssueRelVO);
        });
        return componentIssueRelVOList;
    }

    private List<LabelIssueRelVO> copyLabelIssueRel(List<LabelIssueRelDTO> labelIssueRelDTOList, Long projectId) {
        List<LabelIssueRelVO> labelIssueRelVOList = new ArrayList<>(labelIssueRelDTOList.size());
        labelIssueRelDTOList.forEach(labelIssueRelDO -> {
            LabelIssueRelVO labelIssueRelVO = new LabelIssueRelVO();
            BeanUtils.copyProperties(labelIssueRelDO, labelIssueRelVO);
            labelIssueRelVO.setIssueId(null);
            labelIssueRelVO.setLabelName(null);
            labelIssueRelVO.setObjectVersionNumber(null);
            labelIssueRelVO.setProjectId(projectId);
            labelIssueRelVOList.add(labelIssueRelVO);
        });
        return labelIssueRelVOList;
    }

    private List<VersionIssueRelVO> copyVersionIssueRel(List<VersionIssueRelDTO> versionIssueRelDTOList) {
        List<VersionIssueRelVO> versionIssueRelVOList = new ArrayList<>(versionIssueRelDTOList.size());
        versionIssueRelDTOList.forEach(versionIssueRelDO -> {
            VersionIssueRelVO versionIssueRelVO = new VersionIssueRelVO();
            BeanUtils.copyProperties(versionIssueRelDO, versionIssueRelVO);
            versionIssueRelVO.setIssueId(null);
            versionIssueRelVOList.add(versionIssueRelVO);
        });
        return versionIssueRelVOList;
    }

    public IssueSubCreateVO issueDtoToSubIssueCreateDto(IssueDetailDTO subIssueDetailDTO, Long parentIssueId) {
        IssueSubCreateVO issueCreateDTO = new IssueSubCreateVO();
        BeanUtils.copyProperties(subIssueDetailDTO, issueCreateDTO);
        String subSummary = "CLONE-" + subIssueDetailDTO.getSummary();
        issueCreateDTO.setSummary(subSummary);
        issueCreateDTO.setSprintId(null);
        issueCreateDTO.setIssueNum(null);
        issueCreateDTO.setParentIssueId(parentIssueId);
        issueCreateDTO.setComponentIssueRelVOList(copyComponentIssueRel(subIssueDetailDTO.getComponentIssueRelDTOList()));
        issueCreateDTO.setVersionIssueRelVOList(copyVersionIssueRel(subIssueDetailDTO.getVersionIssueRelDTOList()));
        issueCreateDTO.setLabelIssueRelVOList(copyLabelIssueRel(subIssueDetailDTO.getLabelIssueRelDTOList(), subIssueDetailDTO.getProjectId()));
        return issueCreateDTO;
    }

    public List<IssueComponentDetailDTO> issueComponentDetailDoToDto(Long projectId, List<IssueComponentDetailInfoDTO> issueComponentDetailInfoDTOS) {
        List<IssueComponentDetailDTO> issueComponentDetailDTOS = new ArrayList<>(issueComponentDetailInfoDTOS.size());
        if (!issueComponentDetailInfoDTOS.isEmpty()) {
            List<Long> userIds = issueComponentDetailInfoDTOS.stream().filter(issue -> issue.getAssigneeId() != null && !Objects.equals(issue.getAssigneeId(), 0L)).map(IssueComponentDetailInfoDTO::getAssigneeId).collect(Collectors.toList());
            userIds.addAll(issueComponentDetailInfoDTOS.stream().filter(issue -> issue.getReporterId() != null && !Objects.equals(issue.getReporterId(), 0L)).
                    map(IssueComponentDetailInfoDTO::getReporterId).collect(Collectors.toList()));
            Map<Long, UserMessageDTO> usersMap = userService.queryUsersMap(userIds.stream().distinct().collect(Collectors.toList()), true);
            Map<Long, IssueTypeVO> issueTypeDTOMap = ConvertUtil.getIssueTypeMap(projectId, SchemeApplyType.TEST);
            Map<Long, IssueTypeVO> issueTypeDTOMapAgile = ConvertUtil.getIssueTypeMap(projectId, SchemeApplyType.AGILE);
            issueTypeDTOMap.putAll(issueTypeDTOMapAgile);
            Map<Long, StatusVO> statusMapDTOMap = ConvertUtil.getIssueStatusMap(projectId);
            Map<Long, PriorityVO> priorityDTOMap = ConvertUtil.getIssuePriorityMap(projectId);
            issueComponentDetailInfoDTOS.parallelStream().forEachOrdered(issueDO -> {
                String assigneeName = usersMap.get(issueDO.getAssigneeId()) != null ? usersMap.get(issueDO.getAssigneeId()).getName() : null;
                String assigneeLoginName = usersMap.get(issueDO.getAssigneeId()) != null ? usersMap.get(issueDO.getAssigneeId()).getLoginName() : null;
                String assigneeRealName = usersMap.get(issueDO.getAssigneeId()) != null ? usersMap.get(issueDO.getAssigneeId()).getRealName() : null;
                String reporterName = usersMap.get(issueDO.getReporterId()) != null ? usersMap.get(issueDO.getReporterId()).getName() : null;
                String reporterLoginName = usersMap.get(issueDO.getReporterId()) != null ? usersMap.get(issueDO.getReporterId()).getLoginName() : null;
                String reporterRealName = usersMap.get(issueDO.getReporterId()) != null ? usersMap.get(issueDO.getReporterId()).getRealName() : null;
                String assigneeImageUrl = assigneeName != null ? usersMap.get(issueDO.getAssigneeId()).getImageUrl() : null;
                String reporterImageUrl = reporterName != null ? usersMap.get(issueDO.getReporterId()).getImageUrl() : null;
                IssueComponentDetailDTO issueComponentDetailDTO = new IssueComponentDetailDTO();
                BeanUtils.copyProperties(issueDO, issueComponentDetailDTO);
                issueComponentDetailDTO.setAssigneeName(assigneeName);
                issueComponentDetailDTO.setAssigneeLoginName(assigneeLoginName);
                issueComponentDetailDTO.setAssigneeRealName(assigneeRealName);
                issueComponentDetailDTO.setReporterName(reporterName);
                issueComponentDetailDTO.setReporterLoginName(reporterLoginName);
                issueComponentDetailDTO.setReporterRealName(reporterRealName);
                issueComponentDetailDTO.setAssigneeImageUrl(assigneeImageUrl);
                issueComponentDetailDTO.setReporterImageUrl(reporterImageUrl);
                issueComponentDetailDTO.setIssueTypeVO(issueTypeDTOMap.get(issueDO.getIssueTypeId()));
                issueComponentDetailDTO.setStatusVO(statusMapDTOMap.get(issueDO.getStatusId()));
                issueComponentDetailDTO.setPriorityVO(priorityDTOMap.get(issueDO.getPriorityId()));
                issueComponentDetailDTO.setComponentIssueRelVOList(modelMapper.map(issueDO.getComponentIssueRelDTOList(), new TypeToken<List<ComponentIssueRelVO>>(){}.getType()));
                issueComponentDetailDTO.setVersionIssueRelVOList(modelMapper.map(issueDO.getVersionIssueRelDTOList(),new TypeToken<List<VersionIssueRelVO>>(){}.getType()));
                issueComponentDetailDTO.setLabelIssueRelVOList(modelMapper.map(issueDO.getLabelIssueRelDTOList(), new TypeToken<List<LabelIssueRelVO>>(){}.getType()));
                issueComponentDetailDTOS.add(issueComponentDetailDTO);
            });
        }
        return issueComponentDetailDTOS;
    }

    public List<IssueListTestVO> issueDoToIssueTestListDto(List<IssueDTO> issueDTOList, Map<Long, PriorityVO> priorityMap, Map<Long, StatusVO> statusMapDTOMap, Map<Long, IssueTypeVO> issueTypeDTOMap) {
        List<IssueListTestVO> issueListTestVOS = new ArrayList<>(issueDTOList.size());
        Set<Long> userIds = issueDTOList.stream().filter(issue -> issue.getAssigneeId() != null && !Objects.equals(issue.getAssigneeId(), 0L)).map(IssueDTO::getAssigneeId).collect(Collectors.toSet());
        Map<Long, UserMessageDTO> usersMap = userService.queryUsersMap(Lists.newArrayList(userIds), true);
        issueDTOList.forEach(issueDO -> {
            String assigneeName = usersMap.get(issueDO.getAssigneeId()) != null ? usersMap.get(issueDO.getAssigneeId()).getName() : null;
            String assigneeImageUrl = assigneeName != null ? usersMap.get(issueDO.getAssigneeId()).getImageUrl() : null;
            IssueListTestVO issueListTestVO = toTarget(issueDO, IssueListTestVO.class);
            issueListTestVO.setAssigneeName(assigneeName);
            issueListTestVO.setPriorityVO(priorityMap.get(issueDO.getPriorityId()));
            issueListTestVO.setIssueTypeVO(issueTypeDTOMap.get(issueDO.getIssueTypeId()));
            issueListTestVO.setStatusVO(statusMapDTOMap.get(issueDO.getStatusId()));
            issueListTestVO.setAssigneeImageUrl(assigneeImageUrl);
            issueListTestVOS.add(issueListTestVO);
        });
        return issueListTestVOS;
    }

    public List<IssueNumVO> issueNumDoToDto(List<IssueNumDTO> issueNumDTOList, Long projectId) {
        List<IssueNumVO> issueNumVOS = new ArrayList<>(issueNumDTOList.size());
        if (!issueNumDTOList.isEmpty()) {
            Map<Long, IssueTypeVO> issueTypeDTOMap = ConvertUtil.getIssueTypeMap(projectId, SchemeApplyType.AGILE);
            issueNumDTOList.forEach(issueDO -> {
                IssueNumVO issueNumVO = new IssueNumVO();
                BeanUtils.copyProperties(issueDO, issueNumVO);
                issueNumVO.setIssueTypeVO(issueTypeDTOMap.get(issueDO.getIssueTypeId()));
                issueNumVOS.add(issueNumVO);
            });
        }
        return issueNumVOS;
    }

    public List<UnfinishedIssueVO> unfinishedIssueDoToDto(List<UnfinishedIssueDTO> unfinishedIssueDTOS, Long projectId) {
        List<UnfinishedIssueVO> unfinishedIssueVOS = new ArrayList<>(unfinishedIssueDTOS.size());
        if (!unfinishedIssueDTOS.isEmpty()) {
            Map<Long, IssueTypeVO> issueTypeDTOMap = ConvertUtil.getIssueTypeMap(projectId, SchemeApplyType.AGILE);
            Map<Long, StatusVO> statusMapDTOMap = ConvertUtil.getIssueStatusMap(projectId);
            Map<Long, PriorityVO> priorityDTOMap = ConvertUtil.getIssuePriorityMap(projectId);
            unfinishedIssueDTOS.forEach(unfinishedIssueDTO -> {
                UnfinishedIssueVO unfinishedIssueVO = toTarget(unfinishedIssueDTO, UnfinishedIssueVO.class);
                unfinishedIssueVO.setIssueTypeVO(issueTypeDTOMap.get(unfinishedIssueDTO.getIssueTypeId()));
                unfinishedIssueVO.setStatusVO(statusMapDTOMap.get(unfinishedIssueDTO.getStatusId()));
                unfinishedIssueVO.setPriorityVO(priorityDTOMap.get(unfinishedIssueDTO.getPriorityId()));
                unfinishedIssueVOS.add(unfinishedIssueVO);
            });

        }
        return unfinishedIssueVOS;
    }

    public List<UndistributedIssueVO> undistributedIssueDOToDto(List<UndistributedIssueDTO> undistributedIssueDTOS, Long projectId) {
        List<UndistributedIssueVO> undistributedIssueVOS = new ArrayList<>(undistributedIssueDTOS.size());
        if (!undistributedIssueDTOS.isEmpty()) {
            Map<Long, IssueTypeVO> issueTypeDTOMap = ConvertUtil.getIssueTypeMap(projectId, SchemeApplyType.AGILE);
            Map<Long, StatusVO> statusMapDTOMap = ConvertUtil.getIssueStatusMap(projectId);
            Map<Long, PriorityVO> priorityDTOMap = ConvertUtil.getIssuePriorityMap(projectId);
            undistributedIssueDTOS.forEach(undistributedIssueDTO -> {
                UndistributedIssueVO undistributedIssueVO = toTarget(undistributedIssueDTO, UndistributedIssueVO.class);
                undistributedIssueVO.setIssueTypeVO(issueTypeDTOMap.get(undistributedIssueDTO.getIssueTypeId()));
                undistributedIssueVO.setStatusVO(statusMapDTOMap.get(undistributedIssueDTO.getStatusId()));
                undistributedIssueVO.setPriorityVO(priorityDTOMap.get(undistributedIssueDTO.getPriorityId()));
                undistributedIssueVOS.add(undistributedIssueVO);
            });

        }
        return undistributedIssueVOS;
    }

    public  List<IssueLinkVO> issueDTOTOVO(Long projectId, List<IssueDTO> issueDTOs){
        List<io.choerodon.agile.api.vo.IssueLinkVO> issueLinkVOList = new ArrayList<>(issueDTOs.size());
        if (!issueDTOs.isEmpty()) {
            Map<Long, IssueTypeVO> testIssueTypeDTOMap = ConvertUtil.getIssueTypeMap(projectId, SchemeApplyType.TEST);
            Map<Long, IssueTypeVO> agileIssueTypeDTOMap = ConvertUtil.getIssueTypeMap(projectId, SchemeApplyType.AGILE);
            Map<Long, StatusVO> statusMapDTOMap = ConvertUtil.getIssueStatusMap(projectId);
            Map<Long, PriorityVO> priorityDTOMap = ConvertUtil.getIssuePriorityMap(projectId);
            List<Long> assigneeIds = issueDTOs.stream().filter(issue -> issue.getAssigneeId() != null && !Objects.equals(issue.getAssigneeId(), 0L)).map(IssueDTO::getAssigneeId).distinct().collect(Collectors.toList());
            Map<Long, UserMessageDTO> usersMap = userService.queryUsersMap(assigneeIds, true);
            issueDTOs.forEach(issueLinkDO -> {
                String assigneeName = usersMap.get(issueLinkDO.getAssigneeId()) != null ? usersMap.get(issueLinkDO.getAssigneeId()).getName() : null;
                String imageUrl = assigneeName != null ? usersMap.get(issueLinkDO.getAssigneeId()).getImageUrl() : null;
                io.choerodon.agile.api.vo.IssueLinkVO issueLinkVO = new io.choerodon.agile.api.vo.IssueLinkVO();
                BeanUtils.copyProperties(issueLinkDO, issueLinkVO);
                if (issueLinkDO.getApplyType().equals(SchemeApplyType.TEST)) {
                    issueLinkVO.setIssueTypeVO(testIssueTypeDTOMap.get(issueLinkDO.getIssueTypeId()));
                } else {
                    issueLinkVO.setIssueTypeVO(agileIssueTypeDTOMap.get(issueLinkDO.getIssueTypeId()));
                }
                issueLinkVO.setStatusVO(statusMapDTOMap.get(issueLinkDO.getStatusId()));
                issueLinkVO.setPriorityVO(priorityDTOMap.get(issueLinkDO.getPriorityId()));
                issueLinkVO.setAssigneeName(assigneeName);
                issueLinkVO.setImageUrl(imageUrl);
                issueLinkVOList.add(issueLinkVO);
            });
        }
        return  issueLinkVOList;
    }

    /**
     *  issueDTO转换SprintStatisticsVO
     * @param issueList issueList
     * @return SprintStatisticsVO
     */
    public SprintStatisticsVO issueDTOToSprintStatisticsVO(List<IssueOverviewVO> issueList) {
        SprintStatisticsVO sprintStatistics = new SprintStatisticsVO();
        Map<Boolean, List<IssueOverviewVO>> group = issueList.stream()
                .collect(Collectors.groupingBy(issue -> BooleanUtils.isTrue(issue.getCompleted())));
        sprintStatistics.setTotal(issueList.size());
        sprintStatistics.setCompletedCount(group.getOrDefault(Boolean.TRUE, Collections.emptyList()).size());
        sprintStatistics.setUncompletedCount(group.getOrDefault(Boolean.FALSE, Collections.emptyList()).size());
        sprintStatistics.setTodoCount(Long.valueOf(group.getOrDefault(Boolean.FALSE, Collections.emptyList()).stream()
                .filter(issue -> Objects.equals(StatusType.TODO, issue.getCategoryCode())).count()).intValue());
        sprintStatistics.setUnassignCount(Long.valueOf(group.getOrDefault(Boolean.FALSE, Collections.emptyList()).stream()
                .filter(issue -> Objects.isNull(issue.getAssigneeId())).count()).intValue());
        return sprintStatistics;
    }

    public List<Map.Entry<String, Integer>> convertBugEntry(List<ReportIssueConvertDTO> reportIssueConvertDTOList, DateFormat df, Function<ReportIssueConvertDTO, Boolean> func){
        Map<Date, List<ReportIssueConvertDTO>> group = reportIssueConvertDTOList.stream()
                .filter(func::apply).collect(Collectors.groupingBy(bug1 -> DateUtils.truncate(bug1.getDate(), Calendar.DAY_OF_MONTH)));
        return group.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> new ImmutablePair<>(df.format(entry.getKey()),
                        entry.getValue().stream()
                                .map(v -> v.getNewValue().subtract(v.getOldValue()).intValue()).reduce(Integer::sum).orElse(0)))
                .collect(Collectors.toList());
    }

    public List<OneJobVO> issueToOneJob(SprintDTO sprint, List<IssueOverviewVO> issueList, List<WorkLogDTO> workLogList, List<DataLogDTO> resolutionLogList, List<DataLogDTO> assigneeLogList){
        // 生成迭代经办人，报告人list
        Set<Long> userSet = issueList.stream().map(IssueOverviewVO::getAssigneeId).collect(Collectors.toSet());
        userSet.addAll(issueList.stream().map(IssueOverviewVO::getReporterId).collect(Collectors.toSet()));
        // 移除经办人为null的情况
        userSet.remove(null);
        userSet.remove(0L);
        DateFormat df = new SimpleDateFormat(BaseConstants.Pattern.DATE);
        // 生成基准时间-用户轴
        Map<Date, Set<Long>> timeUserLine = generateTimeUserLine(sprint, userSet);
        Map<Long, UserMessageDTO> userMessageDOMap = userService.queryUsersMap(new ArrayList<>(userSet), true);
        // issue类型map
        Map<Long, IssueOverviewVO> issueTypeMap = issueList.stream().collect(Collectors.toMap(IssueOverviewVO::getIssueId,
                a -> a));
        // 每日每人bug提出数量
        Map<Date, Map<Long, List<IssueOverviewVO>>> dateOneBugMap = groupByList(
                issueList.stream().filter(issue -> IssueTypeCode.isBug(issue.getTypeCode())).collect(Collectors.toList()),
                IssueOverviewVO::getCreationDate, IssueOverviewVO::getCreatedBy,
                issue -> issue.setCreationDate(DateUtils.truncate(issue.getCreationDate(),
                        Calendar.DAY_OF_MONTH)));
        // 每日每人工时
        Map<Date, Map<Long, List<WorkLogDTO>>> dateOneWorkMap = groupByList(workLogList,
                WorkLogDTO::getStartDate, WorkLogDTO::getCreatedBy,
                issue -> issue.setStartDate(DateUtils.truncate(issue.getStartDate(),
                                Calendar.DAY_OF_MONTH)));
        // 每日issue最后经办人
        Map<Date, Map<Long, DataLogDTO>> assigneeMap = getAssigneeMap(assigneeLogList);
        // 计算任务，故事，解决bug
        Map<Date, List<DataLogDTO>> creationMap = getcreationMap(resolutionLogList, assigneeMap, issueTypeMap);
        Map<Date, OneJobVO> oneJobMap = timeUserLine.entrySet().stream().map(entry -> new ImmutablePair<>(entry.getKey()
                , dataLogListToOneJob(entry.getKey(), entry.getValue(),
                creationMap, issueTypeMap, dateOneBugMap, dateOneWorkMap, userMessageDOMap)))
                .collect(Collectors.toMap(ImmutablePair::getLeft, ImmutablePair::getRight));
        return oneJobMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(entry -> {
            List<JobVO> jobList = entry.getValue().getJobList();
            entry.getValue().setTotal(new JobVO(jobList));
            entry.getValue().setWorkDate(df.format(entry.getValue().getWorkDateSource()));
            return entry.getValue();
        }).collect(Collectors.toList());
    }

    /**
     * 生成基准轴
     * @param sprint sprint
     * @param userSet userSet
     * @return 基准轴map
     */
    private Map<Date, Set<Long>> generateTimeUserLine(SprintDTO sprint, Set<Long> userSet) {
        Map<Date, Set<Long>> timeUserLine = new HashMap<>();
        // 生成迭代的开始时间与结束时间
        Date temp = DateUtils.truncate(sprint.getStartDate(), Calendar.DAY_OF_MONTH);
        Date now = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
        // 渲染基础时间轴，包含从迭代开始到目前的所有日期, 迭代所有涉及到的经办人和报告人
        while (!temp.equals(now)){
            timeUserLine.put(temp, new HashSet<>(userSet));
            temp = DateUtils.addDays(temp, 1);
        }
        timeUserLine.put(now, new HashSet<>(userSet));
        return timeUserLine;
    }

    private Map<Date, List<DataLogDTO>> getcreationMap(List<DataLogDTO> dataLogList,
                                                       Map<Date, Map<Long, DataLogDTO>> assigneeMap,
                                                       Map<Long, IssueOverviewVO> issueTypeMap) {
        return dataLogList.stream()
                    .peek(log -> log.setCreationDate(DateUtils.truncate(log.getCreationDate(), Calendar.DAY_OF_MONTH)))
                    // 按照日志的创建日期分组
                    .collect(Collectors.groupingBy(DataLogDTO::getCreationDate))
                    .entrySet().stream()
                    // 按照issueId去重，取logId最大值,即当天的最后解决记录
                    .map(entry -> new ImmutablePair<>(entry.getKey(), entry.getValue().stream()
                            .collect(Collectors.groupingBy(DataLogDTO::getIssueId)).values()
                            .stream().map(list -> list.stream()
                                    .max(Comparator.comparingLong(DataLogDTO::getLogId))
                                    .map(log -> {
                                        DataLogDTO assignee = assigneeMap.getOrDefault(entry.getKey(), Collections.emptyMap())
                                                .getOrDefault(log.getIssueId(), new DataLogDTO());
                                        if (Objects.isNull(assignee.getNewValue())){
                                            log.setCreatedBy(issueTypeMap.get(log.getIssueId()).getCreatedBy());
                                        }else {
                                            log.setCreatedBy(Long.parseLong(assignee.getNewValue()));
                                        }
                                        return log;
                                    })
                                    .orElse(null)).filter(Objects::nonNull).collect(Collectors.toList())))
                    .collect(Collectors.toMap(ImmutablePair::getLeft, ImmutablePair::getRight));
    }

    /**
     * 返回值 Map<日期, Map<issueId, assigneeId>>
     * @param assigneeLogList 经办人日志
     * @return Map
     */
    private Map<Date, Map<Long, DataLogDTO>> getAssigneeMap(List<DataLogDTO> assigneeLogList) {
        return assigneeLogList.stream()
                .peek(log -> log.setCreationDate(DateUtils.truncate(log.getCreationDate(), Calendar.DAY_OF_MONTH)))
                // 按照日志的创建日期分组
                .collect(Collectors.groupingBy(DataLogDTO::getCreationDate))
                .entrySet().stream()
                // 按照issueId去重，取logId最大值,即当天的经办人记录，
                // 如果无经办人则说明issue从创建就没更换过经办人，此时复制issueId, 直接取issue上的人。
                .map(entry -> new ImmutablePair<>(entry.getKey(), entry.getValue().stream()
                        .collect(Collectors.groupingBy(DataLogDTO::getIssueId)).entrySet()
                        .stream().map(e1 -> new ImmutablePair<>(e1.getKey(), e1.getValue().stream()
                                .max(Comparator.comparingLong(DataLogDTO::getLogId))
                                .orElseGet(() -> {
                                    DataLogDTO log = new DataLogDTO();
                                    log.setIssueId(e1.getKey());
                                    return log;
                                })
                                )).collect(Collectors.toMap(ImmutablePair::getLeft, ImmutablePair::getRight))))
                .collect(Collectors.toMap(ImmutablePair::getLeft, ImmutablePair::getRight));
    }

    private <T, K1, K2> Map<K1, Map<K2, List<T>>> groupByList(List<T> list,
                                                              Function<T, K1> k1,
                                                              Function<T, K2> k2,
                                                              Consumer<? super T> action){
        return list.stream().peek(action)
                .collect(Collectors.groupingBy(k1)).entrySet().stream()
                .map(entry -> new ImmutablePair<>(entry.getKey(), entry.getValue().stream()
                        .collect(Collectors.groupingBy(k2))))
                .collect(Collectors.toMap(ImmutablePair::getLeft, ImmutablePair::getRight));
    }

    private OneJobVO dataLogListToOneJob(Date workDate, Set<Long> userSet, Map<Date, List<DataLogDTO>> creationMap,
                                         Map<Long, IssueOverviewVO> issueTypeMap,
                                         Map<Date, Map<Long, List<IssueOverviewVO>>> dateOneBugMap,
                                         Map<Date, Map<Long, List<WorkLogDTO>>> dateOneWorkMap,
                                         Map<Long, UserMessageDTO> userMessageDOMap){
        // 根据日期取日志集合然后按照创建人分组
        Map<Long, List<DataLogDTO>> creationGroup = creationMap.getOrDefault(workDate, Collections.emptyList())
                .stream().collect(Collectors.groupingBy(DataLogDTO::getCreatedBy));
        List<JobVO> jobList = new ArrayList<>(creationGroup.size());
        for (Long userId : userSet) {
            // 取当前用户对应的解决issue集合，将集合按照issueType分组
            Map<String, List<DataLogDTO>> typeMap =
                    creationGroup.getOrDefault(userId, Collections.emptyList())
                            .stream().collect(Collectors.groupingBy(log -> issueTypeMap.get(log.getIssueId()).getTypeCode()));
            JobVO job = new JobVO();
            job.setWorker(userMessageDOMap.get(userId).getRealName());
            job.setTaskCount(typeMap.getOrDefault(IssueTypeCode.TASK.value(), Collections.emptyList()).size()
                    + typeMap.getOrDefault(IssueTypeCode.SUB_TASK.value(), Collections.emptyList()).size());
            job.setStoryCount(typeMap.getOrDefault(IssueTypeCode.STORY.value(), Collections.emptyList()).size());
            job.setStoryPointCount(typeMap.getOrDefault(IssueTypeCode.STORY.value(), Collections.emptyList())
                    .stream().map(points -> issueTypeMap.get(points.getIssueId()).getStoryPoints())
                    .filter(Objects::nonNull).reduce(BigDecimal::add).orElse(BigDecimal.ZERO));
            job.setBugFixCount(typeMap.getOrDefault(IssueTypeCode.BUG.value(), Collections.emptyList()).size());
            job.setBugCreatedCount(dateOneBugMap.getOrDefault(workDate, Collections.emptyMap())
                    .getOrDefault(userId, Collections.emptyList()).size());
            job.setWorkTime(dateOneWorkMap.getOrDefault(workDate, Collections.emptyMap())
                    .getOrDefault(userId, Collections.emptyList())
                    .stream().map(WorkLogDTO::getWorkTime).filter(Objects::nonNull)
                    .reduce(BigDecimal::add).orElse(BigDecimal.ZERO));
            jobList.add(job);
        }
        OneJobVO oneJob = new OneJobVO();
        oneJob.setJobList(jobList);
        oneJob.setWorkDateSource(workDate);
        return oneJob;
    }
}
