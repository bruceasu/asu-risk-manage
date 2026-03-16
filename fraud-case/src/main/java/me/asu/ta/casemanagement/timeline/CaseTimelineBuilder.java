package me.asu.ta.casemanagement.timeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.asu.ta.OfflineBehaviorContextKeys;
import me.asu.ta.casemanagement.model.CaseTimelineEvent;
import me.asu.ta.feature.model.AccountFeatureSnapshot;
import me.asu.ta.risk.model.RiskScoreResult;
import me.asu.ta.rule.model.RuleEngineResult;
import me.asu.ta.rule.model.RuleEvaluationResult;
import org.springframework.stereotype.Component;

@Component
public class CaseTimelineBuilder {
    private final ObjectMapper objectMapper;

    public CaseTimelineBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<CaseTimelineEvent> build(
            long caseId,
            AccountFeatureSnapshot snapshot,
            RuleEngineResult ruleEngineResult,
            RiskScoreResult riskScoreResult,
            Map<String, Object> contextSignals) {
        List<CaseTimelineEvent> timeline = new ArrayList<>();
        Instant createdAt = riskScoreResult.generatedAt();

        timeline.add(caseEvent(
                caseId,
                createdAt,
                createdAt,
                "CASE_CREATED",
                "Investigation case created",
                "Case opened from latest feature, rule, and risk evaluation results.",
                Map.of("riskLevel", riskScoreResult.riskLevel().name(), "riskScore", riskScoreResult.riskScore())));

        for (RuleEvaluationResult hit : ruleEngineResult.hits()) {
            timeline.add(caseEvent(
                    caseId,
                    createdAt,
                    createdAt,
                    "RULE_HIT",
                    "Rule hit: " + hit.ruleCode(),
                    hit.message(),
                    hit.evidence()));
        }

        if (Boolean.TRUE.equals(snapshot.securityChangeBeforeWithdrawFlag24h())) {
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("securityChangeBeforeWithdrawFlag24h", true);
            evidence.put("securityEventCount24h", snapshot.securityEventCount24h());
            timeline.add(caseEvent(
                    caseId,
                    eventTime(createdAt, -50),
                    createdAt,
                    "SECURITY_PATTERN",
                    "Security change before withdrawal",
                    "Feature snapshot indicates a security-sensitive change close to withdrawal activity.",
                    evidence));
        }

        if (Boolean.TRUE.equals(snapshot.rapidProfileChangeFlag24h())
                || intValue(snapshot.securityEventCount24h()) > 0) {
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("rapidProfileChangeFlag24h", Boolean.TRUE.equals(snapshot.rapidProfileChangeFlag24h()));
            evidence.put("securityEventCount24h", snapshot.securityEventCount24h());
            timeline.add(caseEvent(
                    caseId,
                    eventTime(createdAt, -55),
                    createdAt,
                    "PASSWORD_RESET",
                    "Recent password reset or profile change",
                    "Feature snapshot indicates recent password reset or security profile change activity.",
                    evidence));
        }

        if (snapshot.highRiskIpLoginCount24h() != null && snapshot.highRiskIpLoginCount24h() > 0) {
            timeline.add(caseEvent(
                    caseId,
                    eventTime(createdAt, -120),
                    createdAt,
                    "LOGIN_PATTERN",
                    "High-risk IP login activity",
                    "Feature snapshot shows logins from high-risk IP addresses in the last 24 hours.",
                    Map.of("highRiskIpLoginCount24h", snapshot.highRiskIpLoginCount24h())));
        }

        if (intValue(snapshot.newDeviceLoginCount7d()) > 0) {
            timeline.add(caseEvent(
                    caseId,
                    eventTime(createdAt, -110),
                    createdAt,
                    "NEW_DEVICE_LOGIN",
                    "Recent login from a new device",
                    "Feature snapshot indicates recent logins from devices not seen before for this account.",
                    Map.of(
                            "newDeviceLoginCount7d", snapshot.newDeviceLoginCount7d(),
                            "uniqueDeviceCount7d", snapshot.uniqueDeviceCount7d())));
        }

        if (intValue(snapshot.depositCount24h()) > 0 || doubleValue(snapshot.depositAmount24h()) > 0.0d) {
            timeline.add(caseEvent(
                    caseId,
                    eventTime(createdAt, -40),
                    createdAt,
                    "DEPOSIT_ACTIVITY",
                    "Recent deposit activity",
                    "Feature snapshot indicates recent deposit activity relevant to the investigation window.",
                    Map.of(
                            "depositCount24h", intValue(snapshot.depositCount24h()),
                            "depositAmount24h", doubleValue(snapshot.depositAmount24h()))));
        }

        if (intValue(snapshot.withdrawCount24h()) > 0 || doubleValue(snapshot.withdrawAmount24h()) > 0.0d) {
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("withdrawCount24h", intValue(snapshot.withdrawCount24h()));
            evidence.put("withdrawAmount24h", doubleValue(snapshot.withdrawAmount24h()));
            evidence.put("rapidWithdrawAfterDepositFlag24h", Boolean.TRUE.equals(snapshot.rapidWithdrawAfterDepositFlag24h()));
            evidence.put("withdrawAfterDepositDelayAvg24h", snapshot.withdrawAfterDepositDelayAvg24h());
            timeline.add(caseEvent(
                    caseId,
                    eventTime(createdAt, -30),
                    createdAt,
                    "WITHDRAWAL_ACTIVITY",
                    "Recent withdrawal activity",
                    "Feature snapshot indicates recent withdrawal activity within the evaluation window.",
                    evidence));
        }

        if (isHighValueTransfer(snapshot)) {
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("transactionCount24h", intValue(snapshot.transactionCount24h()));
            evidence.put("totalAmount24h", doubleValue(snapshot.totalAmount24h()));
            evidence.put("avgTransactionAmount24h", doubleValue(snapshot.avgTransactionAmount24h()));
            evidence.put("uniqueCounterpartyCount24h", intValue(snapshot.uniqueCounterpartyCount24h()));
            timeline.add(caseEvent(
                    caseId,
                    eventTime(createdAt, -20),
                    createdAt,
                    "HIGH_VALUE_TRANSFER",
                    "High-value transfer pattern detected",
                    "Feature snapshot indicates recent large-value transfer activity requiring investigation context.",
                    evidence));
        }

        double coordinatedTradingScore = doubleValue(contextSignals.get(OfflineBehaviorContextKeys.COORDINATED_TRADING_SCORE));
        int behaviorClusterSize = intValue(contextSignals.get(OfflineBehaviorContextKeys.BEHAVIOR_CLUSTER_SIZE));
        int similarAccountCount = intValue(contextSignals.get(OfflineBehaviorContextKeys.SIMILAR_ACCOUNT_COUNT));
        if (coordinatedTradingScore > 0.0d || behaviorClusterSize > 1 || similarAccountCount > 0) {
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put(OfflineBehaviorContextKeys.COORDINATED_TRADING_SCORE, coordinatedTradingScore);
            evidence.put(OfflineBehaviorContextKeys.BEHAVIOR_CLUSTER_SIZE, behaviorClusterSize);
            evidence.put(OfflineBehaviorContextKeys.SIMILAR_ACCOUNT_COUNT, similarAccountCount);
            evidence.put(OfflineBehaviorContextKeys.BEHAVIOR_MAX_SIMILARITY, doubleValue(contextSignals.get(OfflineBehaviorContextKeys.BEHAVIOR_MAX_SIMILARITY)));
            timeline.add(caseEvent(
                    caseId,
                    eventTime(createdAt, -10),
                    createdAt,
                    "BEHAVIOR_CLUSTER_SIGNAL",
                    "Offline behavior cluster signal",
                    "Offline analysis found behavior-level similarity or coordinated trading patterns around this account.",
                    evidence));
        }

        timeline.sort(Comparator.comparing(CaseTimelineEvent::eventTime).thenComparing(CaseTimelineEvent::createdAt));
        return timeline;
    }

    private CaseTimelineEvent caseEvent(
            long caseId,
            Instant eventTime,
            Instant createdAt,
            String eventType,
            String title,
            String description,
            Object evidence) {
        return new CaseTimelineEvent(
                0L,
                caseId,
                eventTime,
                eventType,
                title,
                description,
                toJson(evidence),
                createdAt);
    }

    private Instant eventTime(Instant baseTime, long minutesOffset) {
        return baseTime.plusSeconds(minutesOffset * 60L);
    }

    private boolean isHighValueTransfer(AccountFeatureSnapshot snapshot) {
        return doubleValue(snapshot.totalAmount24h()) >= 10000.0d
                || doubleValue(snapshot.avgTransactionAmount24h()) >= 5000.0d
                || intValue(snapshot.uniqueCounterpartyCount24h()) >= 8;
    }

    private int intValue(Object value) {
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private double doubleValue(Object value) {
        if (value instanceof Double doubleValue) {
            return doubleValue;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0d;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize timeline evidence", ex);
        }
    }
}
