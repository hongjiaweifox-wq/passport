package com.tuya.passport.cloud.l2.repository;

import com.tuya.passport.cloud.l2.domain.ImportRowErrorEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportRowErrorRepository extends JpaRepository<ImportRowErrorEntity, String> {
    Page<ImportRowErrorEntity> findByOrgCodeAndJobIdAndAttemptNo(String orgCode, String jobId, int attemptNo, Pageable pageable);
    List<ImportRowErrorEntity> findTop10ByOrgCodeAndJobIdAndAttemptNoOrderByRowNoAsc(String orgCode, String jobId, int attemptNo);
    List<ImportRowErrorEntity> findByOrgCodeAndJobIdAndAttemptNoOrderByRowNoAsc(String orgCode, String jobId, int attemptNo);
}
