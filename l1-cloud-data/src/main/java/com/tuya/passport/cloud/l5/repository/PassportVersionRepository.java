package com.tuya.passport.cloud.l5.repository;

import com.tuya.passport.cloud.l5.domain.PassportVersionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassportVersionRepository extends JpaRepository<PassportVersionEntity,String>{Optional<PassportVersionEntity> findByOrgCodeAndPassportIdAndIdempotencyKey(String org,String passport,String key);Optional<PassportVersionEntity> findByOrgCodeAndPassportIdAndVersionNo(String org,String passport,int version);List<PassportVersionEntity> findByOrgCodeAndPassportIdOrderByVersionNoDesc(String org,String passport);}
