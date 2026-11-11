package com.tuya.passport.cloud.l2.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "l2_import_job", indexes = {
    @Index(name = "idx_l2_job_org_created", columnList = "org_code,created_at"),
    @Index(name = "idx_l2_job_org_sha", columnList = "org_code,file_sha256")
}, uniqueConstraints = @UniqueConstraint(name = "uk_l2_job_org_idem", columnNames = {"org_code", "idempotency_key"}))
public class ImportJobEntity {
    @Id @Column(length = 36) private String id;
    @Column(name = "org_code", nullable = false, length = 64) private String orgCode;
    @Column(name = "source_code", nullable = false, length = 64) private String sourceCode;
    @Column(name = "source_name", nullable = false, length = 120) private String sourceName;
    @Enumerated(EnumType.STRING) @Column(name = "entity_type", nullable = false, length = 32) private EntityType entityType;
    @Column(name = "schema_version", nullable = false, length = 32) private String schemaVersion;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private ImportJobStatus status;
    @Column(name = "file_name", nullable = false, length = 255) private String fileName;
    @Column(name = "file_size", nullable = false) private long fileSize;
    @Column(name = "file_sha256", nullable = false, length = 64) private String fileSha256;
    @Column(name = "storage_path", nullable = false, length = 1024) private String storagePath;
    @Column(name = "scan_passed", nullable = false) private boolean scanPassed;
    @Column(name = "idempotency_key", nullable = false, length = 128) private String idempotencyKey;
    @Column(name = "duplicate_of_job_id", length = 36) private String duplicateOfJobId;
    @Column(name = "total_rows", nullable = false) private int totalRows;
    @Column(name = "accepted_rows", nullable = false) private int acceptedRows;
    @Column(name = "rejected_rows", nullable = false) private int rejectedRows;
    @Column(name = "duplicate_rows", nullable = false) private int duplicateRows;
    @Column(name = "attempt_no", nullable = false) private int attemptNo;
    @Column(name = "failure_code", length = 80) private String failureCode;
    @Column(name = "failure_message", length = 500) private String failureMessage;
    @Column(name = "created_by", nullable = false, length = 128) private String createdBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "started_at") private Instant startedAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version @Column(nullable = false) private long version;

    protected ImportJobEntity() {}
    public ImportJobEntity(String id, String orgCode, String sourceCode, String sourceName, EntityType entityType,
            String schemaVersion, String fileName, long fileSize, String fileSha256, String storagePath,
            String idempotencyKey, String createdBy, String duplicateOfJobId) {
        this.id = id; this.orgCode = orgCode; this.sourceCode = sourceCode; this.sourceName = sourceName;
        this.entityType = entityType; this.schemaVersion = schemaVersion; this.fileName = fileName;
        this.fileSize = fileSize; this.fileSha256 = fileSha256; this.storagePath = storagePath;
        this.idempotencyKey = idempotencyKey; this.createdBy = createdBy; this.duplicateOfJobId = duplicateOfJobId;
        this.scanPassed = true; this.status = ImportJobStatus.UPLOADED; this.attemptNo = 1;
        this.createdAt = Instant.now(); this.updatedAt = this.createdAt;
    }
    public void changeStatus(ImportJobStatus next) { this.status = next; this.updatedAt = Instant.now(); if (next == ImportJobStatus.SCANNING && this.startedAt == null) this.startedAt = this.updatedAt; if (next.terminal()) this.completedAt = this.updatedAt; }
    public void setCounts(int total, int accepted, int rejected, int duplicate) { this.totalRows = total; this.acceptedRows = accepted; this.rejectedRows = rejected; this.duplicateRows = duplicate; this.updatedAt = Instant.now(); }
    public void fail(String code, String message) { this.failureCode = code; this.failureMessage = message; changeStatus(ImportJobStatus.FAILED); }
    public void beginRetry() { this.attemptNo++; this.totalRows = 0; this.acceptedRows = 0; this.rejectedRows = 0; this.duplicateRows = 0; this.failureCode = null; this.failureMessage = null; this.startedAt = null; this.completedAt = null; changeStatus(ImportJobStatus.UPLOADED); }
    public String getId() { return id; } public String getOrgCode() { return orgCode; } public String getSourceCode() { return sourceCode; }
    public String getSourceName() { return sourceName; } public EntityType getEntityType() { return entityType; } public String getSchemaVersion() { return schemaVersion; }
    public ImportJobStatus getStatus() { return status; } public String getFileName() { return fileName; } public long getFileSize() { return fileSize; }
    public String getFileSha256() { return fileSha256; } public String getStoragePath() { return storagePath; } public boolean isScanPassed() { return scanPassed; }
    public String getIdempotencyKey() { return idempotencyKey; } public String getDuplicateOfJobId() { return duplicateOfJobId; } public int getTotalRows() { return totalRows; }
    public int getAcceptedRows() { return acceptedRows; } public int getRejectedRows() { return rejectedRows; } public int getDuplicateRows() { return duplicateRows; }
    public int getAttemptNo() { return attemptNo; } public String getFailureCode() { return failureCode; } public String getFailureMessage() { return failureMessage; }
    public String getCreatedBy() { return createdBy; } public Instant getCreatedAt() { return createdAt; } public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; } public Instant getUpdatedAt() { return updatedAt; }
}
