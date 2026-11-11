package com.tuya.passport.cloud.l3.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuya.passport.cloud.common.support.ApiException;
import com.tuya.passport.cloud.common.support.Hashing;
import com.tuya.passport.cloud.l2.domain.ImportJobEntity;
import com.tuya.passport.cloud.l2.domain.ImportJobStatus;
import com.tuya.passport.cloud.l2.domain.ImportRowEntity;
import com.tuya.passport.cloud.l2.domain.ImportRowStatus;
import com.tuya.passport.cloud.l2.repository.ImportJobRepository;
import com.tuya.passport.cloud.l2.repository.ImportRowRepository;
import com.tuya.passport.cloud.l3.domain.PromotionItemEntity;
import com.tuya.passport.cloud.l3.domain.PromotionItemStatus;
import com.tuya.passport.cloud.l3.domain.PromotionRunEntity;
import com.tuya.passport.cloud.l3.domain.PromotionRunStatus;
import com.tuya.passport.cloud.l3.repository.PromotionItemRepository;
import com.tuya.passport.cloud.l3.repository.PromotionRunRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportPromotionService {
 private final ImportJobRepository jobs; private final ImportRowRepository rows; private final PromotionRunRepository runs; private final PromotionItemRepository items; private final PromotionRowProcessor processor; private final Hashing hashing; private final ObjectMapper mapper;
 public ImportPromotionService(ImportJobRepository jobs,ImportRowRepository rows,PromotionRunRepository runs,PromotionItemRepository items,PromotionRowProcessor processor,Hashing hashing,ObjectMapper mapper){this.jobs=jobs;this.rows=rows;this.runs=runs;this.items=items;this.processor=processor;this.hashing=hashing;this.mapper=mapper;}
 @Transactional
 public PromotionResult promote(String org,String user,String jobId){
  ImportJobEntity job=jobs.findByOrgCodeAndId(org,jobId).orElseThrow(()->ApiException.notFound("L3_NOT_FOUND","Import job not found"));
  if(job.getStatus()!=ImportJobStatus.COMPLETED&&job.getStatus()!=ImportJobStatus.PARTIAL)throw ApiException.conflict("L3_JOB_NOT_PROMOTABLE","Only COMPLETED or PARTIAL import jobs can be promoted");
  PromotionRunEntity last=runs.findTopByOrgCodeAndJobIdAndAttemptNoOrderByRunNoDesc(org,jobId,job.getAttemptNo()).orElse(null);
  if(last!=null&&last.getStatus()==PromotionRunStatus.COMPLETED)return result(last);
  int runNo=last==null?1:last.getRunNo()+1; PromotionRunEntity run=runs.save(new PromotionRunEntity(org,jobId,job.getAttemptNo(),runNo,user));
  List<ImportRowEntity> accepted=rows.findByOrgCodeAndJobIdAndAttemptNo(org,jobId,job.getAttemptNo()).stream().filter(r->r.getStatus()==ImportRowStatus.ACCEPTED).toList();
  int promoted=0,duplicated=0,conflicted=0,failed=0;
  for(ImportRowEntity row:accepted){
   PromotionRowProcessor.ItemOutcome outcome;
   try{outcome=processor.process(org,jobId,job.getAttemptNo(),row.getRowNo());}
   catch(Exception ex){outcome=new PromotionRowProcessor.ItemOutcome(PromotionItemStatus.FAILED,null,null,"L3_ROW_PROCESSING_FAILED","The staging row could not be promoted");}
   String detailJson=null;try{if(outcome.detail()!=null)detailJson=mapper.writeValueAsString(outcome.detail());}catch(Exception ignored){}
   items.save(new PromotionItemEntity(org,run.getId(),row.getRowNo(),job.getEntityType(),outcome.status(),outcome.targetType(),outcome.targetId(),outcome.errorCode(),outcome.errorMessage(),detailJson));
   switch(outcome.status()){case PROMOTED->promoted++;case DUPLICATE->duplicated++;case CONFLICT->conflicted++;case FAILED->failed++;}
  }
  run.finish(accepted.size(),promoted,duplicated,conflicted,failed);runs.save(run);return result(run);
 }
 @Transactional(readOnly=true)
 public PromotionResult latest(String org,String jobId){
  if(jobs.findByOrgCodeAndId(org,jobId).isEmpty())throw ApiException.notFound("L3_NOT_FOUND","Import job not found");
  PromotionRunEntity run=runs.findTopByOrgCodeAndJobIdAndAttemptNoOrderByRunNoDesc(org,jobId,jobs.findByOrgCodeAndId(org,jobId).orElseThrow().getAttemptNo()).orElseThrow(()->ApiException.notFound("L3_PROMOTION_NOT_FOUND","No promotion run found"));return result(run);
 }
 private PromotionResult result(PromotionRunEntity run){List<PromotionItemView> views=items.findByOrgCodeAndRunIdOrderByRowNo(run.getOrgCode(),run.getId()).stream().map(i->{Object detail=null;try{if(i.getDetailJson()!=null)detail=mapper.readValue(i.getDetailJson(),Object.class);}catch(Exception ignored){}return new PromotionItemView(i.getRowNo(),i.getEntityType().name(),i.getStatus().name(),i.getTargetType(),i.getTargetId(),i.getErrorCode(),i.getErrorMessage(),detail);}).toList();return new PromotionResult(run.getId(),run.getStatus().name(),run.getTotalRows(),run.getPromoted(),run.getDuplicated(),run.getConflicted(),run.getFailed(),views);}
 public record PromotionItemView(int rowNo,String entityType,String status,String targetType,String targetId,String errorCode,String errorMessage,Object detail){}
 public record PromotionResult(String runId,String status,int totalRows,int promoted,int duplicated,int conflicted,int failed,List<PromotionItemView> items){}
}
