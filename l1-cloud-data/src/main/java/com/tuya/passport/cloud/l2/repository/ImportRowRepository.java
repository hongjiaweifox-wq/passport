package com.tuya.passport.cloud.l2.repository;

import com.tuya.passport.cloud.l2.domain.ImportRowEntity;
import com.tuya.passport.cloud.l2.domain.ImportRowStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportRowRepository extends JpaRepository<ImportRowEntity, String> {
    Page<ImportRowEntity> findByOrgCodeAndJobIdAndAttemptNo(String orgCode, String jobId, int attemptNo, Pageable pageable);
    List<ImportRowEntity> findByOrgCodeAndJobIdAndAttemptNo(String orgCode, String jobId, int attemptNo);
    List<ImportRowEntity> findByOrgCodeAndJobIdAndAttemptNoAndSourceRecordId(String orgCode, String jobId, int attemptNo, String sourceRecordId);
    long countByOrgCodeAndJobIdAndAttemptNoAndStatus(String orgCode, String jobId, int attemptNo, ImportRowStatus status);
}
