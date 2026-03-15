package me.asu.ta.rule.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import me.asu.ta.rule.model.params.RuleParameters;

public record RuleConfig(
        String ruleCode,
        int version,
        int scoreWeight,
        RuleSeverity severity,
        Map<String, Object> parameters,
        RuleParameters typedParameters,
        Instant effectiveFrom,
        Instant effectiveTo
) {
    public RuleConfig {
        Objects.requireNonNull(ruleCode, "ruleCode");
        Objects.requireNonNull(severity, "severity");
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        Objects.requireNonNull(effectiveFrom, "effectiveFrom");
    }

    public <T extends RuleParameters> Optional<T> typedParameters(Class<T> type) {
        return type.isInstance(typedParameters) ? Optional.of(type.cast(typedParameters)) : Optional.empty();
    }
}
