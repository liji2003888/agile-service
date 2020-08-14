package io.choerodon.agile.api.controller.v1;

import com.alibaba.fastjson.JSONObject;
import io.choerodon.agile.api.vo.SearchVO;
import io.choerodon.agile.api.vo.StoryMapDragVO;
import io.choerodon.agile.api.vo.StoryMapVO;
import io.choerodon.agile.app.service.StoryMapService;
import io.choerodon.agile.infra.utils.EncryptionUtils;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.swagger.annotation.Permission;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.iam.InitRoleCode;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import org.hzero.starter.keyencrypt.core.Encrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Created by HuangFuqiang@choerodon.io on 2019/5/31.
 * Email: fuqianghuang01@gmail.com
 */
@RestController
@RequestMapping(value = "/v1/projects/{project_id}/story_map")
public class StoryMapController {

    @Autowired
    private StoryMapService storyMapService;

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("查询故事地图整体")
    @PostMapping("/main")
    public ResponseEntity<StoryMapVO> queryStoryMap(@ApiParam(value = "项目id", required = true)
                                                    @PathVariable(name = "project_id") Long projectId,
                                                    @ApiParam(value = "组织id", required = true)
                                                    @RequestParam Long organizationId,
                                                    @ApiParam(value = "search DTO", required = true)
                                                    @RequestBody SearchVO searchVO) {
        EncryptionUtils.decryptSearchVO(searchVO);
        return Optional.ofNullable(storyMapService.queryStoryMap(projectId, organizationId, searchVO))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.storyMap.get"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("查询故事地图需求池")
    @PostMapping("/demand")
    public ResponseEntity<StoryMapVO> queryStoryMapDemand(@ApiParam(value = "项目id", required = true)
                                                          @PathVariable(name = "project_id") Long projectId,
                                                          @ApiParam(value = "search DTO", required = true)
                                                          @RequestBody SearchVO searchVO) {
        EncryptionUtils.decryptSearchVO(searchVO);
        return Optional.ofNullable(storyMapService.queryStoryMapDemand(projectId, searchVO))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.storyMapDemand.get"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("故事地图移动卡片")
    @PostMapping(value = "/move")
    public ResponseEntity<Void> storyMapMove(@ApiParam(value = "项目id", required = true)
                                       @PathVariable(name = "project_id") Long projectId,
                                       @ApiParam(value = "story map drag DTO", required = true)
                                       @RequestBody @Encrypt StoryMapDragVO storyMapDragVO) {
        storyMapService.storyMapMove(projectId, storyMapDragVO);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

}
