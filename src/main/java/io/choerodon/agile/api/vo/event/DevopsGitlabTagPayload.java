package io.choerodon.agile.api.vo.event;

/**
 * @author superlee
 * @since 2021-04-19
 */
public class DevopsGitlabTagPayload {

    private String tag;

    private Long projectId;

    private String serviceCode;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }
}
