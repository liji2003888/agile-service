package io.choerodon.agile.app.service.impl;

import io.choerodon.agile.app.service.IIssueCommentService;
import io.choerodon.agile.infra.annotation.DataLog;
import io.choerodon.agile.infra.dto.IssueCommentDTO;
import io.choerodon.agile.infra.mapper.IssueCommentMapper;
import io.choerodon.agile.infra.utils.BaseFieldUtil;
import io.choerodon.core.exception.CommonException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class IIssueCommentServiceImpl implements IIssueCommentService {

    private static final String UPDATE_ERROR = "error.IssueComment.update";
    private static final String INSERT_ERROR = "error.IssueComment.insert";
    private static final String DELETE_ERROR = "error.IssueComment.delete";

    @Autowired
    private IssueCommentMapper issueCommentMapper;
    @Autowired
    private ModelMapper modelMapper;

    @Override
    @DataLog(type = "createComment")
    public IssueCommentDTO createBase(IssueCommentDTO issueCommentDTO) {
        if (issueCommentMapper.insert(issueCommentDTO) != 1) {
            throw new CommonException(INSERT_ERROR);
        }
        BaseFieldUtil.updateIssueLastUpdateInfo(issueCommentDTO.getIssueId(), issueCommentDTO.getProjectId());
        return modelMapper.map(issueCommentMapper.selectByPrimaryKey(issueCommentDTO.getCommentId()), IssueCommentDTO.class);
    }

    @Override
    @DataLog(type = "updateComment")
    public IssueCommentDTO updateBase(IssueCommentDTO issueCommentDTO, String[] fieldList) {
//        Criteria criteria = new Criteria();
//        criteria.update(fieldList);
        if (issueCommentMapper.updateOptional(issueCommentDTO, fieldList) != 1) {
            throw new CommonException(UPDATE_ERROR);
        }
        BaseFieldUtil.updateIssueLastUpdateInfoByCommentId(issueCommentMapper, issueCommentDTO.getCommentId());
        return modelMapper.map(issueCommentMapper.selectByPrimaryKey(issueCommentDTO.getCommentId()), IssueCommentDTO.class);
    }

    @Override
    @DataLog(type = "deleteComment")
    public int deleteBase(IssueCommentDTO issueCommentDTO) {
        int isDelete = issueCommentMapper.delete(issueCommentDTO);
        if (isDelete != 1) {
            throw new CommonException(DELETE_ERROR);
        }
        BaseFieldUtil.updateIssueLastUpdateInfo(issueCommentDTO.getIssueId(), issueCommentDTO.getProjectId());
        return isDelete;
    }
}
