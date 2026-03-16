
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

