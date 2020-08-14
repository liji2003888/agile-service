package io.choerodon.agile.api.controller.v1;

import io.choerodon.agile.infra.utils.EncryptionUtils;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.swagger.annotation.Permission;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * @author zhaotianxin
 * @date 2020-07-29 17:47
 */
@RestController
@RequestMapping(value = "/v1/projects/{project_id}")
public class EncryptionController {

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("加密")
    @GetMapping("/encrypt")
    public ResponseEntity<String> startArt(@ApiParam(value = "项目id", required = true)
                                           @PathVariable(name = "project_id") Long projectId,
                                           @RequestParam Long issueId) {
        return ResponseEntity.ok(EncryptionUtils.encrypt(issueId));
    }
}
