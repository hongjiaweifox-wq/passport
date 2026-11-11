package com.tuya.passport.cloud.l4.repository;

import com.tuya.passport.cloud.l4.domain.ValidationIssueEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValidationIssueRepository extends JpaRepository<ValidationIssueEntity,String>{List<ValidationIssueEntity> findByOrgCodeAndValidationRunIdOrderByCreatedAt(String org,String run);}
