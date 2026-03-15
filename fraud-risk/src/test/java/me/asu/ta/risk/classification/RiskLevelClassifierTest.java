package me.asu.ta.risk.classification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import me.asu.ta.risk.model.RiskLevel;
import org.junit.jupiter.api.Test;

class RiskLevelClassifierTest {
    private final RiskLevelClassifier classifier = new RiskLevelClassifier();

    @Test
    void shouldClassifyRiskLevelsByThreshold() {
        assertEquals(RiskLevel.LOW, classifier.classify(0.0d));
        assertEquals(RiskLevel.LOW, classifier.classify(29.99d));
        assertEquals(RiskLevel.MEDIUM, classifier.classify(30.0d));
        assertEquals(RiskLevel.MEDIUM, classifier.classify(59.99d));
        assertEquals(RiskLevel.HIGH, classifier.classify(60.0d));
        assertEquals(RiskLevel.HIGH, classifier.classify(79.99d));
        assertEquals(RiskLevel.CRITICAL, classifier.classify(80.0d));
        assertEquals(RiskLevel.CRITICAL, classifier.classify(100.0d));
    }
}
