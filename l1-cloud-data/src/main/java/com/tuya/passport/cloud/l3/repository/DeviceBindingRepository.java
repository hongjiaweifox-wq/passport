package com.tuya.passport.cloud.l3.repository;

import com.tuya.passport.cloud.l3.domain.DeviceBindingEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceBindingRepository extends JpaRepository<DeviceBindingEntity,String> {
 List<DeviceBindingEntity> findByOrgCodeAndBatteryUnitIdOrderByBoundAtDesc(String org,String unit);
 List<DeviceBindingEntity> findByOrgCodeAndBatteryUnitIdAndUnboundAtIsNull(String org,String unit);
 Optional<DeviceBindingEntity> findFirstByOrgCodeAndBatteryUnitIdAndUnboundAtIsNullOrderByBoundAtDesc(String org,String unit);
 long countByOrgCode(String org);
}
