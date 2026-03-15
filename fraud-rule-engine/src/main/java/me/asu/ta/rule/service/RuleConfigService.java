package me.asu.ta.rule.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import me.asu.ta.rule.model.RuleConfig;
import me.asu.ta.rule.model.RuleConfigReloadLog;
import me.asu.ta.rule.model.RuleDefinition;
import me.asu.ta.rule.model.RuleVersion;
import me.asu.ta.rule.repository.RuleConfigReloadLogRepository;
import me.asu.ta.rule.repository.RuleDefinitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class RuleConfigService {
    private static final Logger log = LoggerFactory.getLogger(RuleConfigService.class);

    private final RuleDefinitionRepository ruleDefinitionRepository;
    private final RuleVersionService ruleVersionService;
    private final RuleConfigReloadLogRepository reloadLogRepository;
    private final RuleParameterParser ruleParameterParser;
    private final ConcurrentHashMap<String, RuleConfig> configCache = new ConcurrentHashMap<>();

    public RuleConfigService(
            RuleDefinitionRepository ruleDefinitionRepository,
            RuleVersionService ruleVersionService,
            RuleConfigReloadLogRepository reloadLogRepository,
            RuleParameterParser ruleParameterParser) {
        this.ruleDefinitionRepository = ruleDefinitionRepository;
        this.ruleVersionService = ruleVersionService;
        this.reloadLogRepository = reloadLogRepository;
        this.ruleParameterParser = ruleParameterParser;
    }

    public Optional<RuleConfig> getConfig(String ruleCode, Instant asOfTime) {
        RuleConfig cached = configCache.get(ruleCode);
        if (cached != null && isEffective(cached, asOfTime)) {
            return Optional.of(cached);
        }
        reload();
        RuleConfig reloaded = configCache.get(ruleCode);
        return reloaded != null && isEffective(reloaded, asOfTime) ? Optional.of(reloaded) : Optional.empty();
    }

    public Map<String, RuleConfig> getActiveConfigs(Instant asOfTime) {
        if (configCache.isEmpty()) {
            reload();
        }
        Map<String, RuleConfig> activeConfigs = new LinkedHashMap<>();
        for (Map.Entry<String, RuleConfig> entry : configCache.entrySet()) {
            if (isEffective(entry.getValue(), asOfTime)) {
                activeConfigs.put(entry.getKey(), entry.getValue());
            }
        }
        return activeConfigs;
    }

    @Scheduled(fixedDelay = 30000)
    public void reload() {
        Instant reloadTime = Instant.now();
        try {
            Map<String, RuleDefinition> definitions = new LinkedHashMap<>();
            for (RuleDefinition definition : ruleDefinitionRepository.findActiveRules()) {
                definitions.put(definition.ruleCode(), definition);
            }
            Map<String, RuleVersion> versions = ruleVersionService.findEffectiveVersions(reloadTime);
            Map<String, RuleConfig> refreshed = new LinkedHashMap<>();
            int failedRuleCount = 0;
            for (Map.Entry<String, RuleVersion> entry : versions.entrySet()) {
                RuleDefinition definition = definitions.get(entry.getKey());
                if (definition == null) {
                    continue;
                }
                try {
                    RuleParameterParser.ParsedRuleParameters parsedParameters = ruleParameterParser.parse(
                            definition.ruleCode(),
                            definition.category(),
                            entry.getValue().parameterJson());
                    refreshed.put(
                            entry.getKey(),
                            new RuleConfig(
                                    definition.ruleCode(),
                                    entry.getValue().version(),
                                    entry.getValue().scoreWeight(),
                                    definition.severity(),
                                    parsedParameters.rawParameters(),
                                    parsedParameters.typedParameters(),
                                    entry.getValue().effectiveFrom(),
                                    entry.getValue().effectiveTo()));
                } catch (RuntimeException ex) {
                    failedRuleCount++;
                    log.warn("Skipping malformed rule config for {}: {}", entry.getKey(), ex.getMessage());
                }
            }
            configCache.clear();
            configCache.putAll(refreshed);
            String status = failedRuleCount == 0 ? "SUCCESS" : "PARTIAL_SUCCESS";
            String errorMessage = failedRuleCount == 0 ? null : "Malformed configs skipped: " + failedRuleCount;
            reloadLogRepository.save(new RuleConfigReloadLog(0L, reloadTime, status, refreshed.size(), errorMessage));
        } catch (RuntimeException ex) {
            log.warn("Failed to reload rule configs: {}", ex.getMessage());
            reloadLogRepository.save(new RuleConfigReloadLog(0L, reloadTime, "FAILED", 0, ex.getMessage()));
        }
    }

    private boolean isEffective(RuleConfig config, Instant asOfTime) {
        return !config.effectiveFrom().isAfter(asOfTime)
                && (config.effectiveTo() == null || config.effectiveTo().isAfter(asOfTime));
    }
}
