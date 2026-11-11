package com.tuya.passport.cloud.l2.support;

import com.tuya.passport.cloud.l2.domain.ImportJobEntity;
import com.tuya.passport.cloud.l2.domain.ImportRowEntity;
import com.tuya.passport.cloud.l2.domain.ImportRowErrorEntity;
import com.tuya.passport.cloud.l2.api.dto.ImportJobDetail;
import com.tuya.passport.cloud.l2.api.dto.ImportJobListItem;
import com.tuya.passport.cloud.l2.api.dto.ImportRowDto;
import com.tuya.passport.cloud.l2.api.dto.ImportRowErrorDto;
import java.util.List;
import java.util.function.Function;

public final class DtoMapper {
    private DtoMapper() {}
    public static ImportJobListItem list(ImportJobEntity j) { return new ImportJobListItem(j.getId(), j.getOrgCode(), j.getSourceCode(), j.getSourceName(), j.getEntityType().name(), j.getSchemaVersion(), j.getStatus().name(), j.getFileName(), j.getFileSize(), j.getTotalRows(), j.getAcceptedRows(), j.getRejectedRows(), j.getDuplicateRows(), j.getAttemptNo(), j.getFailureCode(), j.getCreatedAt(), j.getUpdatedAt()); }
    public static ImportJobDetail detail(ImportJobEntity j, List<ImportRowErrorEntity> errors) { return new ImportJobDetail(j.getId(), j.getOrgCode(), j.getSourceCode(), j.getSourceName(), j.getEntityType().name(), j.getSchemaVersion(), j.getStatus().name(), j.getFileName(), j.getFileSize(), j.getFileSha256(), j.isScanPassed(), j.getTotalRows(), j.getAcceptedRows(), j.getRejectedRows(), j.getDuplicateRows(), j.getAttemptNo(), j.getFailureCode(), j.getFailureMessage(), j.getDuplicateOfJobId(), j.getCreatedBy(), j.getCreatedAt(), j.getStartedAt(), j.getCompletedAt(), j.getUpdatedAt(), errors.stream().map(DtoMapper::error).toList()); }
    public static ImportRowDto row(ImportRowEntity r) { return new ImportRowDto(r.getRowNo(), r.getSourceRecordId(), r.getRecordHash(), r.getCanonicalJson(), r.getStatus().name()); }
    public static ImportRowErrorDto error(ImportRowErrorEntity e) { return new ImportRowErrorDto(e.getRowNo(), e.getSourceRecordId(), e.getFieldCode(), e.getErrorCode(), e.getSafeMessage(), e.getRawValueHash()); }
}
