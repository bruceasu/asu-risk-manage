package me.asu.ta.ai.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.asu.ta.ai.model.InvestigationReport;
import me.asu.ta.casemanagement.model.InvestigationCaseBundle;
import org.springframework.stereotype.Service;

@Service
public class AiReportFacade {
    private final AiReportService aiReportService;

    public AiReportFacade(AiReportService aiReportService) {
        this.aiReportService = aiReportService;
    }

    public InvestigationReport generateReport(long caseId) {
        return aiReportService.generateReport(caseId);
    }

    public InvestigationReport generateReportForCase(InvestigationCaseBundle caseBundle) {
        return aiReportService.generateReportForCase(caseBundle);
    }

    public Map<Long, InvestigationReport> generateBatchReports(List<Long> caseIds) {
        return aiReportService.generateBatchReports(caseIds);
    }

    public Optional<InvestigationReport> findLatestReportByCaseId(long caseId) {
        return aiReportService.findLatestReportByCaseId(caseId);
    }
}
