package io.choerodon.agile.infra.dto;


import org.hzero.starter.keyencrypt.core.Encrypt;

import java.util.List;

/**
 * Created by HuangFuqiang@choerodon.io on 2019/6/6.
 * Email: fuqianghuang01@gmail.com
 */
public class StoryMapStoryDTO {

    @Encrypt
    private Long issueId;

    private String issueNum;

    private String summary;

    @Encrypt(ignoreValue = {"0"})
    private Long epicId;

    private Boolean completed;

    @Encrypt
    private Long issueTypeId;

    @Encrypt
    private Long statusId;

    private List<StoryMapVersionDTO> storyMapVersionDTOList;

    public Long getIssueId() {
        return issueId;
    }

    public void setIssueId(Long issueId) {
        this.issueId = issueId;
    }

    public String getIssueNum() {
        return issueNum;
    }

    public void setIssueNum(String issueNum) {
        this.issueNum = issueNum;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Long getEpicId() {
        return epicId;
    }

    public void setEpicId(Long epicId) {
        this.epicId = epicId;
    }

    public void setStoryMapVersionDTOList(List<StoryMapVersionDTO> storyMapVersionDTOList) {
        this.storyMapVersionDTOList = storyMapVersionDTOList;
    }

    public List<StoryMapVersionDTO> getStoryMapVersionDTOList() {
        return storyMapVersionDTOList;
    }

    public Long getIssueTypeId() {
        return issueTypeId;
    }

    public void setIssueTypeId(Long issueTypeId) {
        this.issueTypeId = issueTypeId;
    }

    public Long getStatusId() {
        return statusId;
    }

    public void setStatusId(Long statusId) {
        this.statusId = statusId;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public Boolean getCompleted() {
        return completed;
    }
}
