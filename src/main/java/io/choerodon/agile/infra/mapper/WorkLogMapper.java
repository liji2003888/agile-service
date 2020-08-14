package io.choerodon.agile.infra.mapper;

import io.choerodon.agile.infra.dto.WorkLogDTO;
import io.choerodon.mybatis.common.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * Created by HuangFuqiang@choerodon.io on 2018/5/18.
 * Email: fuqianghuang01@gmail.com
 */
public interface WorkLogMapper extends BaseMapper<WorkLogDTO> {

    /**
     * 根据issueId 倒序查找WorkLog
     *
     * @param issueId   issueId
     * @param projectId projectId
     * @return WorkLogDTO
     */
    List<WorkLogDTO> queryByIssueId(@Param("issueId") Long issueId, @Param("projectId") Long projectId);

    List<WorkLogDTO> selectWorkTimeBySpring(@Param("projectId") Long projectId,
                                            @Param("sprintId") Long springId,
                                            @Param("startDate") Date startDate,
                                            @Param("endDate") Date endDate);
}
