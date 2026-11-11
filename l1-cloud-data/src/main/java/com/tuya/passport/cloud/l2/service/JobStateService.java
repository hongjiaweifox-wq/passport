package com.tuya.passport.cloud.l2.service;

import com.tuya.passport.cloud.l2.domain.ImportJobEntity;
import com.tuya.passport.cloud.l2.domain.ImportJobStatus;
import com.tuya.passport.cloud.l2.repository.ImportJobRepository;
import com.tuya.passport.cloud.l2.support.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobStateService {
    private final ImportJobRepository jobs; private final com.tuya.passport.cloud.l2.repository.ImportRowRepository rows; private final com.tuya.passport.cloud.l2.repository.ImportRowErrorRepository errors;
    public JobStateService(ImportJobRepository jobs, com.tuya.passport.cloud.l2.repository.ImportRowRepository rows, com.tuya.passport.cloud.l2.repository.ImportRowErrorRepository errors) { this.jobs=jobs; this.rows=rows; this.errors=errors; }
    @Transactional public void transition(String org, String id, ImportJobStatus status) { job(org,id).changeStatus(status); }
    @Transactional public void fail(String org, String id, String code, String message) { job(org,id).fail(code,message); }
    @Transactional public void counts(String org, String id, int total, int accepted, int rejected, int duplicate) { job(org,id).setCounts(total,accepted,rejected,duplicate); }
    @Transactional public void saveBatch(String org, String id, int attempt, List<com.tuya.passport.cloud.l2.domain.ImportRowEntity> rowBatch, List<com.tuya.passport.cloud.l2.domain.ImportRowErrorEntity> errorBatch) {
        rows.saveAll(rowBatch); errors.saveAll(errorBatch); rows.flush(); errors.flush();
    }
    @Transactional public ImportJobEntity beginRetry(String org,String id) {
        ImportJobEntity entity = job(org,id);
        if (entity.getStatus()!=ImportJobStatus.PARTIAL && entity.getStatus()!=ImportJobStatus.FAILED) throw new ApiException("L2_JOB_NOT_RETRYABLE","仅部分成功或失败任务可以重试", HttpStatus.CONFLICT);
        entity.beginRetry(); return entity;
    }
    @Transactional(readOnly = true) public org.springframework.data.domain.Page<com.tuya.passport.cloud.l2.domain.ImportRowEntity> rows(String org, String id, int attempt, org.springframework.data.domain.Pageable pageable) { return rows.findByOrgCodeAndJobIdAndAttemptNo(org,id,attempt,pageable); }
    @Transactional(readOnly = true) public org.springframework.data.domain.Page<com.tuya.passport.cloud.l2.domain.ImportRowErrorEntity> errors(String org, String id, int attempt, org.springframework.data.domain.Pageable pageable) { return errors.findByOrgCodeAndJobIdAndAttemptNo(org,id,attempt,pageable); }
    @Transactional(readOnly = true) public List<com.tuya.passport.cloud.l2.domain.ImportRowErrorEntity> topErrors(String org, String id, int attempt) { return errors.findTop10ByOrgCodeAndJobIdAndAttemptNoOrderByRowNoAsc(org,id,attempt); }
    @Transactional(readOnly = true) public List<com.tuya.passport.cloud.l2.domain.ImportRowErrorEntity> allErrors(String org, String id, int attempt) { return errors.findByOrgCodeAndJobIdAndAttemptNoOrderByRowNoAsc(org,id,attempt); }
    @Transactional(readOnly = true) public List<com.tuya.passport.cloud.l2.domain.ImportRowEntity> allRows(String org, String id, int attempt) { return rows.findByOrgCodeAndJobIdAndAttemptNo(org,id,attempt); }
    @Transactional(readOnly = true) public ImportJobEntity job(String org, String id) { return jobs.findByOrgCodeAndId(org,id).orElseThrow(() -> new ApiException("L2_NOT_FOUND","Import job not found", HttpStatus.NOT_FOUND)); }
}
