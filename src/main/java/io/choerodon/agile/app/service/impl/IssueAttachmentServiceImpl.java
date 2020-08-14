package io.choerodon.agile.app.service.impl;

import io.choerodon.agile.app.service.IIssueAttachmentService;
import io.choerodon.agile.infra.dto.IssueAttachmentDTO;
import io.choerodon.agile.infra.dto.TestCaseAttachmentDTO;
import io.choerodon.agile.infra.feign.CustomFileRemoteService;
import io.choerodon.agile.infra.utils.BaseFieldUtil;
import io.choerodon.agile.infra.utils.ProjectUtil;
import io.choerodon.core.exception.CommonException;
import io.choerodon.agile.api.vo.IssueAttachmentVO;
import io.choerodon.agile.app.service.IssueAttachmentService;
import io.choerodon.agile.infra.mapper.IssueAttachmentMapper;
import org.hzero.boot.file.FileClient;
import org.hzero.core.util.ResponseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by HuangFuqiang@choerodon.io on 2018/5/16.
 * Email: fuqianghuang01@gmail.com
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class IssueAttachmentServiceImpl implements IssueAttachmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IssueAttachmentServiceImpl.class);

    private static final String BACKETNAME = "agile-service";

    @Autowired
    public IssueAttachmentServiceImpl() {
    }

    @Autowired
    private IssueAttachmentMapper issueAttachmentMapper;

    @Autowired
    private IIssueAttachmentService iIssueAttachmentService;

    @Value("${services.attachment.url}")
    private String attachmentUrl;

    @Autowired
    private FileClient fileClient;

    @Autowired
    private CustomFileRemoteService customFileRemoteService;

    @Autowired
    private ProjectUtil projectUtil;

    @Override
    public void dealIssue(Long projectId, Long issueId, String fileName, String url) {
        IssueAttachmentDTO issueAttachmentDTO = new IssueAttachmentDTO();
        issueAttachmentDTO.setProjectId(projectId);
        issueAttachmentDTO.setIssueId(issueId);
        issueAttachmentDTO.setFileName(fileName);
        issueAttachmentDTO.setUrl(url);
        issueAttachmentDTO.setCommentId(1L);
        iIssueAttachmentService.createBase(issueAttachmentDTO);
        BaseFieldUtil.updateIssueLastUpdateInfo(issueAttachmentDTO.getIssueId(), issueAttachmentDTO.getProjectId());
    }

    @Override
    public List<TestCaseAttachmentDTO> migrateIssueAttachment() {
        List<TestCaseAttachmentDTO> list = issueAttachmentMapper.listAttachmentDTO();
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        return list;
    }

    private String dealUrl(String url) {
        String dealUrl = null;
        try {
            URL netUrl = new URL(url);
            dealUrl = netUrl.getFile().substring(BACKETNAME.length() + 2);
        } catch (MalformedURLException e) {
            throw new CommonException("error.malformed.url", e);
        }
        return dealUrl;
    }

    @Override
    public List<IssueAttachmentVO> create(Long projectId, Long issueId, HttpServletRequest request) {
        List<MultipartFile> files = ((MultipartHttpServletRequest) request).getFiles("file");
        if (files != null && !files.isEmpty()) {
            for (MultipartFile multipartFile : files) {
                String fileName = multipartFile.getOriginalFilename();
                Long organizationId = projectUtil.getOrganizationId(projectId);
                String url = fileClient.uploadFile(organizationId, BACKETNAME, null, fileName, multipartFile);
                dealIssue(projectId, issueId, fileName, dealUrl(url));
            }
        }
        IssueAttachmentDTO issueAttachmentDTO = new IssueAttachmentDTO();
        issueAttachmentDTO.setIssueId(issueId);
        List<IssueAttachmentDTO> issueAttachmentDTOList = issueAttachmentMapper.select(issueAttachmentDTO);
        List<IssueAttachmentVO> result = new ArrayList<>();
        if (issueAttachmentDTOList != null && !issueAttachmentDTOList.isEmpty()) {
            issueAttachmentDTOList.forEach(attachment -> {
                IssueAttachmentVO issueAttachmentVO = new IssueAttachmentVO();
                BeanUtils.copyProperties(attachment, issueAttachmentVO);
                issueAttachmentVO.setUrl(attachmentUrl + "/" + BACKETNAME + "/" + attachment.getUrl());
                result.add(issueAttachmentVO);
            });
        }
        return result;
    }

    @Override
    public Boolean delete(Long projectId, Long issueAttachmentId) {
        IssueAttachmentDTO issueAttachmentDTO = issueAttachmentMapper.selectByPrimaryKey(issueAttachmentId);
        if (issueAttachmentDTO == null) {
            throw new CommonException("error.attachment.get");
        }
        if (!issueAttachmentDTO.getProjectId().equals(projectId)) {
            throw new CommonException("error.project.id.does.not.correspond");
        }
        Boolean result = iIssueAttachmentService.deleteBase(issueAttachmentDTO.getAttachmentId());
        BaseFieldUtil.updateIssueLastUpdateInfo(issueAttachmentDTO.getIssueId(), issueAttachmentDTO.getProjectId());
        String url = null;
        try {
            url = URLDecoder.decode(issueAttachmentDTO.getUrl(), "UTF-8");
            String deleteUrl = attachmentUrl + "/" + BACKETNAME + "/" + url;
            Long organizationId = projectUtil.getOrganizationId(projectId);
            ResponseUtils.getResponse(customFileRemoteService.deleteFileByUrl(organizationId, BACKETNAME, Arrays.asList(deleteUrl)), String.class);
        } catch (Exception e) {
            LOGGER.error("error.attachment.delete", e);
        }
        return result;
    }

    @Override
    public List<String> uploadForAddress(Long projectId, HttpServletRequest request) {
        List<MultipartFile> files = ((MultipartHttpServletRequest) request).getFiles("file");
        if (!(files != null && !files.isEmpty())) {
            throw new CommonException("error.attachment.exits");
        }
        List<String> result = new ArrayList<>();
        for (MultipartFile multipartFile : files) {
            String fileName = multipartFile.getOriginalFilename();
            Long organizationId = projectUtil.getOrganizationId(projectId);
            String url = fileClient.uploadFile(organizationId, BACKETNAME, null, fileName, multipartFile);
            result.add(attachmentUrl + "/" + BACKETNAME + "/" + dealUrl(url));
        }
        return result;
    }

    @Override
    public int deleteByIssueId(Long issueId) {
        IssueAttachmentDTO issueAttachmentDTO = new IssueAttachmentDTO();
        issueAttachmentDTO.setIssueId(issueId);
        return issueAttachmentMapper.delete(issueAttachmentDTO);
    }
}
