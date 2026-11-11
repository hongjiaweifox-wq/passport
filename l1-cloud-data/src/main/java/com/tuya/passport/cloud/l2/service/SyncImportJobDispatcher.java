package com.tuya.passport.cloud.l2.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class SyncImportJobDispatcher implements ImportJobDispatcher {
    private final ImportJobProcessor processor;
    public SyncImportJobDispatcher(ImportJobProcessor processor) { this.processor=processor; }
    @Override public void dispatch(String orgCode, String jobId) { processor.process(orgCode,jobId); }
}
