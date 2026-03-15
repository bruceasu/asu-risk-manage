package me.asu.ta.rule.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import me.asu.ta.rule.model.RuleCategory;
import me.asu.ta.rule.model.params.CompositeRuleParams;
import me.asu.ta.rule.model.params.DeviceRuleParams;
import me.asu.ta.rule.model.params.GraphRuleParams;
import me.asu.ta.rule.model.params.LoginRuleParams;
import me.asu.ta.rule.model.params.RuleParameters;
import me.asu.ta.rule.model.params.SecurityRuleParams;
import me.asu.ta.rule.model.params.TransactionRuleParams;
import org.springframework.stereotype.Component;

@Component
public class RuleParameterParser {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public RuleParameterParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    public ParsedRuleParameters parse(String ruleCode, RuleCategory category, String parameterJson) {
        try {
            JsonNode root = objectMapper.readTree(parameterJson);
            if (root == null || !root.isObject()) {
                throw new RuleParameterParsingException("parameter_json for " + ruleCode + " must be a JSON object");
            }
            Map<String, Object> rawParameters = Map.copyOf(objectMapper.convertValue(root, MAP_TYPE));
            RuleParameters typedParameters = parseTypedParameters(ruleCode, category, root);
            return new ParsedRuleParameters(rawParameters, typedParameters);
        } catch (RuleParameterParsingException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuleParameterParsingException("Malformed parameter_json for " + ruleCode + ": " + ex.getMessage(), ex);
        }
    }

    private RuleParameters parseTypedParameters(String ruleCode, RuleCategory category, JsonNode root) {
        return switch (category) {
            case LOGIN -> convert(ruleCode, root, LoginRuleParams.class);
            case TRANSACTION -> convert(ruleCode, root, TransactionRuleParams.class);
            case DEVICE -> convert(ruleCode, root, DeviceRuleParams.class);
            case SECURITY -> convert(ruleCode, root, SecurityRuleParams.class);
            case GRAPH -> convert(ruleCode, root, GraphRuleParams.class);
            case COMPOSITE -> convert(ruleCode, root, CompositeRuleParams.class);
        };
    }

    private <T extends RuleParameters> T convert(String ruleCode, JsonNode root, Class<T> type) {
        try {
            JavaType javaType = objectMapper.getTypeFactory().constructType(type);
            return objectMapper.readerFor(javaType).readValue(root);
        } catch (Exception ex) {
            throw new RuleParameterParsingException("Invalid typed parameters for " + ruleCode + ": " + ex.getMessage(), ex);
        }
    }

    public record ParsedRuleParameters(
            Map<String, Object> rawParameters,
            RuleParameters typedParameters
    ) {
    }
}
