package me.asu.ta.rule.engine;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.asu.ta.rule.api.FraudRule;
import org.springframework.stereotype.Component;

@Component
public class RuleRegistry {
    private final Map<String, FraudRule> rulesByCode;

    public RuleRegistry(List<FraudRule> rules) {
        Map<String, FraudRule> indexedRules = new LinkedHashMap<>();
        for (FraudRule rule : rules) {
            indexedRules.put(rule.ruleCode(), rule);
        }
        this.rulesByCode = Map.copyOf(indexedRules);
    }

    public Optional<FraudRule> findRule(String ruleCode) {
        return Optional.ofNullable(rulesByCode.get(ruleCode));
    }

    public Collection<FraudRule> getRegisteredRules() {
        return rulesByCode.values();
    }
}
