package com.tuya.passport.cloud.l3.repository;

import com.tuya.passport.cloud.l3.domain.PromotionRunEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionRunRepository extends JpaRepository<PromotionRunEntity,String> {
 Optional<PromotionRunEntity> findTopByOrgCodeAndJobIdAndAttemptNoOrderByRunNoDesc(String org,String job,int attempt);
 Optional<PromotionRunEntity> findByOrgCodeAndJobIdAndAttemptNoAndRunNo(String org,String job,int attempt,int runNo);
 List<PromotionRunEntity> findByOrgCodeAndJobIdOrderByRunNoDesc(String org,String job);
}
