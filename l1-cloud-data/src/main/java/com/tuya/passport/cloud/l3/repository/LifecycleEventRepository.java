package com.tuya.passport.cloud.l3.repository;

import com.tuya.passport.cloud.l3.domain.LifecycleEventEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LifecycleEventRepository extends JpaRepository<LifecycleEventEntity,String> {
 Optional<LifecycleEventEntity> findByOrgCodeAndSourceSystemAndSourceRecordId(String org,String source,String recordId);
 List<LifecycleEventEntity> findByOrgCodeAndBatteryUnitIdOrderByEventTimeDesc(String org,String unit);
 long countByOrgCode(String org);
}
