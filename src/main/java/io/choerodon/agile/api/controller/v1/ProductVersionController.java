package io.choerodon.agile.api.controller.v1;

import com.alibaba.fastjson.JSONObject;
import io.choerodon.agile.api.vo.*;
import io.choerodon.agile.app.service.ProductVersionService;

import io.choerodon.agile.infra.utils.VerifyUpdateUtil;
import io.choerodon.core.domain.Page;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.mybatis.pagehelper.domain.Sort;
import io.choerodon.swagger.annotation.Permission;
import io.choerodon.core.exception.CommonException;
import io.choerodon.mybatis.pagehelper.annotation.SortDefault;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.swagger.annotation.CustomPageRequest;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.hzero.starter.keyencrypt.core.Encrypt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * Created by jian_zhang02@163.com on 2018/5/14.
 */
@RestController
@RequestMapping(value = "/v1/projects/{project_id}/product_version")
public class ProductVersionController {

    private static final String CREATE_ERROR = "error.version.create";
    private static final String UPDATE_ERROR = "error.version.update";
    private static final String DELETE_ERROR = "error.version.delete";
    private static final String QUERY_ERROR = "error.version.query";
    private static final String DRAG_ERROR = "error.version.dragVersion";
    private static final String CHECK_ERROR = "error.version.check";
    private static final String QUERY_VERSION_ERROR = "error.versionData.query";
    private static final String VERSION_STATISTICS_ERROR = "error.versionStatistics.query";
    private static final String QUERY_PLAN_VERSION_NAME_ERROR = "error.planVersionName.query";
    private static final String QUERY_VERSION_NAME_ERROR = "error.versionName.query";
    private static final String RELEASE_ERROR = "error.productVersion.release";
    private static final String REVOKE_RELEASE_ERROR = "error.productVersion.revokeRelease";
    private static final String ARCHIVED_ERROR = "error.productVersion.archived";
    private static final String REVOKE_ARCHIVED_ERROR = "error.productVersion.revokeArchived";

    @Autowired
    private ProductVersionService productVersionService;

