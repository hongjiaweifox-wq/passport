package com.tuya.passport.cloud.l3.repository;

import com.tuya.passport.cloud.l3.domain.ModelRevisionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModelRevisionRepository extends JpaRepository<ModelRevisionEntity,String> {
 Optional<ModelRevisionEntity> findByOrgCodeAndModelIdAndRevisionCode(String org,String modelId,String code);
 Optional<ModelRevisionEntity> findByOrgCodeAndId(String org,String id);
 List<ModelRevisionEntity> findByOrgCodeAndModelIdOrderByCreatedAtDesc(String org,String modelId);
 long countByOrgCode(String org);
}
