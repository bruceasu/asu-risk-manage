
-- 1. investigation_case
-- 主案件表。
CREATE TABLE investigation_case (
    case_id                  BIGSERIAL PRIMARY KEY,
    account_id               VARCHAR(64) NOT NULL,
    case_status              VARCHAR(64) NOT NULL,
    risk_score               DOUBLE PRECISION NOT NULL,
    risk_level               VARCHAR(32) NOT NULL,
    profile_name             VARCHAR(64) NOT NULL,
    top_reason_codes         TEXT,
    feature_version          INT NOT NULL,
    evaluation_mode          VARCHAR(32) NOT NULL,
    created_at               TIMESTAMP NOT NULL,
    updated_at               TIMESTAMP NOT NULL
);
-- 索引：
CREATE INDEX idx_investigation_case_account
ON investigation_case(account_id);
CREATE INDEX idx_investigation_case_status
ON investigation_case(case_status);
CREATE INDEX idx_investigation_case_risk_level
ON investigation_case(risk_level);

-- 2. case_risk_summary
-- 详细风险摘要表。
CREATE TABLE case_risk_summary (
    case_id                  BIGINT PRIMARY KEY,
    score_breakdown_json     TEXT NOT NULL,
    rule_score               DOUBLE PRECISION,
    graph_score              DOUBLE PRECISION,
    anomaly_score            DOUBLE PRECISION,
    behavior_score           DOUBLE PRECISION,
    created_at               TIMESTAMP NOT NULL
);

-- 3. case_feature_summary
-- 关键特征摘要表。
CREATE TABLE case_feature_summary (
    case_id                                  BIGINT PRIMARY KEY,
    account_age_days                         INT,
    high_risk_ip_login_count_24h             INT,
    login_failure_rate_24h                   DOUBLE PRECISION,
    new_device_login_count_7d                INT,
    withdraw_after_deposit_delay_avg_24h     DOUBLE PRECISION,
    shared_device_accounts_7d                INT,
    security_change_before_withdraw_flag_24h BOOLEAN,
    graph_cluster_size_30d                   INT,
    risk_neighbor_count_30d                  INT,
    anomaly_score_last                       DOUBLE PRECISION,
    created_at                               TIMESTAMP NOT NULL
);

-- 4. case_rule_hit
-- 案件关联规则命中表。
CREATE TABLE case_rule_hit (
    case_rule_hit_id         BIGSERIAL PRIMARY KEY,
    case_id                  BIGINT NOT NULL,
    rule_code                VARCHAR(128) NOT NULL,
    rule_version             INT NOT NULL,
    severity                 VARCHAR(32) NOT NULL,
    score                    INT NOT NULL,
    reason_code              VARCHAR(128) NOT NULL,
    message                  TEXT,
    evidence_json            TEXT,
    created_at               TIMESTAMP NOT NULL
);
-- 索引：
CREATE INDEX idx_case_rule_hit_case
ON case_rule_hit(case_id);

-- 5. case_graph_summary
CREATE TABLE case_graph_summary (
    case_id                  BIGINT PRIMARY KEY,
    graph_score              DOUBLE PRECISION,
    graph_cluster_size       INT,
    risk_neighbor_count      INT,
    shared_device_accounts   INT,
    shared_bank_accounts     INT,
    created_at               TIMESTAMP NOT NULL
);

-- 6. case_timeline_event
-- 时间线表。
CREATE TABLE case_timeline_event (
    timeline_event_id        BIGSERIAL PRIMARY KEY,
    case_id                  BIGINT NOT NULL,
    event_time               TIMESTAMP NOT NULL,
    event_type               VARCHAR(64) NOT NULL,
    title                    VARCHAR(256) NOT NULL,
    description              TEXT,
    evidence_json            TEXT,
    created_at               TIMESTAMP NOT NULL
);
-- 索引：
CREATE INDEX idx_case_timeline_event_case_time
ON case_timeline_event(case_id, event_time ASC);

-- 7. case_recommended_action
-- 建议动作表。
CREATE TABLE case_recommended_action (
    case_action_id           BIGSERIAL PRIMARY KEY,
    case_id                  BIGINT NOT NULL,
    action_code              VARCHAR(64) NOT NULL,
    action_reason            TEXT,
    created_at               TIMESTAMP NOT NULL
);

-- 8. case_generation_job
-- 批量案件生成任务表。
CREATE TABLE case_generation_job (
    job_id                   BIGSERIAL PRIMARY KEY,
    job_type                 VARCHAR(32) NOT NULL,
    started_at               TIMESTAMP NOT NULL,
    finished_at              TIMESTAMP,
    status                   VARCHAR(32) NOT NULL,
    target_account_count     INT,
    processed_account_count  INT,
    failed_account_count     INT,
    error_message            TEXT
);

