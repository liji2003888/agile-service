package io.choerodon.agile.infra.feign;

import io.choerodon.agile.infra.feign.fallback.NotifyFeignClientFallback;
import io.choerodon.agile.infra.feign.vo.MessageSettingVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(value = "hzero-message", fallback = NotifyFeignClientFallback.class)
public interface NotifyFeignClient {


    @GetMapping("/choerodon/v1/projects/{project_id}/message_settings/type/{notify_type}/code/{code}")
    ResponseEntity<MessageSettingVO> getMessageSetting(@PathVariable(value = "project_id") Long projectId,
                                                       @PathVariable(value = "notify_type") String notifyType,
                                                       @PathVariable(value = "code") String code,
                                                       @RequestParam(value = "env_id", required = false) Long envId,
                                                       @RequestParam(value = "event_name", required = false) String eventName);

    @GetMapping("/choerodon/v1/upgrade")
    ResponseEntity<String> checkLog(@RequestParam(value = "version") String version,
                                    @RequestParam(value = "type") String type);
}