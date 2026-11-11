package com.tuya.passport.cloud.l2.service;

import com.tuya.passport.cloud.l2.domain.EntityType;
import com.tuya.passport.cloud.l2.domain.ImportJobEntity;
import com.tuya.passport.cloud.l2.domain.ImportJobStatus;
import com.tuya.passport.cloud.l2.repository.ImportJobRepository;
import com.tuya.passport.cloud.l2.repository.ImportRowErrorRepository;
import com.tuya.passport.cloud.l2.repository.ImportRowRepository;
import com.tuya.passport.cloud.l2.support.ApiException;
import com.tuya.passport.cloud.l2.support.PageResponse;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportJobService {
    private static final String SCHEMA = "dpp-schema/1.0";
    private final ImportJobRepository jobs; private final ImportRowRepository rows; private final ImportRowErrorRepository errors;
    private final L1SourceClient sourceClient; private final ImportJobDispatcher dispatcher; private final JobStateService states; private final Path storageRoot;
    private final long maxBytes;
    public ImportJobService(ImportJobRepository jobs, ImportRowRepository rows, ImportRowErrorRepository errors,
            L1SourceClient sourceClient, ImportJobDispatcher dispatcher, JobStateService states,
            @Value("${l2.storage.root:./data/uploads}") String root,
            @Value("${l2.import.max-file-bytes:20971520}") long maxBytes) {
        this.jobs=jobs;this.rows=rows;this.errors=errors;this.sourceClient=sourceClient;this.dispatcher=dispatcher;this.states=states;
        this.storageRoot=Path.of(root).toAbsolutePath().normalize();this.maxBytes=maxBytes;
    }
    public ImportJobService.UploadResult upload(String rawOrg, String user, String key, MultipartFile file, String sourceCode, String entityTypeRaw, String schemaVersion) {
        String org=normalizeOrg(rawOrg); validateMeta(sourceCode,entityTypeRaw,schemaVersion);
        if(file==null||file.isEmpty()) throw new ApiException("INGEST_EMPTY_FILE","请选择非空 CSV 文件",HttpStatus.BAD_REQUEST);
        if(file.getSize()>maxBytes) throw new ApiException("L2_FILE_TOO_LARGE","文件不能超过 20MB",HttpStatus.PAYLOAD_TOO_LARGE);
        String filename=Path.of(file.getOriginalFilename()==null?"upload.csv":file.getOriginalFilename()).getFileName().toString();
        if(!filename.toLowerCase(Locale.ROOT).endsWith(".csv")) throw new ApiException("INGEST_UNSUPPORTED_FORMAT","仅支持 CSV 文件",HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        String contentType=file.getContentType();
        if(contentType!=null&&!List.of("text/csv","application/csv","application/vnd.ms-excel","application/octet-stream").contains(contentType.toLowerCase(Locale.ROOT)))
            throw new ApiException("INGEST_UNSUPPORTED_FORMAT","文件 MIME 类型不受支持",HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        EntityType type=parseType(entityTypeRaw); String schema=schemaVersion==null||schemaVersion.isBlank()?SCHEMA:schemaVersion.trim();
        if(!SCHEMA.equals(schema)) throw new ApiException("INGEST_SCHEMA_MISMATCH","当前仅支持 schema dpp-schema/1.0",HttpStatus.BAD_REQUEST);
        L1Source source=sourceClient.requireByCode(org,sourceCode);
        if(!"ACTIVE".equalsIgnoreCase(source.status())) throw new ApiException("INGEST_SOURCE_INACTIVE","该数据源当前未启用",HttpStatus.BAD_REQUEST);
        if(source.templateTypes()==null||!source.templateTypes().contains(type.name())) throw new ApiException("INGEST_ENTITY_NOT_ALLOWED","该数据源未授权此模板类型",HttpStatus.BAD_REQUEST);
        String idem=key==null||key.isBlank()?UUID.randomUUID().toString():key.trim();
        if(idem.length()>128) throw new ApiException("L2_VALIDATION_ERROR","Idempotency-Key 最长 128 字符",HttpStatus.BAD_REQUEST);
        String sha; Path finalPath=null; Path tempPath=null; String id=UUID.randomUUID().toString();
        try {
            Files.createDirectories(storageRoot.resolve(org).resolve(id)); finalPath=storageRoot.resolve(org).resolve(id).resolve("source.csv"); tempPath=finalPath.resolveSibling("source.csv.tmp");
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            try(InputStream in=file.getInputStream();var out=Files.newOutputStream(tempPath)){
                byte[] buffer=new byte[8192];int n;long total=0;byte[] prefix=new byte[1024];int prefixCount=0;
                while((n=in.read(buffer))!=-1){total+=n;if(total>maxBytes)throw new ApiException("L2_FILE_TOO_LARGE","文件不能超过 20MB",HttpStatus.PAYLOAD_TOO_LARGE); if(prefixCount<prefix.length){int copy=Math.min(n,prefix.length-prefixCount);System.arraycopy(buffer,0,prefix,prefixCount,copy);prefixCount+=copy;}digest.update(buffer,0,n);out.write(buffer,0,n);}
                rejectMagic(prefix,prefixCount);
            }
            sha=HexFormat.of().formatHex(digest.digest()); Files.move(tempPath,finalPath,StandardCopyOption.ATOMIC_MOVE);
            var existing=jobs.findByOrgCodeAndIdempotencyKey(org,idem);
            if(existing.isPresent()) { cleanup(finalPath); ImportJobEntity e=existing.get(); if(!e.getFileSha256().equals(sha)) throw conflict(); return new UploadResult(e,true); }
            String duplicate=jobs.findFirstByOrgCodeAndFileSha256AndIdNotOrderByCreatedAtDesc(org,sha,id).map(ImportJobEntity::getId).orElse(null);
            ImportJobEntity job=new ImportJobEntity(id,org,source.sourceCode(),source.sourceName(),type,schema,filename,file.getSize(),sha,finalPath.toString(),idem,user==null||user.isBlank()?"demo-user":user,duplicate);
            try { jobs.saveAndFlush(job); }
            catch(DataIntegrityViolationException conflict){ cleanup(finalPath); return racedIdempotency(org,idem,sha); }
            dispatcher.dispatch(org,id); return new UploadResult(job,false);
        } catch(ApiException ex){cleanup(tempPath);cleanup(finalPath);throw ex;}
        catch(Exception ex){cleanup(tempPath);cleanup(finalPath);throw new ApiException("L2_STORAGE_ERROR","无法保存上传文件",HttpStatus.INTERNAL_SERVER_ERROR);}
    }
    private UploadResult racedIdempotency(String org,String key,String sha) {
        var e=jobs.findByOrgCodeAndIdempotencyKey(org,key).orElseThrow(ImportJobService::conflict);
        if(!e.getFileSha256().equals(sha)) throw conflict(); return new UploadResult(e,true);
    }
    public ImportJobEntity requireJob(String org,String id){return states.job(normalizeOrg(org),id);}
    public PageResponse<com.tuya.passport.cloud.l2.api.dto.ImportJobListItem> list(String org,String q,String status,String entityType,int page,int size){
        checkPage(page,size); ImportJobStatus s=status==null||status.isBlank()?null:parseStatus(status); EntityType t=entityType==null||entityType.isBlank()?null:parseType(entityType);
        var p=jobs.search(normalizeOrg(org),q,s,t,PageRequest.of(page-1,size)); return new PageResponse<>(p.getContent().stream().map(com.tuya.passport.cloud.l2.support.DtoMapper::list).toList(),p.getTotalElements(),page,size);
    }
    public com.tuya.passport.cloud.l2.api.dto.ImportJobSummary summary(String rawOrg){String org=normalizeOrg(rawOrg);return new com.tuya.passport.cloud.l2.api.dto.ImportJobSummary(jobs.countByOrgCode(org),jobs.countByOrgCodeAndStatusIn(org,List.of(ImportJobStatus.UPLOADED,ImportJobStatus.SCANNING,ImportJobStatus.PARSING,ImportJobStatus.VALIDATING,ImportJobStatus.PUBLISHING)),jobs.countByOrgCodeAndStatus(org,ImportJobStatus.COMPLETED),jobs.countByOrgCodeAndStatus(org,ImportJobStatus.PARTIAL),jobs.countByOrgCodeAndStatus(org,ImportJobStatus.FAILED));}
    public com.tuya.passport.cloud.l2.api.dto.ImportJobDetail detail(String rawOrg,String id){var job=requireJob(rawOrg,id);return com.tuya.passport.cloud.l2.support.DtoMapper.detail(job,states.topErrors(job.getOrgCode(),id,job.getAttemptNo()));}
    public PageResponse<com.tuya.passport.cloud.l2.api.dto.ImportRowDto> rows(String rawOrg,String id,int page,int size){checkPage(page,size);var job=requireJob(rawOrg,id);var p=states.rows(job.getOrgCode(),id,job.getAttemptNo(),PageRequest.of(page-1,size));return new PageResponse<>(p.getContent().stream().map(com.tuya.passport.cloud.l2.support.DtoMapper::row).toList(),p.getTotalElements(),page,size);}
    public PageResponse<com.tuya.passport.cloud.l2.api.dto.ImportRowErrorDto> errors(String rawOrg,String id,int page,int size){checkPage(page,size);var job=requireJob(rawOrg,id);var p=states.errors(job.getOrgCode(),id,job.getAttemptNo(),PageRequest.of(page-1,size));return new PageResponse<>(p.getContent().stream().map(com.tuya.passport.cloud.l2.support.DtoMapper::error).toList(),p.getTotalElements(),page,size);}
    public List<com.tuya.passport.cloud.l2.domain.ImportRowErrorEntity> allErrors(String rawOrg,String id){var job=requireJob(rawOrg,id);return states.allErrors(job.getOrgCode(),id,job.getAttemptNo());}
    public ImportJobEntity retry(String rawOrg,String id){String org=normalizeOrg(rawOrg);var job=states.beginRetry(org,id);dispatcher.dispatch(org,id);return job;}
    public static String normalizeOrg(String raw){String org=raw==null||raw.isBlank()?"DEMO-ORG":raw.trim().toUpperCase(Locale.ROOT);if(!org.matches("[A-Z0-9_-]{1,64}"))throw new ApiException("L2_VALIDATION_ERROR","Invalid X-Org-Code",HttpStatus.BAD_REQUEST);return org;}
    private static void validateMeta(String source,String type,String schema){if(source==null||source.isBlank())throw new ApiException("L2_VALIDATION_ERROR","sourceCode is required",HttpStatus.BAD_REQUEST);parseType(type);}
    private static EntityType parseType(String raw){try{return EntityType.valueOf(raw==null?"":raw.trim().toUpperCase(Locale.ROOT));}catch(Exception ex){throw new ApiException("L2_VALIDATION_ERROR","Unsupported entityType",HttpStatus.BAD_REQUEST);}}
    private static ImportJobStatus parseStatus(String raw){try{return ImportJobStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));}catch(Exception ex){throw new ApiException("L2_VALIDATION_ERROR","Unsupported status",HttpStatus.BAD_REQUEST);}}
    private static void checkPage(int page,int size){if(page<1||size<1||size>100)throw new ApiException("L2_VALIDATION_ERROR","page must be >= 1 and size must be 1..100",HttpStatus.BAD_REQUEST);}
    private static void rejectMagic(byte[] b,int length){if(length==0)throw new ApiException("INGEST_EMPTY_FILE","请选择非空 CSV 文件",HttpStatus.BAD_REQUEST);String prefix=new String(b,0,length,java.nio.charset.StandardCharsets.UTF_8).stripLeading().toLowerCase(Locale.ROOT);if(b[0]==0||prefix.startsWith("pk")||prefix.startsWith("%pdf")||prefix.startsWith("<html")||prefix.startsWith("<!doctype html"))throw new ApiException("INGEST_UNSUPPORTED_FORMAT","文件内容不是 CSV 文本",HttpStatus.UNSUPPORTED_MEDIA_TYPE);for(int i=0;i<length;i++)if(b[i]==0)throw new ApiException("INGEST_UNSUPPORTED_FORMAT","CSV 文件包含二进制内容",HttpStatus.UNSUPPORTED_MEDIA_TYPE);}
    private static ApiException conflict(){return new ApiException("L2_IDEMPOTENCY_CONFLICT","相同 Idempotency-Key 已用于不同文件",HttpStatus.CONFLICT);}
    private static void cleanup(Path p){if(p!=null)try{Files.deleteIfExists(p);}catch(Exception ignored){}}
    public record UploadResult(ImportJobEntity job,boolean reused){}
}
