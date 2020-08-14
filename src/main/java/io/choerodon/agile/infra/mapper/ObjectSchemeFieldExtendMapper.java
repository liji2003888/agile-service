package io.choerodon.agile.infra.mapper;

import io.choerodon.agile.api.vo.ObjectSchemeFieldVO;
import io.choerodon.agile.api.vo.PageConfigFieldVO;
import io.choerodon.agile.infra.dto.ObjectSchemeFieldExtendDTO;
import io.choerodon.mybatis.common.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;


/**
 * @author superlee
 * @since 2020-08-10
 */
public interface ObjectSchemeFieldExtendMapper extends BaseMapper<ObjectSchemeFieldExtendDTO> {

    /**
     * 查询最小的排序值
     *
     * @param organizationId
     * @param projectId
     * @param issueType
     * @return
     */
    String selectMinRank(@Param("organizationId") Long organizationId,
                         @Param("projectId") Long projectId,
                         @Param("issueType") String issueType);

    /**
     * 查询组织层的扩展字段
     *
     * @param issueType
     * @param organizationId
     * @param fieldId
     * @return
     */
    List<ObjectSchemeFieldExtendDTO> selectExtendField(@Param("issueType") String issueType,
                                                       @Param("organizationId") Long organizationId,
                                                       @Param("fieldId") Long fieldId,
                                                       @Param("projectId") Long projectId);

    /**
     * 查询页面配置数据
     *
     * @param organizationId
     * @param projectId
     * @param issueType
     * @return
     */
    List<PageConfigFieldVO> listConfigs(@Param("organizationId") Long organizationId,
                                        @Param("projectId") Long projectId,
                                        @Param("issueType") String issueType);

    /**
     * 批量更新组织下的required字段
     *
     * @param issueType
     * @param organizationId
     * @param fieldId
     * @param required
     */
    void batchUpdateRequired(@Param("issueType") String issueType,
                             @Param("organizationId") Long organizationId,
                             @Param("fieldId") Long fieldId,
                             @Param("required") Boolean required);

    /**
     * 查询配置过的项目集合
     *
     * @param organizationId
     * @return
     */
    Set<Long> selectProjectIdsByOrganizationId(@Param("organizationId") Long organizationId);

    /**
     * 查询targetRank之前最大的rank值
     * @param organizationId
     * @param projectId
     * @param issueType
     * @param targetRank
     * @return
     */
    String selectPreviousRank(@Param("organizationId") Long organizationId,
                              @Param("projectId") Long projectId,
                              @Param("issueType") String issueType,
                              @Param("targetRank") String targetRank);

    /**
     *
     * @param organizationId
     * @param projectId
     * @param issueType
     * @return
     */
    List<ObjectSchemeFieldVO> unselected(@Param("organizationId") Long organizationId,
                                         @Param("projectId") Long projectId,
                                         @Param("issueType") String issueType);

    /**
     * 批量插入接口
     * @param objectSchemeFieldExtends
     */
    void batchInsert(@Param("insertList") List<ObjectSchemeFieldExtendDTO> objectSchemeFieldExtends);
}
