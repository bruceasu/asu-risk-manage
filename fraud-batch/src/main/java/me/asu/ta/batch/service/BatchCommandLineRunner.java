package me.asu.ta.batch.service;

import me.asu.ta.batch.model.BatchRunSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BatchCommandLineRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(BatchCommandLineRunner.class);
    private final BatchOrchestratorService batchOrchestratorService;

    public BatchCommandLineRunner(BatchOrchestratorService batchOrchestratorService) {
        this.batchOrchestratorService = batchOrchestratorService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("job")) {
            return;
        }
        String value = args.getOptionValues("job").getFirst();
        BatchRunSummary summary = batchOrchestratorService.run(BatchJobType.from(value));
        log.info("Batch job {} finished with {} stages", summary.jobName(), summary.stages().size());
    }
}
