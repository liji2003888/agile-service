package io.choerodon.agile.api.vo.report;

import org.hzero.starter.keyencrypt.core.Encrypt;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author chihao.ran@hand-china.com
 * 2021/06/11 11:30
 */
public class CustomChartPointVO implements Serializable {
    private static final long serialVersionUID = -2096291259245233816L;
    private String analysisValue;
    @Encrypt
    private Long analysisId;
    private String value;
    private String comparedValue;
    @Encrypt
    private Long comparedId;
    private BigDecimal percentage;

    public CustomChartPointVO() {
    }

    public CustomChartPointVO(String analysisValue, Long analysisId, String comparedValue, Long comparedId) {
        this.analysisValue = analysisValue;
        this.analysisId = analysisId;
        this.comparedId = comparedId;
        this.comparedValue = comparedValue;
        this.value = "0";
        this.percentage = BigDecimal.ZERO;
    }

    public String getAnalysisValue() {
        return analysisValue;
    }

    public void setAnalysisValue(String analysisValue) {
        this.analysisValue = analysisValue;
    }

    public Long getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(Long analysisId) {
        this.analysisId = analysisId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getComparedValue() {
        return comparedValue;
    }

    public void setComparedValue(String comparedValue) {
        this.comparedValue = comparedValue;
    }

    public Long getComparedId() {
        return comparedId;
    }

    public void setComparedId(Long comparedId) {
        this.comparedId = comparedId;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }
}
