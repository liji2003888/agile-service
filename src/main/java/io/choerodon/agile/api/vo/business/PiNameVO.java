package io.choerodon.agile.api.vo.business;

import io.swagger.annotations.ApiModelProperty;
import org.hzero.starter.keyencrypt.core.Encrypt;

import java.util.List;

/**
 * Created by HuangFuqiang@choerodon.io on 2019/4/2.
 * Email: fuqianghuang01@gmail.com
 */
public class PiNameVO {

    @ApiModelProperty(value = "pi主键id")
    @Encrypt
    private Long id;

    @ApiModelProperty(value = "pi编码")
    private String code;

    @ApiModelProperty(value = "pi名称")
    private String name;

    @ApiModelProperty(value = "pi状态")
    private String statusCode;

    private List<SprintDetailVO> sprints;

    private String fullName;

    public List<SprintDetailVO> getSprints() {
        return sprints;
    }

    public void setSprints(List<SprintDetailVO> sprints) {
        this.sprints = sprints;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
