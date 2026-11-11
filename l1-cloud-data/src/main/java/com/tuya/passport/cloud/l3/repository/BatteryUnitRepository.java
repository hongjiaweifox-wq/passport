package com.tuya.passport.cloud.l3.repository;

import com.tuya.passport.cloud.l3.domain.BatteryUnitEntity;
import com.tuya.passport.cloud.l3.domain.BatteryUnitStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatteryUnitRepository extends JpaRepository<BatteryUnitEntity,String> {
 Optional<BatteryUnitEntity> findByOrgCodeAndId(String org,String id);
 Optional<BatteryUnitEntity> findByOrgCodeAndIssuerCodeAndSnNormalized(String org,String issuer,String sn);
 Optional<BatteryUnitEntity> findByOrgCodeAndSourceSystemAndSourceRecordId(String org,String source,String recordId);
 Page<BatteryUnitEntity> findByOrgCode(String org,Pageable pageable);
 Page<BatteryUnitEntity> findByOrgCodeAndStatus(String org,BatteryUnitStatus status,Pageable pageable);
 long countByOrgCode(String org);
 long countByOrgCodeAndStatus(String org,BatteryUnitStatus status);
}
