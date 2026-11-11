package com.tuya.passport.cloud.l2.repository;

import com.tuya.passport.cloud.l2.domain.ImportJobEntity;
import com.tuya.passport.cloud.l2.domain.ImportJobStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImportJobRepository extends JpaRepository<ImportJobEntity, String> {
    Optional<ImportJobEntity> findByOrgCodeAndId(String orgCode, String id);
    Optional<ImportJobEntity> findByOrgCodeAndIdempotencyKey(String orgCode, String key);
    Optional<ImportJobEntity> findFirstByOrgCodeAndFileSha256AndIdNotOrderByCreatedAtDesc(String orgCode, String sha, String id);
    @Query("select j from ImportJobEntity j where j.orgCode=:org and (:q is null or :q='' or lower(j.fileName) like lower(concat('%',:q,'%')) or lower(j.sourceName) like lower(concat('%',:q,'%')) or lower(j.sourceCode) like lower(concat('%',:q,'%'))) and (:status is null or j.status=:status) and (:entityType is null or j.entityType=:entityType) order by j.createdAt desc")
    Page<ImportJobEntity> search(@Param("org") String org, @Param("q") String q, @Param("status") ImportJobStatus status, @Param("entityType") com.tuya.passport.cloud.l2.domain.EntityType entityType, Pageable pageable);
    long countByOrgCode(String orgCode);
    long countByOrgCodeAndStatusIn(String orgCode, List<ImportJobStatus> statuses);
    long countByOrgCodeAndStatus(String orgCode, ImportJobStatus status);
    List<ImportJobEntity> findByStatusIn(List<ImportJobStatus> statuses);
}
