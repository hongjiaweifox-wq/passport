package com.tuya.passport.cloud.l3.repository;

import com.tuya.passport.cloud.l3.domain.PromotionItemEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionItemRepository extends JpaRepository<PromotionItemEntity,String> {
 List<PromotionItemEntity> findByOrgCodeAndRunIdOrderByRowNo(String org,String run);
 Page<PromotionItemEntity> findByOrgCodeAndRunId(String org,String run,Pageable pageable);
}
