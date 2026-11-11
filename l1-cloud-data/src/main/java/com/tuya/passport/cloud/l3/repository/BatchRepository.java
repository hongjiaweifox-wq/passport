package com.tuya.passport.cloud.l3.repository;

import com.tuya.passport.cloud.l3.domain.BatchEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchRepository extends JpaRepository<BatchEntity,String> {
 Optional<BatchEntity> findByOrgCodeAndModelRevisionIdAndBatchCode(String org,String revision,String code);
 Optional<BatchEntity> findByOrgCodeAndId(String org,String id);
}
