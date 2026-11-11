package com.tuya.passport.cloud.l4.repository;

import com.tuya.passport.cloud.l4.domain.FieldDefinitionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FieldDefinitionRepository extends JpaRepository<FieldDefinitionEntity,String>{List<FieldDefinitionEntity> findByOrgCodeAndSchemaVersionAndStatusOrderByFieldCode(String org,String schema,String status);Optional<FieldDefinitionEntity> findByOrgCodeAndFieldCodeAndSchemaVersion(String org,String field,String schema);}
