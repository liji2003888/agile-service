package io.choerodon.agile.infra.mapper;

import io.choerodon.agile.infra.dto.MessageDTO;
import io.choerodon.mybatis.common.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


public interface NoticeMapper extends BaseMapper<MessageDTO> {

    List<MessageDTO> selectChangeMessageByProjectId(Long projectId);

    MessageDTO selectChangeMessageByDetail(@Param("projectId") Long projectId,
                                           @Param("event") String event,
                                           @Param("noticeType") String noticeType);

    List<MessageDTO> selectByEvent(String event);

    List<MessageDTO> selectByProjectIdAndEvent(@Param("projectId") Long projectId,
                                               @Param("event") String event);


}