    @Autowired
    private VerifyUpdateUtil verifyUpdateUtil;

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("创建version")
    @PostMapping
    public ResponseEntity<ProductVersionDetailVO> createVersion(@ApiParam(value = "项目id", required = true)
                                                                 @PathVariable(name = "project_id") Long projectId,
                                                                @ApiParam(value = "releasePlan信息", required = true)
                                                                 @RequestBody @Valid ProductVersionCreateVO versionCreateVO) {
        return Optional.ofNullable(productVersionService.createVersion(projectId, versionCreateVO))
                .map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
                .orElseThrow(() -> new CommonException(CREATE_ERROR));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("更新version")
    @PutMapping(value = "/update/{versionId}")
    public ResponseEntity<ProductVersionDetailVO> updateVersion(@ApiParam(value = "项目id", required = true)
                                                                 @PathVariable(name = "project_id") Long projectId,
                                                                @ApiParam(value = "versionId", required = true)
                                                                 @PathVariable @Encrypt Long versionId,
                                                                @ApiParam(value = "version信息", required = true)
                                                                 @RequestBody JSONObject versionUpdateDTO) {
        ProductVersionUpdateVO productVersionUpdate = new ProductVersionUpdateVO();
        List<String> fieldList = verifyUpdateUtil.verifyUpdateData(versionUpdateDTO, productVersionUpdate);
        return Optional.ofNullable(productVersionService.updateVersion(projectId, versionId, productVersionUpdate, fieldList))
                .map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
                .orElseThrow(() -> new CommonException(UPDATE_ERROR));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("根据id删除version")
    @DeleteMapping(value = "/delete/{versionId}")
    public ResponseEntity<Boolean> deleteVersion(@ApiParam(value = "项目id", required = true)
                                                 @PathVariable(name = "project_id") Long projectId,
                                                 @ApiParam(value = "versionId", required = true)
                                                 @PathVariable @Encrypt Long versionId,
                                                 @ApiParam(value = "更改的目标版本")
                                                 @RequestParam(required = false, name = "targetVersionId") @Encrypt Long targetVersionId) {
        return Optional.ofNullable(productVersionService.deleteVersion(projectId, versionId, targetVersionId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.NO_CONTENT))
                .orElseThrow(() -> new CommonException(DELETE_ERROR));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @CustomPageRequest
    @ApiOperation(value = "根据项目id查找version")
    @PostMapping(value = "/versions")
    public ResponseEntity<Page<ProductVersionPageVO>> listByOptions(@ApiParam(value = "项目id", required = true)
                                                                     @PathVariable(name = "project_id") Long projectId,
                                                                    @ApiParam(value = "查询参数")
                                                                     @RequestBody(required = false) SearchVO searchVO,
                                                                    @ApiParam(value = "分页信息", required = true)
                                                                     @SortDefault(value = "sequence", direction = Sort.Direction.DESC)
                                                                     @ApiIgnore PageRequest pageRequest) {
        return Optional.ofNullable(productVersionService.queryByProjectId(projectId, pageRequest, searchVO))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException(QUERY_ERROR));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "是否重名")
    @GetMapping(value = "/check")
    public ResponseEntity<Boolean> checkName(@ApiParam(value = "项目id", required = true)
                                             @PathVariable(name = "project_id") Long projectId,
                                             @ApiParam(value = "name", required = true)
                                             @RequestParam String name) {
        return Optional.ofNullable(productVersionService.repeatName(projectId, name))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException(CHECK_ERROR));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "backlog页面查询所有版本")
    @GetMapping
    public ResponseEntity<List<ProductVersionDataVO>> queryVersionByProjectId(@ApiParam(value = "项目id", required = true)
                                                                               @PathVariable(name = "project_id") Long projectId) {
        return Optional.ofNullable(productVersionService.queryVersionByProjectId(projectId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException(QUERY_VERSION_ERROR));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "版本详情及issue统计信息")
    @GetMapping(value = "/{versionId}")
    public ResponseEntity<ProductVersionStatisticsVO> queryVersionStatisticsByVersionId(@ApiParam(value = "项目id", required = true)
                                                                                         @PathVariable(name = "project_id") Long projectId,
                                                                                        @ApiParam(value = "versionId", required = true)
                                                                                         @PathVariable @Encrypt Long versionId) {
        return Optional.ofNullable(productVersionService.queryVersionStatisticsByVersionId(projectId, versionId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException(VERSION_STATISTICS_ERROR));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "查询规划中版本名及要发布版本未完成issue统计")
    @GetMapping(value = "/{versionId}/plan_names")
    public ResponseEntity<VersionMessageVO> queryReleaseMessageByVersionId(@ApiParam(value = "项目id", required = true)
                                                                            @PathVariable(name = "project_id") Long projectId,
                                                                           @ApiParam(value = "versionId", required = true)
                                                                            @PathVariable @Encrypt Long versionId) {
        return Optional.ofNullable(productVersionService.queryReleaseMessageByVersionId(projectId, versionId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException(QUERY_PLAN_VERSION_NAME_ERROR));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "发布版本")
    @PostMapping(value = "/release")
    public ResponseEntity<ProductVersionDetailVO> releaseVersion(@ApiParam(value = "项目id", required = true)
                                                                  @PathVariable(name = "project_id") Long projectId,
                                                                 @ApiParam(value = "发布版本信息", required = true)
                                                                  @RequestBody @Valid ProductVersionReleaseVO productVersionRelease) {
        return Optional.ofNullable(productVersionService.releaseVersion(projectId, productVersionRelease))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException(RELEASE_ERROR));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "撤销发布版本")
    @PostMapping(value = "/{versionId}/revoke_release")
    public ResponseEntity<ProductVersionDetailVO> revokeReleaseVersion(@ApiParam(value = "项目id", required = true)
                                                                        @PathVariable(name = "project_id") Long projectId,
                                                                       @ApiParam(value = "版本id", required = true)
                                                                        @PathVariable @Encrypt Long versionId) {
        return Optional.ofNullable(productVersionService.revokeReleaseVersion(projectId, versionId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException(REVOKE_RELEASE_ERROR));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "归档版本")
    @PostMapping(value = "/{versionId}/archived")
    public ResponseEntity<ProductVersionDetailVO> archivedVersion(@ApiParam(value = "项目id", required = true)
                                                                   @PathVariable(name = "project_id") Long projectId,
                                                                  @ApiParam(value = "版本id", required = true)
                                                                   @PathVariable @Encrypt Long versionId) {
        return Optional.ofNullable(productVersionService.archivedVersion(projectId, versionId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException(ARCHIVED_ERROR));
    }


    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "撤销归档版本")
    @PostMapping(value = "/{versionId}/revoke_archived")
    public ResponseEntity<ProductVersionDetailVO> revokeArchivedVersion(@ApiParam(value = "项目id", required = true)
                                                                         @PathVariable(name = "project_id") Long projectId,
                                                                        @ApiParam(value = "版本id", required = true)
                                                                         @PathVariable @Encrypt Long versionId) {
        return Optional.ofNullable(productVersionService.revokeArchivedVersion(projectId, versionId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException(REVOKE_ARCHIVED_ERROR));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "查询所有版本名及要删除版本issue统计")
    @GetMapping(value = "/{versionId}/names")
    public ResponseEntity<VersionMessageVO> queryDeleteMessageByVersionId(@ApiParam(value = "项目id", required = true)
                                                                           @PathVariable(name = "project_id") Long projectId,
                                                                          @ApiParam(value = "versionId", required = true)
                                                                           @PathVariable @Encrypt Long versionId) {
        return Optional.ofNullable(productVersionService.queryDeleteMessageByVersionId(projectId, versionId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException(QUERY_VERSION_NAME_ERROR));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "根据状态查询版本名")
    @PostMapping(value = "/names")
    public ResponseEntity<List<ProductVersionNameVO>> queryNameByOptions(@ApiParam(value = "项目id", required = true)
                                                                          @PathVariable(name = "project_id") Long projectId,
                                                                         @ApiParam(value = "状态列表", required = false)
                                                                          @RequestBody(required = false) List<String> statusCodes) {
        return Optional.ofNullable(productVersionService.queryNameByOptions(projectId, statusCodes))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException(QUERY_VERSION_NAME_ERROR));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "根据项目id查找version列表")
    @GetMapping(value = "/versions")
    public ResponseEntity<List<ProductVersionVO>> listByProjectId(@ApiParam(value = "项目id", required = true)
                                                                   @PathVariable(name = "project_id") Long projectId) {
        return Optional.ofNullable(productVersionService.listByProjectId(projectId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException(QUERY_ERROR));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "查找所有项目的version ids")
    @GetMapping(value = "/ids")
    public ResponseEntity<List<Long>> listIds(@ApiParam(value = "项目id", required = true)
                                              @PathVariable(name = "project_id") Long projectId) {
        return Optional.ofNullable(productVersionService.listIds(projectId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.versionIds.get"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "拖动版本位置")
    @PutMapping(value = "/drag")
    public ResponseEntity<ProductVersionPageVO> dragVersion(@ApiParam(value = "项目id", required = true)
                                                             @PathVariable(name = "project_id") Long projectId,
                                                            @ApiParam(value = "排序对象", required = true)
                                                             @RequestBody VersionSequenceVO versionSequenceVO) {
        return Optional.ofNullable(productVersionService.dragVersion(projectId, versionSequenceVO))
                .map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
                .orElseThrow(() -> new CommonException(DRAG_ERROR));
    }
}
