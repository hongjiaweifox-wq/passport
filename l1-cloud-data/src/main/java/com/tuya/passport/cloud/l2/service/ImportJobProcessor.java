package com.tuya.passport.cloud.l2.service;

import com.tuya.passport.cloud.l2.domain.EntityType;
import com.tuya.passport.cloud.l2.domain.ImportJobEntity;
import com.tuya.passport.cloud.l2.domain.ImportJobStatus;
import com.tuya.passport.cloud.l2.domain.ImportRowEntity;
import com.tuya.passport.cloud.l2.domain.ImportRowErrorEntity;
import com.tuya.passport.cloud.l2.domain.ImportRowStatus;
import com.tuya.passport.cloud.l2.repository.ImportJobRepository;
import com.tuya.passport.cloud.l2.support.ApiException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ImportJobProcessor {
    private final JobStateService states; private final CsvImportParser parser; private final RowValidator validator; private final ImportJobRepository jobs;
    private final int maxRows; private final int batchSize; private final Map<String,Boolean> processing=new ConcurrentHashMap<>();
    public ImportJobProcessor(JobStateService states,CsvImportParser parser,RowValidator validator,ImportJobRepository jobs,
        @Value("${l2.import.max-rows:50000}")int maxRows,@Value("${l2.import.batch-size:500}")int batchSize){this.states=states;this.parser=parser;this.validator=validator;this.jobs=jobs;this.maxRows=maxRows;this.batchSize=batchSize;}
    public void process(String org,String id){
        if(processing.putIfAbsent(org+":"+id,true)!=null)return;
        try{
            ImportJobEntity job=jobs.findByOrgCodeAndId(org,id).orElseThrow();int attempt=job.getAttemptNo();
            states.transition(org,id,ImportJobStatus.SCANNING);
            EntityType type=job.getEntityType();Map<String,String> seen=new HashMap<>();
            List<ImportRowEntity> rowBatch=new ArrayList<>();List<ImportRowErrorEntity> errorBatch=new ArrayList<>();int[] counts=new int[4];int[] batchCount={0};
            states.transition(org,id,ImportJobStatus.PARSING);
            parser.parse(Path.of(job.getStoragePath()),type,maxRows,(rowNo,record)->{
                if(batchCount[0]++==0)states.transition(org,id,ImportJobStatus.VALIDATING);
                var valid=validator.validate(org,job.getSourceCode(),type,job.getSchemaVersion(),rowNo,record);counts[0]++;
                ImportRowStatus status;String recordId=valid.sourceRecordId();
                if(!valid.valid()){status=ImportRowStatus.REJECTED;counts[2]++;}
                else if(seen.containsKey(recordId)){
                    if(seen.get(recordId).equals(valid.recordHash())){status=ImportRowStatus.DUPLICATE;counts[3]++;}
                    else{status=ImportRowStatus.REJECTED;counts[2]++;errorBatch.add(new ImportRowErrorEntity(org,id,attempt,rowNo,recordId,"source_record_id","INGEST_DUPLICATE_KEY_CONFLICT","同一文件中 source_record_id 对应不同内容",valid.recordHash()));}
                }else{status=ImportRowStatus.ACCEPTED;counts[1]++;seen.put(recordId,valid.recordHash());}
                rowBatch.add(new ImportRowEntity(org,id,attempt,rowNo,recordId,valid.recordHash(),valid.canonicalJson(),status));
                for(var issue:valid.errors())errorBatch.add(new ImportRowErrorEntity(org,id,attempt,rowNo,recordId,issue.fieldCode(),issue.errorCode(),issue.safeMessage(),issue.rawValueHash()));
                if(rowBatch.size()>=batchSize)flush(org,id,attempt,rowBatch,errorBatch);
            });
            flush(org,id,attempt,rowBatch,errorBatch);
            if(batchCount[0]==0)states.transition(org,id,ImportJobStatus.VALIDATING);
            states.counts(org,id,counts[0],counts[1],counts[2],counts[3]);states.transition(org,id,ImportJobStatus.PUBLISHING);
            ImportJobStatus terminal=counts[2]==0?ImportJobStatus.COMPLETED:(counts[1]+counts[3]>0?ImportJobStatus.PARTIAL:ImportJobStatus.FAILED);
            if(terminal==ImportJobStatus.FAILED)states.fail(org,id,"INGEST_ALL_ROWS_REJECTED","所有数据行均未通过基础校验");else states.transition(org,id,terminal);
        }catch(ApiException ex){states.fail(org,id,ex.code(),ex.getMessage());}
        catch(Exception ex){states.fail(org,id,"L2_PROCESSING_ERROR","导入处理失败，请检查文件格式后重试");}
        finally{processing.remove(org+":"+id);}
    }
    private void flush(String org,String id,int attempt,List<ImportRowEntity> rowBatch,List<ImportRowErrorEntity> errorBatch){if(rowBatch.isEmpty()&&errorBatch.isEmpty())return;states.saveBatch(org,id,attempt,new ArrayList<>(rowBatch),new ArrayList<>(errorBatch));rowBatch.clear();errorBatch.clear();}
}
