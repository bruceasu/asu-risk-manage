已经确定的技术路线来收敛：
    • Java 25
    • Spring Boot 4.x
    • Maven
    • Spring JDBC
    • PostgreSQL
    • Python 只做 ML
    • fraud-ai 主要做 调查报告生成与解释层
    • 不把 AI 放在最终风控决策闭环里

fraud-ai 生产级设计
1. 模块定位
fraud-ai 不是做检测，也不是做最终判定。
它的职责是：
    • 将 InvestigationCase 转成适合 LLM 的 Prompt
    • 调用 LLM API
    • 解析输出
    • 保存报告
    • 支持重试、版本化、审计
一句话：
fraud-ai 是“结构化案件 → 可审阅调查报告”的生成层。

2. 核心原则
2.1 AI 只解释，不判案
AI 不直接决定：
    • 是否欺诈
    • 是否冻结
    • 是否封禁
AI 只做：
    • 摘要
    • 风险解释
    • 证据整理
    • 调查建议表述

2.2 Prompt 必须版本化
因为你后续一定会改：
    • 输出格式
    • 语气
    • 字段映射
    • 风险表述
所以 Prompt 不能散落在代码里，必须有：
    • 模板版本
    • 生效状态
    • 可审计记录

2.3 输入必须结构化
LLM 不应直接读取原始事件日志。
最佳输入是：
    • InvestigationCase
    • RiskSummary
    • RuleSummary
    • Timeline
    • RecommendedActions

2.4 输出必须可存档
报告生成后要保存：
    • 原始 prompt
    • 模型标识
    • 模板版本
    • 生成结果
    • 状态
    • 错误信息

3. 模块职责
建议模块：
fraud-ai
├── model
├── prompt
├── client
├── parser
├── repository
├── service
└── job

3.1 prompt
负责：
    • Prompt 模板管理
    • 占位符替换
    • Prompt 渲染

3.2 client
负责：
    • 调用 LLM API
    • 超时
    • 重试
    • 错误处理

3.3 parser
负责：
    • 解析 LLM 返回
    • 生成结构化报告对象
    • 容错处理

3.4 repository
负责：
    • 存 Prompt 模板
    • 存生成记录
    • 存报告结果

3.5 service
负责：
    • 从 InvestigationCase 生成 Prompt
    • 调用 LLM
    • 解析结果
    • 持久化

4. 输入输出设计
4.1 输入
核心输入对象：
InvestigationCase
包含：
    • accountId
    • RiskSummary
    • FeatureSummary
    • RuleSummary
    • GraphSummary
    • Timeline
    • RecommendedActions

4.2 输出
建议输出对象：
InvestigationReport
建议字段：
    • reportId
    • caseId
    • reportStatus
    • reportTitle
    • executiveSummary
    • keyRiskIndicators
    • behaviorAnalysis
    • relationshipAnalysis
    • timelineObservations
    • possibleRiskPatterns
    • recommendations
    • modelName
    • promptTemplateCode
    • promptTemplateVersion
    • generatedAt

5. 报告生成流程
InvestigationCase
    ↓
PromptTemplateService
    ↓
Rendered Prompt
    ↓
LlmClient
    ↓
Raw LLM Response
    ↓
InvestigationReportParser
    ↓
InvestigationReport
    ↓
DB Persistence

6. Prompt 设计策略
推荐一套模板分层：
6.1 system instruction template
定义 AI 的角色与边界。
6.2 report output format template
约束输出结构。
6.3 case rendering template
把 InvestigationCase 渲染成输入。
6.4 optional few-shot template
只有在确实需要稳定风格时再加。
第一版建议：
    • 先不用 few-shot
    • 先靠结构化输入和格式约束

7. fraud-ai 存储设计
下面给出推荐 schema。

7.1 ai_prompt_template
Prompt 模板表。
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
template_type 示例：
    • SYSTEM
    • REPORT_FORMAT
    • CASE_RENDERER

7.2 ai_generation_job
批量报告生成任务表。
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

7.3 ai_generation_request_log
生成请求审计表。
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

7.4 investigation_report
报告主表。
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
索引：
CREATE INDEX idx_investigation_report_case
ON investigation_report(case_id);
CREATE INDEX idx_investigation_report_generated
ON investigation_report(generated_at DESC);

7.5 ai_generation_retry_log
可选，但生产中很有用。
CREATE TABLE ai_generation_retry_log (
    retry_id                 BIGSERIAL PRIMARY KEY,
    request_id               BIGINT NOT NULL,
    retry_time               TIMESTAMP NOT NULL,
    retry_count              INT NOT NULL,
    status                   VARCHAR(32) NOT NULL,
    error_message            TEXT
);

8. Java 模型建议
8.1 PromptTemplate
public record PromptTemplate(
    String templateCode,
    int version,
    String templateType,
    String templateContent,
    boolean active
) {}

8.2 RenderedPrompt
public record RenderedPrompt(
    String templateCode,
    int templateVersion,
    String renderedContent
) {}

8.3 InvestigationReport
public record InvestigationReport(
    Long reportId,
    Long caseId,
    String reportStatus,
    String reportTitle,
    String executiveSummary,
    String keyRiskIndicators,
    String behaviorAnalysis,
    String relationshipAnalysis,
    String timelineObservations,
    String possibleRiskPatterns,
    String recommendations,
    String modelName,
    String templateCode,
    int templateVersion,
    Instant generatedAt,
    String rawResponse
) {}

