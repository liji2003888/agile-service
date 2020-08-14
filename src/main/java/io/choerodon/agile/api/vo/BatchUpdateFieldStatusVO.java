package io.choerodon.agile.api.vo;


/**
 * @author zhaotianxin
 * @date 2020-05-08 10:44
 */
public class BatchUpdateFieldStatusVO {

    private String key;

    private Long userId;

    private String status;

    private Double process;

    private String error;

    private Double incrementalValue;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getProcess() {
        return process;
    }

    public void setProcess(Double process) {
        this.process = process;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Double getIncrementalValue() {
        return incrementalValue;
    }

    public void setIncrementalValue(Double incrementalValue) {
        this.incrementalValue = incrementalValue;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
