package io.choerodon.agile.app.service;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSONObject;

import io.choerodon.agile.api.vo.*;
import io.choerodon.agile.infra.dto.*;
import io.choerodon.agile.infra.mapper.IssueMapper;
import io.choerodon.core.domain.Page;
import io.choerodon.core.domain.PageInfo;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.mybatis.pagehelper.domain.Sort;


/**
 * 敏捷开发Issue
 *
 * @author dinghuang123@gmail.com
 * @since 2018-05-14 20:30:48
 */
public interface IssueService {

    void setIssueMapper(IssueMapper issueMapper);

    IssueVO queryIssueCreate(Long projectId, Long issueId);

    void handleInitIssue(IssueConvertDTO issueConvertDTO, Long statusId, ProjectInfoDTO projectInfoDTO);

    void afterCreateIssue(Long issueId, IssueConvertDTO issueConvertDTO, IssueCreateVO issueCreateVO, ProjectInfoDTO projectInfoDTO);

    void afterCreateSubIssue(Long issueId, IssueConvertDTO subIssueConvertDTO, IssueSubCreateVO issueSubCreateVO, ProjectInfoDTO projectInfoDTO);

    /**
     * 查询单个issue
     *
     * @param projectId projectId
     * @param issueId   issueId
     * @return IssueVO
     */
    IssueVO queryIssue(Long projectId, Long issueId, Long organizationId);

    /**
     * 分页过滤查询issueList（包含子任务）
     *
     * @param projectId   projectId
     * @param searchVO   searchVO
     * @param pageRequest pageRequest
     * @return IssueListVO
     */
    Page<IssueListFieldKVVO> listIssueWithSub(Long projectId, SearchVO searchVO, PageRequest pageRequest, Long organizationId);

    List<EpicDataVO> listEpic(Long projectId);

    /**
     * 更新issue
     *
     * @param projectId      projectId
     * @param issueUpdateVO issueUpdateVO
     * @param fieldList      fieldList
     * @return IssueVO
     */
    IssueVO updateIssue(Long projectId, IssueUpdateVO issueUpdateVO, List<String> fieldList);

    /**
     * 更新issue的状态
     *
     * @param projectId
     * @param issueId
     * @param transformId
     * @return
     */
    IssueVO updateIssueStatus(Long projectId, Long issueId, Long transformId, Long objectVersionNumber, String applyType);

    /**
     * 更新issue自己的字段
     *
     * @param issueUpdateVO
     * @param fieldList
     * @param projectId
     */
    void handleUpdateIssue(IssueUpdateVO issueUpdateVO, List<String> fieldList, Long projectId);

    /**
     * 删除issue
     *
     * @param projectId projectId
     * @param issueId   issueId
     * @return int
     */
    void deleteIssue(Long projectId, Long issueId);

    void batchDeleteIssues(Long projectId, List<Long> issueIds);

    void batchDeleteIssuesAgile(Long projectId, List<Long> issueIds);

    void handleInitSubIssue(IssueConvertDTO subIssueConvertDTO, Long statusId, ProjectInfoDTO projectInfoDTO);

    IssueSubVO queryIssueSubByCreate(Long projectId, Long issueId);

    List<IssueSearchVO> batchIssueToVersion(Long projectId, Long versionId, List<Long> issueIds);

    void batchIssueToVersionTest(Long projectId, Long versionId, List<Long> issueIds);

    List<IssueSearchVO> batchIssueToEpic(Long projectId, Long epicId, List<Long> issueIds);

    List<IssueSearchVO> batchIssueToSprint(Long projectId, Long sprintId, MoveIssueVO moveIssueVO);

    /**
     * 根据项目id查询epic
     *
     * @param projectId projectId
     * @return IssueEpicVO
     */
    List<IssueEpicVO> listEpicSelectData(Long projectId);

    /**
     * 查询单个子任务信息
     *
     * @param projectId projectId
     * @param issueId   issueId
     * @return IssueSubVO
     */
    IssueSubVO queryIssueSub(Long projectId, Long organizationId, Long issueId);

    /**
     * 更改issue类型
     *
     * @param issueConvertDTO             issueConvertDTO
     * @param issueUpdateTypeVO issueUpdateTypeVO
     * @return IssueVO
     */
    IssueVO updateIssueTypeCode(IssueConvertDTO issueConvertDTO, IssueUpdateTypeVO issueUpdateTypeVO, Long organizationId);

    /**
     * 通过项目id和issueId查询issueE
     *
     * @param projectId projectId
     * @param issueId   issueId
     * @return IssueConvertDTO
     */
    IssueConvertDTO queryIssueByProjectIdAndIssueId(Long projectId, Long issueId);

    Page<IssueNumVO> queryIssueByOption(Long projectId, Long issueId, String issueNum, Boolean onlyActiveSprint, Boolean self, String content, PageRequest pageRequest);

    void exportIssues(Long projectId, SearchVO searchVO, HttpServletRequest request,
                      HttpServletResponse response, Long organizationId, Sort sort);

    /**
     * 根据issueId复制一个issue
     *
     * @param projectId        projectId
     * @param issueId          issueId
     * @param copyConditionVO copyConditionVO
     * @return IssueVO
     */
    IssueVO cloneIssueByIssueId(Long projectId, Long issueId, CopyConditionVO copyConditionVO, Long organizationId, String applyType);

    /**
     * 根据issueId转换为子任务
     *
     * @param projectId             projectId
     * @param issueTransformSubTask issueTransformSubTask
     * @return IssueSubVO
     */
    IssueSubVO transformedSubTask(Long projectId, Long organizationId, IssueTransformSubTask issueTransformSubTask);