8.4 AiGenerationRequestLog
public record AiGenerationRequestLog(
    Long requestId,
    Long caseId,
    String templateCode,
    int templateVersion,
    String modelName,
    String requestPayload,
    Instant requestedAt,
    String status,
    String errorMessage
) {}

9. 关键服务设计
9.1 PromptTemplateService
方法建议：
    • getActiveTemplate(templateCode)
    • renderPrompt(case)
    • renderSystemPrompt()
    • renderReportFormatPrompt()

9.2 LlmClient
职责：
    • 调用 API
    • timeout
    • retry
    • 错误分类
建议不要引入复杂 AI 框架链式抽象。
第一版保持简单。

9.3 InvestigationReportParser
职责：
    • 从 LLM 文本解析到 InvestigationReport
    • 若格式不完全匹配，尽量容错

9.4 AiReportService
职责：
    • 读取案件
    • 渲染 Prompt
    • 调用 LLM
    • 保存日志
    • 保存报告

9.5 AiReportGenerationJobRunner
职责：
    • 批量处理 case
    • 控制吞吐
    • 更新 job 状态

10. fraud-ai AI 代码生成 Prompt
下面是可直接用来生成模块代码的 Prompt。

Prompt 1 — 模块骨架
You are a senior Java backend architect.
Generate a production-ready module named `fraud-ai`.
Environment:
- Java 25
- Spring Boot 4.x
- Maven
- Spring JDBC
- PostgreSQL
Constraints:
- Keep dependencies minimal
- Do not use ORM frameworks
- Use explicit SQL only
- Keep AI integration simple and auditable
- Avoid heavy AI orchestration frameworks unless clearly necessary
Responsibilities:
- manage prompt templates
- render prompts from InvestigationCase
- call an external LLM API
- parse responses into InvestigationReport
- persist prompt logs and reports
- support batch and single-case generation
Project structure:
fraud-ai
 ├── model
 ├── prompt
 ├── client
 ├── parser
 ├── repository
 ├── service
 └── job
Generate:
- pom.xml
- package structure
- all source files

Prompt 2 — 模型类
Generate Java model classes for `fraud-ai`.
Required classes:
- PromptTemplate
- RenderedPrompt
- AiGenerationJob
- AiGenerationRequestLog
- InvestigationReport
Requirements:
- Use plain Java
- Use record where suitable
- Align with PostgreSQL schema

Prompt 3 — JDBC Repositories
Generate Spring JDBC repositories for `fraud-ai`.
Repositories required:
- PromptTemplateRepository
- AiGenerationJobRepository
- AiGenerationRequestLogRepository
- InvestigationReportRepository
Requirements:
- Use JdbcTemplate or NamedParameterJdbcTemplate
- Explicit SQL only
- Implement RowMapper classes
- Provide save(), findActiveTemplate(), findByCaseId(), createJob(), updateJobStatus()

Prompt 4 — Prompt 渲染
Generate prompt-related services for `fraud-ai`.
Required classes:
- PromptTemplateService
- PromptRenderer
Responsibilities:
- load active prompt templates
- render prompts using InvestigationCase
- support template types:
  - SYSTEM
  - REPORT_FORMAT
  - CASE_RENDERER
- ensure deterministic prompt rendering
Requirements:
- keep rendering logic explicit
- support prompt versioning
- do not embed prompt text directly everywhere in code

Prompt 5 — LLM Client
Generate LlmClient for `fraud-ai`.
Responsibilities:
- call an external LLM HTTP API
- send prompt payload
- handle timeout
- handle retries
- return raw response
- log request metadata
Requirements:
- keep client simple
- support configurable base URL, model name, timeout
- clearly surface API failures
- avoid unnecessary abstraction

Prompt 6 — Report Parser
Generate InvestigationReportParser.
Responsibilities:
- parse raw LLM response into InvestigationReport fields
- extract:
  - report title
  - executive summary
  - key risk indicators
  - behavior analysis
  - relationship analysis
  - timeline observations
  - possible risk patterns
  - recommendations
- tolerate imperfect formatting where practical
Requirements:
- keep parser robust but simple
- preserve raw response for auditing

Prompt 7 — AiReportService
Generate AiReportService and AiReportFacade.
Responsibilities:
- accept InvestigationCase
- render prompt
- create request log
- call LLM
- parse response
- persist InvestigationReport
- support single-case and batch generation
Methods:
- generateReport(caseId)
- generateReportForCase(InvestigationCase)
- generateBatchReports(caseIds)
Requirements:
- support retries
- support batch mode
- keep orchestration readable

Prompt 8 — Batch Job Runner
Generate AiGenerationJobRunner.
Responsibilities:
- process InvestigationCase rows in batches
- render prompts
- call LLM
- persist reports
- update ai_generation_job table
- log progress
Requirements:
- keep memory usage controlled
- include retry handling
- make failures auditable

Prompt 9 — Prompt 初始化与 README
Generate:
1. SQL initialization scripts for initial ai_prompt_template records
2. README for `fraud-ai`
README should include:
- architecture overview
- prompt versioning approach
- report generation flow
- storage tables
- single-case vs batch generation
- auditability strategy

Prompt 10 — 测试
Generate unit tests and focused integration tests for `fraud-ai`.
Test cases required:
- prompt rendering correctness
- active template loading
- request logging
- report parsing
- single-case generation flow
- batch generation flow
Requirements:
- use deterministic sample InvestigationCase objects
- keep tests stable

