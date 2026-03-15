insert into ai_prompt_template (
    template_code,
    version,
    template_type,
    template_content,
    is_active,
    created_at,
    updated_at,
    created_by,
    change_note
) values (
    'INVESTIGATION_REPORT_SYSTEM',
    1,
    'SYSTEM',
    'You are a fraud investigation analyst. Produce a concise, auditable investigation report. Do not invent facts. Use only the supplied case input. Output valid JSON only.',
    true,
    current_timestamp,
    current_timestamp,
    'fraud-ai-init',
    'Initial system prompt'
);

insert into ai_prompt_template (
    template_code,
    version,
    template_type,
    template_content,
    is_active,
    created_at,
    updated_at,
    created_by,
    change_note
) values (
    'INVESTIGATION_REPORT_FORMAT',
    1,
    'REPORT_FORMAT',
    'Return a JSON object with these fields only: reportTitle, executiveSummary, keyRiskIndicators, behaviorAnalysis, relationshipAnalysis, timelineObservations, possibleRiskPatterns, recommendations.',
    true,
    current_timestamp,
    current_timestamp,
    'fraud-ai-init',
    'Initial report format prompt'
);

insert into ai_prompt_template (
    template_code,
    version,
    template_type,
    template_content,
    is_active,
    created_at,
    updated_at,
    created_by,
    change_note
) values (
    'INVESTIGATION_CASE_RENDERER',
    1,
    'CASE_RENDERER',
    'caseId={{caseId}}
accountId={{accountId}}
riskLevel={{riskLevel}}
riskScore={{riskScore}}
topReasonCodes={{topReasonCodes}}
riskSummary={{riskSummary}}
featureSummary={{featureSummary}}
ruleHits={{ruleHits}}
graphSummary={{graphSummary}}
timeline={{timeline}}
recommendedActions={{recommendedActions}}',
    true,
    current_timestamp,
    current_timestamp,
    'fraud-ai-init',
    'Initial case renderer prompt'
);
