package io.choerodon.agile.infra.dto;

import io.choerodon.mybatis.annotation.ModifyAudit;
import io.choerodon.mybatis.annotation.VersionAudit;
import io.choerodon.mybatis.domain.AuditDomain;
import org.hzero.starter.keyencrypt.core.Encrypt;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author zhaotianxin
 * @date 2021-05-07 13:47
 */
@Table(name = "fd_list_layout")
@ModifyAudit
@VersionAudit
public class ListLayoutDTO extends AuditDomain {
    @Id
    @GeneratedValue
    @Encrypt
    private Long id;

    private String typeCode;

    @Encrypt
    private Long userId;

    private Long projectId;

    private Long organizationId;

    public ListLayoutDTO() {
    }

    public ListLayoutDTO(String typeCode, Long userId, Long projectId, Long organizationId) {
        this.typeCode = typeCode;
        this.userId = userId;
        this.projectId = projectId;
        this.organizationId = organizationId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }
}
