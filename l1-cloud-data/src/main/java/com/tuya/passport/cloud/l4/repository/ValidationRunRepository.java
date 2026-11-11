package com.tuya.passport.cloud.l4.repository;

import com.tuya.passport.cloud.l4.domain.ValidationRunEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValidationRunRepository extends JpaRepository<ValidationRunEntity,String>{Optional<ValidationRunEntity> findByOrgCodeAndId(String org,String id);Page<ValidationRunEntity> findByOrgCode(String org,Pageable pageable);Page<ValidationRunEntity> findByOrgCodeAndSubjectTypeAndSubjectId(String org,String type,String subject,Pageable pageable);List<ValidationRunEntity> findByOrgCodeOrderByFinishedAtDesc(String org);long countByOrgCode(String org);long countByOrgCodeAndStatus(String org,com.tuya.passport.cloud.l4.domain.ValidationStatus status);}
