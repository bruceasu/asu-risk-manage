package me.asu.ta.casemanagement.recommendation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import me.asu.ta.casemanagement.model.CaseRecommendedAction;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.risk.model.RiskLevel;
import me.asu.ta.risk.model.RiskScoreResult;
import me.asu.ta.rule.model.RuleEngineResult;
import org.springframework.stereotype.Component;

@Component
public class CaseRecommendationBuilder {
    public List<CaseRecommendedAction> build(
            long caseId,
            AccountFeatureSnapshot snapshot,
            RuleEngineResult ruleEngineResult,
            RiskScoreResult riskScoreResult) {
        List<CaseRecommendedAction> actions = new ArrayList<>();
        Instant createdAt = riskScoreResult.generatedAt();
        RiskLevel level = riskScoreResult.riskLevel();

        if (level == RiskLevel.CRITICAL) {
            actions.add(action(caseId, "FREEZE_ACCOUNT", "Critical risk score requires immediate account restriction.", createdAt));
            actions.add(action(caseId, "MANUAL_REVIEW", "Critical case should be escalated to an investigator immediately.", createdAt));
        } else if (level == RiskLevel.HIGH) {
            actions.add(action(caseId, "HOLD_WITHDRAWAL", "High-risk case should pause withdrawals until reviewed.", createdAt));
            actions.add(action(caseId, "STEP_UP_VERIFICATION", "Require enhanced verification before allowing sensitive actions.", createdAt));
        } else if (level == RiskLevel.MEDIUM) {
            actions.add(action(caseId, "MONITOR_ACCOUNT", "Medium-risk case should stay under enhanced monitoring.", createdAt));
        } else {
            actions.add(action(caseId, "RETAIN_FOR_AUDIT", "Low-risk case is retained for audit traceability.", createdAt));
        }

        if (Boolean.TRUE.equals(snapshot.securityChangeBeforeWithdrawFlag24h())) {
            actions.add(action(caseId, "REVIEW_SECURITY_CHANGES", "Security changes occurred close to withdrawal activity.", createdAt));
        }
        if ((snapshot.sharedDeviceAccounts7d() != null && snapshot.sharedDeviceAccounts7d() >= 5)
                || ruleEngineResult.reasonCodes().contains("GRAPH_SHARED_DEVICE_CLUSTER")) {
            actions.add(action(caseId, "INVESTIGATE_LINKED_ACCOUNTS", "Shared-device exposure indicates potentially linked suspicious accounts.", createdAt));
        }
        return actions.stream().distinct().toList();
    }

    private CaseRecommendedAction action(long caseId, String actionCode, String actionReason, Instant createdAt) {
        return new CaseRecommendedAction(0L, caseId, actionCode, actionReason, createdAt);
    }
}
