package com.tuya.passport.cloud.l2.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "l2_import_row", indexes = @Index(name = "idx_l2_row_attempt", columnList = "org_code,job_id,attempt_no,row_no"),
       uniqueConstraints = @UniqueConstraint(name = "uk_l2_row_attempt_no", columnNames = {"org_code", "job_id", "attempt_no", "row_no"}))
public class ImportRowEntity {
    @Id @Column(length = 36) private String id;
    @Column(name = "org_code", nullable = false, length = 64) private String orgCode;
    @Column(name = "job_id", nullable = false, length = 36) private String jobId;
    @Column(name = "attempt_no", nullable = false) private int attemptNo;
    @Column(name = "row_no", nullable = false) private int rowNo;
    @Column(name = "source_record_id", length = 128) private String sourceRecordId;
    @Column(name = "record_hash", length = 64) private String recordHash;
    @Lob @Column(name = "canonical_json") private String canonicalJson;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private ImportRowStatus status;

    protected ImportRowEntity() {}
    public ImportRowEntity(String orgCode, String jobId, int attemptNo, int rowNo, String sourceRecordId, String recordHash, String canonicalJson, ImportRowStatus status) {
        this.id = java.util.UUID.randomUUID().toString(); this.orgCode = orgCode; this.jobId = jobId; this.attemptNo = attemptNo;
        this.rowNo = rowNo; this.sourceRecordId = sourceRecordId; this.recordHash = recordHash; this.canonicalJson = canonicalJson; this.status = status;
    }
    public String getId() { return id; } public String getOrgCode() { return orgCode; } public String getJobId() { return jobId; }
    public int getAttemptNo() { return attemptNo; } public int getRowNo() { return rowNo; } public String getSourceRecordId() { return sourceRecordId; }
    public String getRecordHash() { return recordHash; } public String getCanonicalJson() { return canonicalJson; } public ImportRowStatus getStatus() { return status; }
}
