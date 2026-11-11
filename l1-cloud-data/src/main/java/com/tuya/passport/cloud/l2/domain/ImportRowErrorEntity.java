package com.tuya.passport.cloud.l2.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "l2_import_row_error", indexes = @Index(name = "idx_l2_error_attempt", columnList = "org_code,job_id,attempt_no,row_no"))
public class ImportRowErrorEntity {
    @Id @Column(length = 36) private String id;
    @Column(name = "org_code", nullable = false, length = 64) private String orgCode;
    @Column(name = "job_id", nullable = false, length = 36) private String jobId;
    @Column(name = "attempt_no", nullable = false) private int attemptNo;
    @Column(name = "row_no", nullable = false) private int rowNo;
    @Column(name = "source_record_id", length = 128) private String sourceRecordId;
    @Column(name = "field_code", length = 100) private String fieldCode;
    @Column(name = "error_code", nullable = false, length = 80) private String errorCode;
    @Column(name = "safe_message", nullable = false, length = 500) private String safeMessage;
    @Column(name = "raw_value_hash", length = 64) private String rawValueHash;

    protected ImportRowErrorEntity() {}
    public ImportRowErrorEntity(String orgCode, String jobId, int attemptNo, int rowNo, String sourceRecordId, String fieldCode, String errorCode, String safeMessage, String rawValueHash) {
        this.id = java.util.UUID.randomUUID().toString(); this.orgCode = orgCode; this.jobId = jobId; this.attemptNo = attemptNo;
        this.rowNo = rowNo; this.sourceRecordId = sourceRecordId; this.fieldCode = fieldCode; this.errorCode = errorCode; this.safeMessage = safeMessage; this.rawValueHash = rawValueHash;
    }
    public String getId() { return id; } public String getOrgCode() { return orgCode; } public String getJobId() { return jobId; }
    public int getAttemptNo() { return attemptNo; } public int getRowNo() { return rowNo; } public String getSourceRecordId() { return sourceRecordId; }
    public String getFieldCode() { return fieldCode; } public String getErrorCode() { return errorCode; } public String getSafeMessage() { return safeMessage; }
    public String getRawValueHash() { return rawValueHash; }
}
