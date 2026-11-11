package com.tuya.passport.cloud.l4.api;

import com.tuya.passport.cloud.common.api.ApiEnvelope;
import com.tuya.passport.cloud.common.api.PageResponse;
import com.tuya.passport.cloud.common.support.ActorContext;
import com.tuya.passport.cloud.l4.domain.*;
import com.tuya.passport.cloud.l4.repository.*;
import com.tuya.passport.cloud.l4.service.FieldCoverageService;
import com.tuya.passport.cloud.l4.service.FieldDefinitionService;
import com.tuya.passport.cloud.l4.service.ReviewService;
import com.tuya.passport.cloud.l4.service.ValidationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/l4")
public class L4Controller {
 private final ActorContext actors;private final FieldDefinitionService definitions;private final FieldCoverageService coverage;private final ValidationService validation;private final ReviewService reviews;private final ReviewTaskRepository reviewRepo;private final ValidationRunRepository runs;private final ValidationIssueRepository issues;private final PublicationCandidateRepository candidates;
 public L4Controller(ActorContext actors,FieldDefinitionService definitions,FieldCoverageService coverage,ValidationService validation,ReviewService reviews,ReviewTaskRepository reviewRepo,ValidationRunRepository runs,ValidationIssueRepository issues,PublicationCandidateRepository candidates){this.actors=actors;this.definitions=definitions;this.coverage=coverage;this.validation=validation;this.reviews=reviews;this.reviewRepo=reviewRepo;this.runs=runs;this.issues=issues;this.candidates=candidates;}
 @GetMapping("/summary") public ApiEnvelope<Map<String,Long>> summary(@RequestHeader(value="X-Org-Code",required=false)String org,HttpServletRequest req){var a=actors.actor(org,null,null);return env(Map.of("pendingReview",reviewRepo.countByOrgCodeAndStatus(a.orgCode(),ReviewStatus.PENDING_REVIEW),"approved",reviewRepo.countByOrgCodeAndStatus(a.orgCode(),ReviewStatus.APPROVED),"validationFail",runs.countByOrgCodeAndStatus(a.orgCode(),ValidationStatus.FAIL),"candidateReady",candidates.countByOrgCodeAndStatus(a.orgCode(),CandidateStatus.READY)),req);}
 @GetMapping("/field-coverage") public ApiEnvelope<FieldCoverageService.CoverageView> fieldCoverage(@RequestHeader(value="X-Org-Code",required=false)String org,@RequestParam(required=false)String subjectType,@RequestParam(required=false)String subjectId,HttpServletRequest req){var a=actors.actor(org,null,null);return env(coverage.coverage(a.orgCode(),subjectType,subjectId),req);}
 @GetMapping("/field-definitions") public ApiEnvelope<List<FieldDefinitionEntity>> fields(@RequestHeader(value="X-Org-Code",required=false)String org,@RequestParam(defaultValue="dpp-schema/1.0")String schemaVersion,HttpServletRequest req){var a=actors.actor(org,null,null);return env(definitions.ensureDefaults(a.orgCode()),req);}
 @PatchMapping("/field-definitions/{id}") public ApiEnvelope<FieldDefinitionEntity> updateFieldVisibility(@RequestHeader(value="X-Org-Code",required=false)String org,@RequestHeader(value="X-User",required=false)String user,@RequestHeader(value="X-Role",required=false)String role,@PathVariable String id,@Valid @RequestBody UpdateFieldVisibility body,HttpServletRequest req){var a=actors.actor(org,user,role);actors.requireAnyRole(a,"DATA_EDITOR","COMPLIANCE_MAKER","COMPLIANCE_CHECKER","ADMIN");return env(definitions.updateVisibility(a.orgCode(),id,body.visibilityClass()),req);}
 @PostMapping("/records/{subjectType}/{subjectId}/validate") public ApiEnvelope<ValidationService.ValidationResult> validate(@RequestHeader(value="X-Org-Code",required=false)String org,@RequestHeader(value="X-User",required=false)String user,@RequestHeader(value="X-Role",required=false)String role,@PathVariable String subjectType,@PathVariable String subjectId,HttpServletRequest req){var a=actors.actor(org,user,role);actors.requireAnyRole(a,"DATA_EDITOR","COMPLIANCE_MAKER","COMPLIANCE_CHECKER","PASSPORT_OPERATOR","ADMIN");return env(validation.validate(a.orgCode(),subjectType,subjectId),req);}
 @GetMapping("/validation-runs") public ApiEnvelope<PageResponse<ValidationRunEntity>> validationRuns(@RequestHeader(value="X-Org-Code",required=false)String org,@RequestParam(required=false)String subjectType,@RequestParam(required=false)String subjectId,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size,HttpServletRequest req){var a=actors.actor(org,null,null);var pageable=PageRequest.of(Math.max(0,page-1),Math.min(100,size),Sort.by(Sort.Direction.DESC,"finishedAt"));var result=subjectType==null||subjectType.isBlank()||subjectId==null||subjectId.isBlank()?runs.findByOrgCode(a.orgCode(),pageable):runs.findByOrgCodeAndSubjectTypeAndSubjectId(a.orgCode(),subjectType,subjectId,pageable);return env(PageResponse.from(result),req);}
 @GetMapping("/validation-runs/{id}") public ApiEnvelope<ValidationDetail> validationRun(@RequestHeader(value="X-Org-Code",required=false)String org,@PathVariable String id,HttpServletRequest req){var a=actors.actor(org,null,null);var run=runs.findByOrgCodeAndId(a.orgCode(),id).orElseThrow(()->com.tuya.passport.cloud.common.support.ApiException.notFound("GOV_NOT_FOUND","Validation run not found"));return env(new ValidationDetail(run,issues.findByOrgCodeAndValidationRunIdOrderByCreatedAt(a.orgCode(),id)),req);}
 @PostMapping("/reviews") @ResponseStatus(HttpStatus.CREATED) public ApiEnvelope<ReviewTaskEntity> submit(@RequestHeader(value="X-Org-Code",required=false)String org,@RequestHeader(value="X-User",required=false)String user,@RequestHeader(value="X-Role",required=false)String role,@Valid @RequestBody SubmitReview body,HttpServletRequest req){var a=actors.actor(org,user,role);actors.requireAnyRole(a,"DATA_EDITOR","COMPLIANCE_MAKER","ADMIN");return env(reviews.submit(a.orgCode(),a.userId(),body.subjectType(),body.subjectId()),req);}
 @GetMapping("/reviews") public ApiEnvelope<PageResponse<ReviewTaskEntity>> reviewList(@RequestHeader(value="X-Org-Code",required=false)String org,@RequestParam(required=false)String status,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size,HttpServletRequest req){var a=actors.actor(org,null,null);var pageable=PageRequest.of(Math.max(0,page-1),Math.min(size,100),Sort.by(Sort.Direction.DESC,"createdAt"));return env(PageResponse.from(status==null||status.isBlank()?reviewRepo.findByOrgCode(a.orgCode(),pageable):reviewRepo.findByOrgCodeAndStatus(a.orgCode(),ReviewStatus.valueOf(status),pageable)),req);}
 @GetMapping("/reviews/{id}") public ApiEnvelope<ReviewTaskEntity> review(@RequestHeader(value="X-Org-Code",required=false)String org,@PathVariable String id,HttpServletRequest req){var a=actors.actor(org,null,null);return env(reviewRepo.findByOrgCodeAndId(a.orgCode(),id).orElseThrow(()->com.tuya.passport.cloud.common.support.ApiException.notFound("GOV_NOT_FOUND","Review task not found")),req);}
 @PostMapping("/reviews/{id}:decision") public ApiEnvelope<ReviewTaskEntity> decision(@RequestHeader(value="X-Org-Code",required=false)String org,@RequestHeader(value="X-User",required=false)String user,@RequestHeader(value="X-Role",required=false)String role,@PathVariable String id,@Valid @RequestBody Decision body,HttpServletRequest req){var a=actors.actor(org,user,role);actors.requireAnyRole(a,"COMPLIANCE_CHECKER","ADMIN");return env(reviews.decide(a.orgCode(),a.userId(),id,body.decision(),body.reason()),req);}
 @GetMapping("/records/{subjectType}/{subjectId}/publication-candidate") public ApiEnvelope<PublicationCandidateEntity> candidate(@RequestHeader(value="X-Org-Code",required=false)String org,@PathVariable String subjectType,@PathVariable String subjectId,HttpServletRequest req){var a=actors.actor(org,null,null);return env(reviews.readyCandidate(a.orgCode(),subjectType,subjectId),req);}
 private static <T>ApiEnvelope<T> env(T data,HttpServletRequest req){Object trace=req.getAttribute("traceId");return new ApiEnvelope<>(data,trace==null?"":trace.toString());}
 public record SubmitReview(@NotBlank String subjectType,@NotBlank String subjectId){}
 public record UpdateFieldVisibility(@NotNull VisibilityClass visibilityClass){}
 public record Decision(@NotBlank String decision,@NotBlank String reason){}
 public record ValidationDetail(ValidationRunEntity run,List<ValidationIssueEntity> issues){}
}
