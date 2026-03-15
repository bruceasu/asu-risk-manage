package me.asu.ta.rule.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.asu.ta.rule.model.RuleVersion;
import me.asu.ta.rule.repository.RuleVersionRepository;
import org.springframework.stereotype.Service;

@Service
public class RuleVersionService {
    private final RuleVersionRepository ruleVersionRepository;

    public RuleVersionService(RuleVersionRepository ruleVersionRepository) {
        this.ruleVersionRepository = ruleVersionRepository;
    }

    public List<RuleVersion> findByRuleCode(String ruleCode) {
        return ruleVersionRepository.findByRuleCode(ruleCode);
    }

    public Map<String, RuleVersion> findEffectiveVersions(Instant asOfTime) {
        Map<String, RuleVersion> effectiveVersions = new LinkedHashMap<>();
        for (RuleVersion version : ruleVersionRepository.findEffectiveVersions(asOfTime)) {
            effectiveVersions.putIfAbsent(version.ruleCode(), version);
        }
        return effectiveVersions;
    }
}
