package io.choerodon.agile.app.service.impl;

import io.choerodon.agile.app.service.IWorkLogService;
import io.choerodon.agile.infra.annotation.DataLog;
import io.choerodon.agile.infra.dto.WorkLogDTO;
import io.choerodon.agile.infra.mapper.WorkLogMapper;
import io.choerodon.agile.infra.utils.BaseFieldUtil;
import io.choerodon.core.exception.CommonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by HuangFuqiang@choerodon.io on 2019/9/5.
 * Email: fuqianghuang01@gmail.com
 */
@Service
public class IWorkLogServiceImpl implements IWorkLogService {

    @Autowired
    private WorkLogMapper workLogMapper;

    @Override
    @DataLog(type = "createWorkLog")
    public WorkLogDTO createBase(WorkLogDTO workLogDTO) {
        if (workLogMapper.insert(workLogDTO) != 1) {
            throw new CommonException("error.workLog.insert");
        }
        return workLogMapper.selectByPrimaryKey(workLogDTO.getLogId());
    }

    @Override
    @DataLog(type = "deleteWorkLog")
    public void deleteBase(Long projectId,Long logId) {
        WorkLogDTO query = new WorkLogDTO();
        query.setProjectId(projectId);
        query.setLogId(logId);
        WorkLogDTO workLogDTO = workLogMapper.selectOne(query);
        if (workLogDTO == null) {
            throw new CommonException("error.workLog.get");
        }
        if (workLogMapper.delete(workLogDTO) != 1) {
            throw new CommonException("error.workLog.delete");
        }
        BaseFieldUtil.updateIssueLastUpdateInfo(workLogDTO.getIssueId(), workLogDTO.getProjectId());
    }

}
