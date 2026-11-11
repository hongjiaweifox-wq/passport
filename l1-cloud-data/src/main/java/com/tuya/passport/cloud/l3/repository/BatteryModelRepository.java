package com.tuya.passport.cloud.l3.repository;

import com.tuya.passport.cloud.l3.domain.BatteryModelEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BatteryModelRepository extends JpaRepository<BatteryModelEntity,String> {
 Optional<BatteryModelEntity> findByOrgCodeAndIssuerCodeAndModelCode(String org,String issuer,String code);
 Optional<BatteryModelEntity> findByOrgCodeAndId(String org,String id);
 java.util.List<BatteryModelEntity> findByOrgCodeAndSnProductCode(String org,String snProductCode);
 @Query("select m from BatteryModelEntity m where m.orgCode=:org and (:q is null or :q='' or lower(m.modelCode) like lower(concat('%',:q,'%')) or lower(m.manufacturerName) like lower(concat('%',:q,'%'))) order by m.updatedAt desc")
 Page<BatteryModelEntity> search(@Param("org")String org,@Param("q")String query,Pageable pageable);
 long countByOrgCode(String org);
}
