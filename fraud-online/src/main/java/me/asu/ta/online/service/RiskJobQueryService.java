package me.asu.ta.online.service;

import java.util.Optional;
import me.asu.ta.risk.model.RiskEvaluationJob;
import me.asu.ta.risk.repository.RiskEvaluationJobRepository;
import org.springframework.stereotype.Service;

@Service
public class RiskJobQueryService {
    private final RiskEvaluationJobRepository riskEvaluationJobRepository;

    public RiskJobQueryService(RiskEvaluationJobRepository riskEvaluationJobRepository) {
        this.riskEvaluationJobRepository = riskEvaluationJobRepository;
    }

    public Optional<RiskEvaluationJob> getLatestJob() {
        return riskEvaluationJobRepository.findLatest();
    }

    public Optional<RiskEvaluationJob> getJob(long jobId) {
        return riskEvaluationJobRepository.findById(jobId);
    }
}
