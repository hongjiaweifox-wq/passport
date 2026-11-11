package com.tuya.passport.cloud.l4.repository;

import com.tuya.passport.cloud.l4.domain.ReviewStatus;
import com.tuya.passport.cloud.l4.domain.ReviewTaskEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewTaskRepository extends JpaRepository<ReviewTaskEntity,String>{Optional<ReviewTaskEntity> findByOrgCodeAndId(String org,String id);Optional<ReviewTaskEntity> findFirstByOrgCodeAndSubjectTypeAndSubjectIdAndSnapshotHashAndStatus(String org,String type,String subject,String hash,ReviewStatus status);Page<ReviewTaskEntity> findByOrgCodeAndStatus(String org,ReviewStatus status,Pageable pageable);Page<ReviewTaskEntity> findByOrgCode(String org,Pageable pageable);long countByOrgCodeAndStatus(String org,ReviewStatus status);}
