package me.asu.ta.ai.parser;

public class InvestigationReportParseException extends RuntimeException {
    public InvestigationReportParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvestigationReportParseException(String message) {
        super(message);
    }
}
