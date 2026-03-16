
-- 1 ai_prompt_template
-- Prompt 模板表。
CREATE TABLE ai_prompt_template (
    template_code            VARCHAR(128) NOT NULL,
    version                  INT NOT NULL,
    template_type            VARCHAR(64) NOT NULL,
    template_content         TEXT NOT NULL,
    is_active                BOOLEAN NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMP NOT NULL,
    updated_at               TIMESTAMP NOT NULL,
    created_by               VARCHAR(128),
    change_note              TEXT,
    PRIMARY KEY(template_code, version)
);
-- template_type 示例：
--     • SYSTEM
--     • REPORT_FORMAT
--     • CASE_RENDERER

-- 2 ai_generation_job
-- 批量报告生成任务表。
CREATE TABLE ai_generation_job (
    job_id                   BIGSERIAL PRIMARY KEY,
    job_type                 VARCHAR(32) NOT NULL,
    started_at               TIMESTAMP NOT NULL,
    finished_at              TIMESTAMP,
    status                   VARCHAR(32) NOT NULL,
    target_case_count        INT,
    processed_case_count     INT,
    failed_case_count        INT,
    error_message            TEXT
);

-- 3 ai_generation_request_log
-- 生成请求审计表。
CREATE TABLE ai_generation_request_log (
    request_id               BIGSERIAL PRIMARY KEY,
    case_id                  BIGINT NOT NULL,
    template_code            VARCHAR(128) NOT NULL,
    template_version         INT NOT NULL,
    model_name               VARCHAR(128) NOT NULL,
    request_payload          TEXT NOT NULL,
    requested_at             TIMESTAMP NOT NULL,
    status                   VARCHAR(32) NOT NULL,
    error_message            TEXT
);

-- 4 investigation_report
-- 报告主表。
CREATE TABLE investigation_report (
    report_id                BIGSERIAL PRIMARY KEY,
    case_id                  BIGINT NOT NULL,
    report_status            VARCHAR(32) NOT NULL,
    report_title             VARCHAR(256),
    executive_summary        TEXT,
    key_risk_indicators      TEXT,
    behavior_analysis        TEXT,
    relationship_analysis    TEXT,
    timeline_observations    TEXT,
    possible_risk_patterns   TEXT,
    recommendations          TEXT,
    model_name               VARCHAR(128) NOT NULL,
    template_code            VARCHAR(128) NOT NULL,
    template_version         INT NOT NULL,
    generated_at             TIMESTAMP NOT NULL,
    raw_response             TEXT
);
-- 索引：
CREATE INDEX idx_investigation_report_case
ON investigation_report(case_id);
CREATE INDEX idx_investigation_report_generated
ON investigation_report(generated_at DESC);

-- 5 ai_generation_retry_log
-- 可选，但生产中很有用。
CREATE TABLE ai_generation_retry_log (
    retry_id                 BIGSERIAL PRIMARY KEY,
    request_id               BIGINT NOT NULL,
    retry_time               TIMESTAMP NOT NULL,
    retry_count              INT NOT NULL,
    status                   VARCHAR(32) NOT NULL,
    error_message            TEXT
);
