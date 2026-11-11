package com.tuya.passport.cloud.l2.service;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class AsyncImportJobDispatcher implements ImportJobDispatcher {
    private final ImportJobProcessor processor;
    public AsyncImportJobDispatcher(ImportJobProcessor processor) { this.processor=processor; }
    @Override @Async("importExecutor") public void dispatch(String orgCode, String jobId) { processor.process(orgCode,jobId); }
}
