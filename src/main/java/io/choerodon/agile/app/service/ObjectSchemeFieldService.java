package io.choerodon.agile.app.service;

import io.choerodon.agile.api.vo.*;
import io.choerodon.agile.infra.dto.ObjectSchemeFieldDTO;

import java.util.List;
import java.util.Map;

/**
 * @author shinan.chen
 * @since 2019/3/29
 */
public interface ObjectSchemeFieldService {

    /**
     *
     * @param field
     * @param contexts
     * @param issueTypeForRank 需要设置rank值的issueType，空值时不设置
     * @return
     */
    ObjectSchemeFieldDTO baseCreate(ObjectSchemeFieldDTO field, String[] contexts, String issueTypeForRank);

    void baseUpdate(ObjectSchemeFieldDTO field);

    ObjectSchemeFieldDTO baseQueryById(Long organizationId, Long projectId, Long fieldId);

    List<ObjectSchemeFieldDTO> listQuery(Long organizationId, Long projectId, ObjectSchemeFieldSearchVO searchDTO);

    ObjectSchemeFieldDTO queryByFieldCode(Long organizationId, Long projectId, String fieldCode);

    /**
     * 组织层/项目层 获取字段列表
     *
     * @param organizationId
     * @param schemeCode
     * @return
     */
    Map<String, Object> listQuery(Long organizationId, Long projectId, String schemeCode);

    /**
     * 组织层/项目层 创建字段
     *
     * @param organizationId
     * @param projectId
     * @param fieldCreateDTO
     * @param issueTypeForRank 需要设置rank值的issueType，空值时不设置
     * @return
     */
    ObjectSchemeFieldDetailVO create(Long organizationId, Long projectId, ObjectSchemeFieldCreateVO fieldCreateDTO, String issueTypeForRank);

    /**
     * 组织层/项目层 查询字段详情
     *
     * @param organizationId
     * @param projectId
     * @param fieldId
     * @return
     */
    ObjectSchemeFieldDetailVO queryById(Long organizationId, Long projectId, Long fieldId);

    /**
     * 组织层/项目层 删除字段
     *
     * @param organizationId
     * @param projectId
     * @param fieldId
     */
    void delete(Long organizationId, Long projectId, Long fieldId);

    /**
     * 组织层/项目层 更新字段
     *
     * @param organizationId
     * @param projectId
     * @param fieldId
     * @param updateDTO
     * @return
     */
    ObjectSchemeFieldDetailVO update(Long organizationId, Long projectId, Long fieldId, ObjectSchemeFieldUpdateVO updateDTO);

    /**
     * 组织层/项目层 字段名称是否重复
     *
     * @param organizationId
     * @param projectId
     * @param name
     * @param schemeCode
     * @return
     */
    Boolean checkName(Long organizationId, Long projectId, String name, String schemeCode);

    /**
     * 组织层/项目层 字段编码是否重复
     *
     * @param organizationId
     * @param projectId
     * @param code
     * @param schemeCode
     * @return
     */
    Boolean checkCode(Long organizationId, Long projectId, String code, String schemeCode);

    List<AgileIssueHeadVO> getIssueHeadForAgile(Long organizationId, Long projectId, String schemeCode);

    List<ObjectSchemeFieldDetailVO> queryCustomFieldList(Long projectId);

    ObjectSchemeFieldDTO selectById(Long fieldId);

    /**
     * 页面配置接口
     *
     * @param organizationId
     * @param projectId
     * @param pageConfigUpdateVO
     */
    void config(Long organizationId, Long projectId, PageConfigUpdateVO pageConfigUpdateVO);

    /**
     * 查询
     *
     * @param organizationId
     * @param projectId
     * @param issueType
     * @return
     */
    PageConfigVO listConfigs(Long organizationId, Long projectId, String issueType);

    /**
     * 查询字段及字段配置
     *
     * @param projectId
     * @param issueTypeId
     * @param organizationId
     * @return
     */
    List<ObjectSchemeFieldDetailVO> listFieldsWithOptionals(Long projectId, Long issueTypeId, Long organizationId);

    /**
     * 更新字段是否必输
     *
     * @param organizationId
     * @param projectId
     * @param fieldId
     * @param required
     */
    void updateRequired(Long organizationId, Long projectId, Long fieldId, Boolean required);

    /**
     * 查询rank值
     *
     * @param organizationId
     * @param projectId
     * @param adjustOrderVO
     * @return
     */
    String queryRank(Long organizationId, Long projectId, AdjustOrderVO adjustOrderVO);

    List<ObjectSchemeFieldVO> selectMemberList(Long organizationId, Long projectId, String schemeCode, Long issueTypeId, List<String> fieldCodeList);

    /**
     * 查询issueType下未配置的字段
     *
     * @param organizationId
     * @param projectId
     * @param issueType
     * @return
     */
    List<ObjectSchemeFieldVO> unselected(Long organizationId, Long projectId, String issueType);
}
