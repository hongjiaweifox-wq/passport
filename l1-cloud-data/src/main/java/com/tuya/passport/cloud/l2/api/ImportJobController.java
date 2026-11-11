package com.tuya.passport.cloud.l2.api;

import com.tuya.passport.cloud.l2.api.dto.ImportJobCreated;
import com.tuya.passport.cloud.l2.api.dto.ImportJobDetail;
import com.tuya.passport.cloud.l2.api.dto.ImportJobListItem;
import com.tuya.passport.cloud.l2.api.dto.ImportJobSummary;
import com.tuya.passport.cloud.l2.api.dto.ImportRowDto;
import com.tuya.passport.cloud.l2.api.dto.ImportRowErrorDto;
import com.tuya.passport.cloud.l2.domain.EntityType;
import com.tuya.passport.cloud.l2.domain.ImportJobEntity;
import com.tuya.passport.cloud.l2.service.CsvFormulaNeutralizer;
import com.tuya.passport.cloud.l2.service.ImportJobService;
import com.tuya.passport.cloud.l2.support.ApiEnvelope;
import com.tuya.passport.cloud.l2.support.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/l2/import-jobs")
@Validated
public class ImportJobController {
    private final ImportJobService service; private final CsvFormulaNeutralizer csv; private final com.tuya.passport.cloud.l2.service.ImportJobDispatcher dispatcher;
    public ImportJobController(ImportJobService service,CsvFormulaNeutralizer csv,com.tuya.passport.cloud.l2.service.ImportJobDispatcher dispatcher){this.service=service;this.csv=csv;this.dispatcher=dispatcher;}
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiEnvelope<ImportJobCreated>> create(@RequestHeader(value="X-Org-Code",required=false) String org,
      @RequestHeader(value="X-User",required=false) String user,@RequestHeader(value="Idempotency-Key",required=false) String key,
      @RequestParam("file") MultipartFile file,@RequestParam("sourceCode") @NotBlank String sourceCode,
      @RequestParam("entityType") @NotBlank String entityType,@RequestParam(value="schemaVersion",defaultValue="dpp-schema/1.0") String schemaVersion,
      HttpServletRequest request){
        var result=service.upload(org,user,key,file,sourceCode,entityType,schemaVersion); ImportJobEntity job=result.job();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ApiEnvelope<>(new ImportJobCreated(job.getId(),job.getStatus().name(),"/api/v1/l2/import-jobs/"+job.getId(),result.reused(),job.getDuplicateOfJobId()),trace(request)));
    }
    @GetMapping public ApiEnvelope<PageResponse<ImportJobListItem>> list(@RequestHeader(value="X-Org-Code",required=false)String org,@RequestParam(required=false)String q,@RequestParam(required=false)String status,@RequestParam(required=false)String entityType,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size,HttpServletRequest req){return new ApiEnvelope<>(service.list(org,q,status,entityType,page,size),trace(req));}
    @GetMapping("/summary") public ApiEnvelope<ImportJobSummary> summary(@RequestHeader(value="X-Org-Code",required=false)String org,HttpServletRequest req){return new ApiEnvelope<>(service.summary(org),trace(req));}
    @GetMapping("/{id}") public ApiEnvelope<ImportJobDetail> detail(@RequestHeader(value="X-Org-Code",required=false)String org,@PathVariable String id,HttpServletRequest req){return new ApiEnvelope<>(service.detail(org,id),trace(req));}
    @GetMapping("/{id}/rows") public ApiEnvelope<PageResponse<ImportRowDto>> rows(@RequestHeader(value="X-Org-Code",required=false)String org,@PathVariable String id,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int size,HttpServletRequest req){return new ApiEnvelope<>(service.rows(org,id,page,size),trace(req));}
    @GetMapping("/{id}/errors") public ApiEnvelope<PageResponse<ImportRowErrorDto>> errors(@RequestHeader(value="X-Org-Code",required=false)String org,@PathVariable String id,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int size,HttpServletRequest req){return new ApiEnvelope<>(service.errors(org,id,page,size),trace(req));}
    @GetMapping("/{id}/errors.csv") public ResponseEntity<byte[]> errorCsv(@RequestHeader(value="X-Org-Code",required=false)String org,@PathVariable String id){byte[] body=csv.render(service.allErrors(org,id));return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv; charset=UTF-8")).header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("import-errors-"+id+".csv",StandardCharsets.UTF_8).build().toString()).body(body);}
    @PostMapping("/{id}/retry") public ResponseEntity<ApiEnvelope<ImportJobCreated>> retry(@RequestHeader(value="X-Org-Code",required=false)String org,@PathVariable String id,HttpServletRequest req){var job=service.retry(org,id);return ResponseEntity.accepted().body(new ApiEnvelope<>(new ImportJobCreated(job.getId(),job.getStatus().name(),"/api/v1/l2/import-jobs/"+job.getId(),false,job.getDuplicateOfJobId()),trace(req)));}
    private static String trace(HttpServletRequest req){Object id=req.getAttribute("traceId");return id==null?"":id.toString();}
}
