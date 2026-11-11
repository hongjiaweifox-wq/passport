package com.tuya.passport.cloud.l2.service;

import com.tuya.passport.cloud.l2.domain.ImportJobStatus;
import com.tuya.passport.cloud.l2.repository.ImportJobRepository;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StartupRecovery implements ApplicationRunner {
    private final ImportJobRepository jobs;
    public StartupRecovery(ImportJobRepository jobs) { this.jobs=jobs; }
    @Override @Transactional public void run(ApplicationArguments args) {
        List<ImportJobStatus> processing=List.of(ImportJobStatus.SCANNING,ImportJobStatus.PARSING,ImportJobStatus.VALIDATING,ImportJobStatus.PUBLISHING);
        for(var job:jobs.findByStatusIn(processing)) job.fail("L2_PROCESS_INTERRUPTED","服务重启时任务尚未完成，可重试");
    }
}
