package io.choerodon.agile.api.controller.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.hzero.starter.keyencrypt.core.Encrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;

import io.choerodon.agile.api.vo.AppVersionCreateVO;
import io.choerodon.agile.api.vo.AppVersionSearchVO;
import io.choerodon.agile.api.vo.AppVersionUpdateVO;
import io.choerodon.agile.api.vo.AppVersionVO;
import io.choerodon.agile.app.service.AppVersionService;
import io.choerodon.core.domain.Page;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.mybatis.pagehelper.annotation.SortDefault;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.mybatis.pagehelper.domain.Sort;
import io.choerodon.swagger.annotation.CustomPageRequest;
import io.choerodon.swagger.annotation.Permission;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author superlee
 * @since 2021-03-10
 */
@RestController
@RequestMapping(value = "/v1/projects/{project_id}/app_version")
public class AppVersionController {

    @Autowired
    private AppVersionService appVersionService;

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("创建应用版本")
    @PostMapping
    public ResponseEntity<AppVersionVO> createAppVersion(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(name = "project_id") Long projectId,
            @ApiParam(value = "新增应用版本", required = true)
            @RequestBody @Valid AppVersionCreateVO appVersionCreateVO) {
        return Optional.ofNullable(appVersionService.createAppVersion(
                projectId, appVersionCreateVO))
                .map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
                .orElseThrow(() -> new CommonException("error.appVersion.create"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("批量创建应用版本")
    @PostMapping("/batch")
    public ResponseEntity<List<AppVersionVO>> batchCreateAppVersion(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(name = "project_id") Long projectId,
            @ApiParam(value = "新增应用版本", required = true)
            @RequestBody @Valid List<AppVersionCreateVO> appVersionCreateVOList) {
        return Optional.ofNullable(appVersionService.batchCreateAppVersion(
                projectId, appVersionCreateVOList))
                .map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
                .orElseThrow(() -> new CommonException("error.appVersion.create"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("更新应用版本")
    @PutMapping(value = "/update/{appVersionId}")
    public ResponseEntity<AppVersionVO> updateVersion(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(name = "project_id") Long projectId,
            @ApiParam(value = "appVersionId", required = true)
            @PathVariable @Encrypt Long appVersionId,
            @ApiParam(value = "version信息", required = true)
            @RequestBody AppVersionUpdateVO appVersionUpdateVO) {
        return Optional.ofNullable(appVersionService.updateAppVersion(projectId, appVersionId, appVersionUpdateVO))
                .map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
                .orElseThrow(() -> new CommonException("error.appVersion.update"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @CustomPageRequest
    @ApiOperation(value = "根据项目id查找version")
    @GetMapping(value = "/{appVersionId}")
    public ResponseEntity<AppVersionVO> queryAppVersionById(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(name = "project_id") Long projectId,
            @ApiParam(value = "appVersionId", required = true)
            @PathVariable @Encrypt Long appVersionId) {
        return Optional.ofNullable(appVersionService.queryAppVersionById(projectId, appVersionId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.version.query"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @CustomPageRequest
    @ApiOperation(value = "分页查询项目下应用版本")
    @GetMapping
    public ResponseEntity<Page<AppVersionVO>> listAppVersionByProjectId(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(name = "project_id") Long projectId,
            @SortDefault(value = {"service_code", "version"}, direction = Sort.Direction.ASC)
                    PageRequest pageRequest,
            @ApiParam(value = "筛选条件") AppVersionSearchVO appVersionSearchVO) {
        return Optional.ofNullable(appVersionService.listAppVersionByProjectId(projectId, appVersionSearchVO, pageRequest))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.version.query"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("根据id删除应用版本")
    @DeleteMapping(value = "/delete/{appVersionId}")
    public ResponseEntity<Void> deleteAppVersion(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(name = "project_id") Long projectId,
            @ApiParam(value = "versionId", required = true)
            @PathVariable @Encrypt Long appVersionId) {
        appVersionService.deleteAppVersion(projectId, appVersionId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("根据id删除应用版本与产品版本关系")
    @DeleteMapping(value = "/delete/{appVersionId}/product_version/{productVersionId}")
    public ResponseEntity<Void> deleteAppVersionProductVersionRel(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(name = "project_id") Long projectId,
            @ApiParam(value = "versionId", required = true)
            @PathVariable @Encrypt Long appVersionId,
            @PathVariable @Encrypt Long productVersionId) {
        appVersionService.deleteAppVersionProductVersionRel(projectId, appVersionId, productVersionId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("删除应用版本与问题关系")
    @DeleteMapping(value = "/delete/{appVersionId}/issue/{issueId}")
    public ResponseEntity<Void> deleteAppVersionIssueRel(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(name = "project_id") Long projectId,
            @ApiParam(value = "versionId", required = true)
            @PathVariable @Encrypt Long appVersionId,
            @PathVariable @Encrypt Long issueId) {
        appVersionService.deleteAppVersionIssueRel(projectId, appVersionId, issueId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "应用版本是否重复")
    @PostMapping(value = "/check")
    public ResponseEntity<Boolean> checkTag(
            @ApiParam(value = "项目id", required = true)
            @PathVariable(name = "project_id") Long projectId,
            @ApiParam(value = "name", required = true)
            @RequestBody @Valid AppVersionVO appVersionVO) {
        return Optional.ofNullable(appVersionService.checkRepeat(projectId, appVersionVO))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.appVersion.check"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "解析pom返回数据")
    @PostMapping(value = "/parse_pom")
    public ResponseEntity<List<AppVersionVO>> parsePom(@ApiParam(value = "项目id", required = true)
                                                       @PathVariable(name = "project_id") Long projectId,
                                                       @ApiParam(value = "groupId", required = true)
                                                       @RequestParam(required = false) String groupIds,
                                                       @RequestBody MultipartFile file) {
        return Optional.ofNullable(appVersionService.parsePom(projectId, groupIds, file))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.appVersion.parsePom"));
    }
}
