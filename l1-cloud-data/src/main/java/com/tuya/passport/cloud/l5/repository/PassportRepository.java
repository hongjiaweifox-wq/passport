package com.tuya.passport.cloud.l5.repository;

import com.tuya.passport.cloud.l5.domain.PassportEntity;
import com.tuya.passport.cloud.l5.domain.PassportStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassportRepository extends JpaRepository<PassportEntity,String>{Optional<PassportEntity> findByOrgCodeAndId(String org,String id);Optional<PassportEntity> findByOrgCodeAndBatteryUnitId(String org,String unit);Optional<PassportEntity> findByPublicCode(String code);Page<PassportEntity> findByOrgCode(String org,Pageable pageable);Page<PassportEntity> findByOrgCodeAndStatus(String org,PassportStatus status,Pageable pageable);long countByOrgCode(String org);long countByOrgCodeAndStatus(String org,PassportStatus status);}
