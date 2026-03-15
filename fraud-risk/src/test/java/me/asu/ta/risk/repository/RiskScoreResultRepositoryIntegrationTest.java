package me.asu.ta.risk.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.sql.DataSource;
import me.asu.ta.risk.RiskTestSupport;
import me.asu.ta.risk.model.RiskLevel;
import me.asu.ta.risk.model.RiskScoreResult;
import me.asu.ta.risk.model.ScoreBreakdown;
import me.asu.ta.rule.model.EvaluationMode;
import org.junit.jupiter.api.Test;

class RiskScoreResultRepositoryIntegrationTest {
    @Test
    void shouldPersistAndReadLatestRiskScoreResult() throws Exception {
        DataSource dataSource = RiskTestSupport.createDataSource();
        RiskScoreResultRepository repository = RiskTestSupport.riskScoreResultRepository(dataSource);

        RiskScoreResult saved = repository.saveRiskScoreResult(new RiskScoreResult(
                0L,
                "acct-risk-4",
                81.5d,
                RiskLevel.CRITICAL,
                "DEFAULT",
                3,
                RiskTestSupport.FIXED_TIME,
                EvaluationMode.REALTIME,
                java.util.List.of("RULE_TAKEOVER", "ML_ANOMALY_HIGH"),
                new ScoreBreakdown(88.0d, 70.0d, 92.0d, 40.0d, 81.5d, "DEFAULT")));

        assertTrue(saved.scoreId() > 0L);

        RiskScoreResult loaded = repository.findLatestRiskScoreByAccountId("acct-risk-4").orElseThrow();
        assertEquals(saved.scoreId(), loaded.scoreId());
        assertEquals(81.5d, loaded.riskScore());
        assertEquals(RiskLevel.CRITICAL, loaded.riskLevel());
        assertEquals(java.util.List.of("RULE_TAKEOVER", "ML_ANOMALY_HIGH"), loaded.topReasonCodes());
        assertEquals(81.5d, loaded.scoreBreakdown().finalScore());
    }
}
