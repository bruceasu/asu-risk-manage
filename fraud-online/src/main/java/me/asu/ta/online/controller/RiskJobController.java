package me.asu.ta.online.controller;

import me.asu.ta.online.service.RiskJobQueryService;
import me.asu.ta.risk.model.RiskEvaluationJob;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
public class RiskJobController {
    private final RiskJobQueryService riskJobQueryService;

    public RiskJobController(RiskJobQueryService riskJobQueryService) {
        this.riskJobQueryService = riskJobQueryService;
    }

    @GetMapping("/api/jobs/risk/latest")
    public RiskEvaluationJob getLatestRiskJob() {
        return riskJobQueryService.getLatestJob()
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No risk job found"));
    }

    @GetMapping("/api/jobs/risk/{jobId}")
    public RiskEvaluationJob getRiskJob(@PathVariable long jobId) {
        return riskJobQueryService.getJob(jobId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No risk job found for jobId " + jobId));
    }
}
