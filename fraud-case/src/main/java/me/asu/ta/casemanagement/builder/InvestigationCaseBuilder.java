package me.asu.ta.casemanagement.builder;

import java.time.Instant;
import java.util.List;
import me.asu.ta.casemanagement.model.CaseFeatureSummary;
import me.asu.ta.casemanagement.model.CaseGenerationRequest;
import me.asu.ta.casemanagement.model.CaseGraphSummary;
import me.asu.ta.casemanagement.model.CaseRiskSummary;
import me.asu.ta.casemanagement.model.CaseRuleHit;
import me.asu.ta.casemanagement.model.CaseStatus;
import me.asu.ta.casemanagement.model.InvestigationCase;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.risk.model.RiskScoreResult;
import me.asu.ta.rule.model.RuleEngineResult;
import org.springframework.stereotype.Component;

@Component
public class InvestigationCaseBuilder {
    private final RiskSummaryBuilder riskSummaryBuilder;
    private final FeatureSummaryBuilder featureSummaryBuilder;
    private final RuleSummaryBuilder ruleSummaryBuilder;
    private final GraphSummaryBuilder graphSummaryBuilder;

    public InvestigationCaseBuilder(
            RiskSummaryBuilder riskSummaryBuilder,
            FeatureSummaryBuilder featureSummaryBuilder,
            RuleSummaryBuilder ruleSummaryBuilder,
            GraphSummaryBuilder graphSummaryBuilder) {
        this.riskSummaryBuilder = riskSummaryBuilder;
        this.featureSummaryBuilder = featureSummaryBuilder;
        this.ruleSummaryBuilder = ruleSummaryBuilder;
        this.graphSummaryBuilder = graphSummaryBuilder;
    }

    public InvestigationCase buildCaseHeader(AccountFeatureSnapshot snapshot, RiskScoreResult riskScoreResult) {
        Instant now = riskScoreResult.generatedAt();
        return new InvestigationCase(
                0L,
                snapshot.accountId(),
                CaseStatus.OPEN,
                riskScoreResult.riskScore(),
                riskScoreResult.riskLevel(),
                riskScoreResult.profileName(),
                riskScoreResult.topReasonCodes(),
                snapshot.featureVersion(),
                riskScoreResult.evaluationMode(),
                now,
                now);
    }

    public CaseRiskSummary buildRiskSummary(long caseId, RiskScoreResult riskScoreResult) {
        return riskSummaryBuilder.build(caseId, riskScoreResult);
    }

    public CaseFeatureSummary buildFeatureSummary(long caseId, AccountFeatureSnapshot snapshot, Instant createdAt) {
        return featureSummaryBuilder.build(caseId, snapshot, createdAt);
    }

    public List<CaseRuleHit> buildRuleHits(long caseId, RuleEngineResult ruleEngineResult, Instant createdAt) {
        return ruleSummaryBuilder.build(caseId, ruleEngineResult, createdAt);
    }

    public CaseGraphSummary buildGraphSummary(long caseId, CaseGenerationRequest request, Instant createdAt) {
        return graphSummaryBuilder.build(caseId, request, createdAt);
    }
}