    /**
     * 子任务转换为任务
     *
     * @param issueConvertDTO
     * @param issueTransformTask
     * @param organizationId
     * @return
     */
    IssueVO transformedTask(IssueConvertDTO issueConvertDTO, IssueTransformTask issueTransformTask, Long organizationId);

    List<IssueInfoVO> listByIssueIds(Long projectId, List<Long> issueIds);

    /**
     * 参数查询issueList提供给测试模块
     *
     * @param projectId   projectId
     * @param searchVO   searchVO
     * @param pageRequest pageRequest
     * @return IssueListVO
     */
    Page<IssueListTestVO> listIssueWithoutSubToTestComponent(Long projectId, SearchVO searchVO, PageRequest pageRequest, Long organizationId);

    Page<IssueListTestWithSprintVersionVO> listIssueWithLinkedIssues(Long projectId, SearchVO searchVO, PageRequest pageRequest, Long organizationId);

    List<IssueCreationNumVO> queryIssueNumByTimeSlot(Long projectId, String typeCode, Integer timeSlot);

    /**
     * 参数查询issue列表，不对外开放
     *
     * @param projectId   projectId
     * @param issueId     issueId
     * @param issueNum    issueNum
     * @param self        self
     * @param content     content
     * @param pageRequest pageRequest
     * @return IssueNumVO
     */
    Page<IssueNumVO> queryIssueByOptionForAgile(Long projectId, Long issueId, String issueNum,
                                                    Boolean self, String content, PageRequest pageRequest);

    /**
     * 拖动epic
     *
     * @param projectId       projectId
     * @param epicSequenceVO epicSequenceVO
     * @return EpicDataVO
     */
    EpicDataVO dragEpic(Long projectId, EpicSequenceVO epicSequenceVO);

//    /**
//     * 查询issue统计信息
//     *
//     * @param projectId  projectId
//     * @param type       type
//     * @param issueTypes issueTypes要排除的issue类型
//     * @return PieChartVO
//     */
//    List<PieChartVO> issueStatistic(Long projectId, String type, List<String> issueTypes);

//    /**
//     * 测试模块查询issue详情列表
//     *
//     * @param projectId   projectId
//     * @param searchVO   searchVO
//     * @param pageRequest pageRequest
//     * @return IssueComponentDetailTO
//     */
//    Page<IssueComponentDetailDTO> listIssueWithoutSubDetail(Long projectId, SearchVO searchVO, PageRequest pageRequest);

    IssueVO issueParentIdUpdate(Long projectId, IssueUpdateParentIdVO issueUpdateParentIdVO);

    JSONObject countUnResolveByProjectId(Long projectId);

    List<Long> queryIssueIdsByOptions(Long projectId, SearchVO searchVO);

    Page<UndistributedIssueVO> queryUnDistributedIssues(Long projectId, PageRequest pageable);

    List<UnfinishedIssueVO> queryUnfinishedIssues(Long projectId, Long assigneeId);

    /**
     * 查询用户故事地图泳道
     *
     * @param projectId projectId
     * @return String
     */
    String querySwimLaneCode(Long projectId);

//    /**
//     * 克隆issue同时生成版本
//     *
//     * @param projectId projectId
//     * @param versionId versionId
//     * @param issueIds  issueIds
//     * @return new issueIds
//     */
//    List<Long> cloneIssuesByVersionId(Long projectId, Long versionId, List<Long> issueIds);

//    /**
//     * 根据项目分组测试类型issue
//     *
//     * @return IssueProjectVO
//     */
//    List<IssueProjectVO> queryIssueTestGroupByProject();

    /**
     * 批量把issue根据冲刺判断更新为初始状态
     *
     * @param projectId    projectId
     * @param moveIssueIds moveIssueIds
     * @param sprintId     sprintId
     */
    void batchHandleIssueStatus(Long projectId, List<Long> moveIssueIds, Long sprintId);

    /**
     * 处理高级搜索中的用户搜索
     *
     * @param searchVO searchVO
     * @param projectId projectId
     */
    Boolean handleSearchUser(SearchVO searchVO, Long projectId);

    Boolean checkEpicName(Long projectId, String epicName, Long epicId);

//    IssueNumDTO queryIssueByIssueNum(Long projectId, String issueNum);

    /**
     * 根据projectId按批次迁移数据
     */
    List<TestCaseDTO> migrateTestCaseByProjectId(Long projectId);

    /**
     * 查询issue中的projecid
     */
    List<Long> queryProjectIds();

    List<IssueLinkVO> queryIssueByIssueIds(Long projectId, List<Long> issueIds);

    Page<IssueListFieldKVVO> queryStoryAndTask(Long projectId, PageRequest pageRequest, SearchVO searchVO);

    /**
     * 分页查询项目下的项目成员和分配过问题的用户
     *
     * @param pageRequest
     * @param projectId
     * @param param
     * @return
     */
    Page<UserDTO> pagingQueryUsers(PageRequest pageRequest, Long projectId, String param);

    /**
     * 分页查询项目下的项目成员和分配过问题的报告人
     *
     * @param pageRequest
     * @param projectId
     * @param param
     * @return
     */
    Page<UserDTO> pagingQueryReporters(PageRequest pageRequest, Long projectId, String param);

    /**
     * 删除自己的创建的Issue
     * @param projectId
     * @param issueId
     */
    void deleteSelfIssue(Long projectId, Long issueId);

    /**
     * 个人工作台查询代办事项
     * @param organizationId
     * @param projectId
     * @return
     */
    Page<IssueListFieldKVVO> queryBackLogIssuesByPersonal(Long organizationId, Long projectId,PageRequest pageRequest);
}