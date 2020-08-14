package io.choerodon.agile.infra.mapper;

import io.choerodon.agile.infra.dto.RankDTO;
import io.choerodon.mybatis.common.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Created by HuangFuqiang@choerodon.io on 2019/6/24.
 * Email: fuqianghuang01@gmail.com
 */
public interface RankMapper extends BaseMapper<RankDTO> {

    RankDTO selectRankByIssueId(@Param("projectId") Long projectId, @Param("type") String type, @Param("issueId") Long issueId);

    List<Long> selectEpicIdsByProject(@Param("projectId") Long projectId);

    List<Long> checkRankEmpty(@Param("projectId") Long projectId, @Param("type") String type);

    void batchInsertRank(@Param("projectId") Long projectId, @Param("type") String type, @Param("insertRankList") List<RankDTO> insertRankList);

    String selectLeftRank(@Param("projectId") Long projectId, @Param("type") String type, @Param("rank") String rank);

    String selectRightRank(@Param("projectId") Long projectId, @Param("type") String type, @Param("rank") String rank);

    String selectMinRank(@Param("projectId") Long projectId, @Param("type") String type);

    void deleteRankByIssueId(@Param("issueId") Long issueId);
}
