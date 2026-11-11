package com.tuya.passport.cloud.l3.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="l3_battery_model", uniqueConstraints=@UniqueConstraint(name="uk_l3_model_identity", columnNames={"org_code","issuer_code","model_code"}))
public class BatteryModelEntity {
    @Id @Column(length=36) private String id;
    @Column(name="org_code",nullable=false,length=64) private String orgCode;
    @Column(name="issuer_code",nullable=false,length=64) private String issuerCode;
    @Column(name="model_code",nullable=false,length=128) private String modelCode;
    @Column(name="manufacturer_name",length=255) private String manufacturerName;
    @Column(name="battery_category",length=32) private String batteryCategory;
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    @Column(name="sn_product_code",length=8) private String snProductCode;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    protected BatteryModelEntity() {}
    public BatteryModelEntity(String org,String issuer,String code,String manufacturer,String category){id=UUID.randomUUID().toString();orgCode=org;issuerCode=issuer;modelCode=code;manufacturerName=manufacturer;batteryCategory=category;createdAt=Instant.now();updatedAt=createdAt;}
    public void update(String manufacturer,String category){manufacturerName=manufacturer;batteryCategory=category;updatedAt=Instant.now();}
    public void setSnProductCode(String code){this.snProductCode=code;this.updatedAt=Instant.now();}
    public String getId(){return id;} public String getOrgCode(){return orgCode;} public String getIssuerCode(){return issuerCode;} public String getModelCode(){return modelCode;} public String getManufacturerName(){return manufacturerName;} public String getBatteryCategory(){return batteryCategory;} public String getSnProductCode(){return snProductCode;}
}
