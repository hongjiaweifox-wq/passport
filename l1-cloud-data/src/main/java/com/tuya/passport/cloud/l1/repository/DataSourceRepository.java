package com.tuya.passport.cloud.l1.repository;

import com.tuya.passport.cloud.l1.domain.DataSourceEntity;
import com.tuya.passport.cloud.l1.domain.SourceStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DataSourceRepository extends JpaRepository<DataSourceEntity, String>, JpaSpecificationExecutor<DataSourceEntity> {
    boolean existsByOrgCodeAndSourceCode(String orgCode, String sourceCode);
    boolean existsByOrgCodeAndSourceCodeAndIdNot(String orgCode, String sourceCode, String id);
    Optional<DataSourceEntity> findByIdAndOrgCode(String id, String orgCode);
    long countByOrgCode(String orgCode);
    long countByOrgCodeAndStatus(String orgCode, SourceStatus status);
    List<DataSourceEntity> findAllByOrgCodeOrderByUpdatedAtDesc(String orgCode);
}
