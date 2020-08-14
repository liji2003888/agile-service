package io.choerodon.agile.infra.mapper;

import java.util.List;
import io.choerodon.mybatis.common.BaseMapper;
import io.choerodon.agile.infra.dto.*;
import org.apache.ibatis.annotations.Param;


/**
 * 敏捷开发Issue标签关联
 *
 * @author dinghuang123@gmail.com
 * @since 2018-05-14 21:31:22
 */
public interface LabelIssueRelMapper extends BaseMapper<LabelIssueRelDTO> {

}