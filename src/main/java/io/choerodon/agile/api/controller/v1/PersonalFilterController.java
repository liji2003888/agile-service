package io.choerodon.agile.api.controller.v1;

import io.choerodon.agile.api.vo.PersonalFilterVO;
import io.choerodon.agile.app.service.PersonalFilterService;

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

import java.util.List;
import java.util.Optional;

/**
 * @author shinan.chen
 * @since 2019/2/25
 */
@RestController
@RequestMapping(value = "/v1/projects/{project_id}/personal_filter")
public class PersonalFilterController {
    @Autowired
    private PersonalFilterService personalFilterService;

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("创建我的筛选")
    @PostMapping
    public ResponseEntity<PersonalFilterVO> create(@ApiParam(value = "项目id", required = true)
                                                   @PathVariable(name = "project_id") Long projectId,
                                                   @ApiParam(value = "personal filter object", required = true)
                                                   @RequestBody @Encrypt PersonalFilterVO personalFilterVO) {
        return Optional.ofNullable(personalFilterService.create(projectId, personalFilterVO))
                .map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
                .orElseThrow(() -> new CommonException("error.personalFilter.create"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("修改我的筛选")
    @PutMapping(value = "/{filterId}")
    public ResponseEntity<PersonalFilterVO> update(@ApiParam(value = "项目id", required = true)
                                                   @PathVariable(name = "project_id") Long projectId,
                                                   @ApiParam(value = "filter id", required = true)
                                                   @PathVariable  @Encrypt Long filterId,
                                                   @ApiParam(value = "personal filter object", required = true)
                                                   @RequestBody  @Encrypt PersonalFilterVO personalFilterVO) {
        return Optional.ofNullable(personalFilterService.update(projectId, filterId, personalFilterVO))
                .map(result -> new ResponseEntity<>(result, HttpStatus.CREATED))
                .orElseThrow(() -> new CommonException("error.personalFilter.create"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("删除我的筛选")
    @DeleteMapping(value = "/{filterId}")
    public ResponseEntity<PersonalFilterVO> deleteById(@ApiParam(value = "项目id", required = true)
                                                       @PathVariable(name = "project_id") Long projectId,
                                                       @ApiParam(value = "filter id", required = true)
                                                       @PathVariable  @Encrypt Long filterId) {
        personalFilterService.deleteById(projectId, filterId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("查询我的筛选列表")
    @GetMapping(value = "/query_all/{userId}")
    public ResponseEntity<List<PersonalFilterVO>> listByProjectId(@ApiParam(value = "项目id", required = true)
                                                                  @PathVariable(name = "project_id") Long projectId,
                                                                  @ApiParam(value = "用户id", required = true)
                                                                  @PathVariable @Encrypt Long userId,
                                                                  @RequestParam(name = "searchStr", required = false) String searchStr) {
        return Optional.ofNullable(personalFilterService.listByProjectId(projectId, userId, searchStr))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.personalFilter.list"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("根据id查询我的筛选")
    @GetMapping(value = "/{filterId}")
    public ResponseEntity<PersonalFilterVO> queryById(@ApiParam(value = "项目id", required = true)
                                                      @PathVariable(name = "project_id") Long projectId,
                                                      @ApiParam(value = "filter id", required = true)
                                                      @PathVariable @Encrypt Long filterId) {
        return Optional.ofNullable(personalFilterService.queryById(projectId, filterId))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.personalFilter.queryById"));
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation("我的筛选重名校验")
    @GetMapping(value = "/check_name")
    public ResponseEntity<Boolean> checkName(@ApiParam(value = "项目id", required = true)
                                             @PathVariable(name = "project_id") Long projectId,
                                             @ApiParam(value = "用户id", required = true)
                                             @RequestParam @Encrypt Long userId,
                                             @ApiParam(value = "name", required = true)
                                             @RequestParam String name) {
        return Optional.ofNullable(personalFilterService.checkName(projectId, userId, name))
                .map(result -> new ResponseEntity<>(result, HttpStatus.OK))
                .orElseThrow(() -> new CommonException("error.checkName.get"));
    }
}
