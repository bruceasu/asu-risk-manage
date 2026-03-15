package me.asu.ta.rule.service;

/**
 * Raised when parameter_json cannot be safely parsed into a validated typed rule config.
 */
public class RuleParameterParsingException extends IllegalArgumentException {
    public RuleParameterParsingException(String message) {
        super(message);
    }

    public RuleParameterParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
