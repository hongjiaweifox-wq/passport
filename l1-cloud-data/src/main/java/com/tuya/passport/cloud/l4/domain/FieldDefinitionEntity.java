package com.tuya.passport.cloud.l4.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="l4_field_definition",uniqueConstraints=@UniqueConstraint(name="uk_l4_field_schema",columnNames={"org_code","field_code","schema_version"}))
public class FieldDefinitionEntity {
 @Id @Column(length=36) private String id;
 @Column(name="org_code",nullable=false,length=64) private String orgCode;
 @Column(name="field_code",nullable=false,length=160) private String fieldCode;
 @Column(name="value_type",nullable=false,length=32) private String valueType;
 @Column(name="unit_code",length=32) private String unitCode;
 @Enumerated(EnumType.STRING) @Column(name="visibility_class",nullable=false,length=16) private VisibilityClass visibilityClass;
 @Column(name="schema_version",nullable=false,length=64) private String schemaVersion;
 @Column(name="legal_reference",length=256) private String legalReference;
 @Column(name="definition_json",length=4000) private String definitionJson;
 @Column(nullable=false,length=16) private String status;
 @Column(name="dpp_no") private Integer dppNo;
 @Column(name="category",length=180) private String category;
 @Column(name="sub_category",length=180) private String subCategory;
 @Column(name="display_name_en",length=255) private String displayNameEn;
 @Column(name="display_name_zh",length=255) private String displayNameZh;
 @Column(name="granularity",length=120) private String granularity;
 @Column(name="mandatory_stationary",nullable=true) private Boolean mandatoryStationary;
 @Column(name="static_dynamic",length=16) private String staticDynamic;
 @Column(name="storage_target",length=24) private String storageTarget;
 @Column(name="implementation_status",length=24) private String implementationStatus;
 @Column(name="example_value",length=512) private String exampleValue;
 @Column(name="requirement_notes",length=4000) private String requirementNotes;
 @Column(name="visibility_customized") private Boolean visibilityCustomized;
 @Column(name="created_at",nullable=false) private Instant createdAt;
 @Column(name="updated_at") private Instant updatedAt;
 protected FieldDefinitionEntity(){}
 public FieldDefinitionEntity(String org,String field,String type,String unit,VisibilityClass visibility,String schema,String legal){id=UUID.randomUUID().toString();orgCode=org;fieldCode=field;valueType=type;unitCode=unit;visibilityClass=visibility;schemaVersion=schema;legalReference=legal;definitionJson="{}";status="ACTIVE";createdAt=Instant.now();}
 public void applyCatalog(Integer no,String category,String subCategory,String displayNameEn,String displayNameZh,String granularity,Boolean mandatory,String staticDynamic,String storageTarget,String implementationStatus,String valueType,String unitCode,VisibilityClass visibility,String legalReference,String definitionJson,String exampleValue,String requirementNotes){this.dppNo=no;this.category=category;this.subCategory=subCategory;this.displayNameEn=displayNameEn;this.displayNameZh=displayNameZh;this.granularity=granularity;this.mandatoryStationary=mandatory;this.staticDynamic=staticDynamic;this.storageTarget=storageTarget;this.implementationStatus=implementationStatus;this.valueType=valueType;this.unitCode=unitCode;this.visibilityClass=Boolean.TRUE.equals(visibilityCustomized)?this.visibilityClass:visibility;this.legalReference=legalReference;this.definitionJson=definitionJson;this.exampleValue=exampleValue;this.requirementNotes=requirementNotes;this.status="ACTIVE";this.updatedAt=Instant.now();}
 public void applyExampleValue(String exampleValue){this.exampleValue=exampleValue;this.updatedAt=Instant.now();}
 public void updateVisibility(VisibilityClass visibility){this.visibilityClass=visibility;this.visibilityCustomized=true;this.updatedAt=Instant.now();}
 public void markSuperseded(String definitionJson){this.status="SUPERSEDED";this.definitionJson=definitionJson;this.updatedAt=Instant.now();}
 public String getId(){return id;}public String getOrgCode(){return orgCode;}public String getFieldCode(){return fieldCode;}public String getValueType(){return valueType;}public String getUnitCode(){return unitCode;}public VisibilityClass getVisibilityClass(){return visibilityClass;}public String getSchemaVersion(){return schemaVersion;}public String getLegalReference(){return legalReference;}public String getDefinitionJson(){return definitionJson;}public String getStatus(){return status;}public Integer getDppNo(){return dppNo;}public String getCategory(){return category;}public String getSubCategory(){return subCategory;}public String getDisplayNameEn(){return displayNameEn;}public String getDisplayNameZh(){return displayNameZh;}public String getGranularity(){return granularity;}public Boolean getMandatoryStationary(){return mandatoryStationary;}public String getStaticDynamic(){return staticDynamic;}public String getStorageTarget(){return storageTarget;}public String getImplementationStatus(){return implementationStatus;}public String getExampleValue(){return exampleValue;}public String getRequirementNotes(){return requirementNotes;}public Boolean getVisibilityCustomized(){return visibilityCustomized;}public Instant getCreatedAt(){return createdAt;}public Instant getUpdatedAt(){return updatedAt;}
}
