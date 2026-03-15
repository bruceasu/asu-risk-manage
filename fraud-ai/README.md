# fraud-ai

`fraud-ai` 是把结构化案件转换成可审计 AI 调查报告的模块。它不参与最终风控决策，只负责基于 [fraud-case](/d:/03_projects/suk/asu-trading-analysis/fraud-case) 提供的案件详情生成报告、记录调用审计信息，并持久化最终报告。

## Architecture Overview

模块按职责拆分为：

- `model`
  - Prompt 模板、请求日志、报告、批任务等核心模型
- `prompt`
  - 模板加载与渲染
- `client`
  - OpenAI 兼容 HTTP LLM client
- `parser`
  - LLM 响应解析为 `InvestigationReport`
- `repository`
  - Spring JDBC 持久化
- `service`
  - 单案与批量报告生成编排
- `job`
  - 批量报告任务执行

主链路如下：

1. `AiReportService` 通过 [CaseFacade.java](/d:/03_projects/suk/asu-trading-analysis/fraud-case/src/main/java/me/asu/ta/casemanagement/service/CaseFacade.java) 读取 `InvestigationCaseBundle`
2. `PromptTemplateService` 加载 active 模板
3. `PromptRenderer` 生成 system prompt、report format prompt、case prompt
4. `OpenAiCompatibleLlmClient` 调用外部 LLM
5. `InvestigationReportParser` 解析 JSON 响应
6. repository 落 `ai_generation_request_log` 与 `investigation_report`

## Prompt Versioning Approach

Prompt 模板保存在 `ai_prompt_template` 表，主键为 `template_code + version`。

- `PromptTemplateService.findActiveTemplate(...)` 按 `template_code + template_type + is_active = true` 查询
- 如果同一模板有多个 active 版本，按 `version desc` 取最高版本
- 第一版固定使用三类模板：
  - `INVESTIGATION_REPORT_SYSTEM`
  - `INVESTIGATION_REPORT_FORMAT`
  - `INVESTIGATION_CASE_RENDERER`

这样做的好处是：

- prompt 文本不散落在代码里
- 模板升级可审计
- 报告记录里会保存 `template_code` 与 `template_version`

## Report Generation Flow

单案生成由 [AiReportService.java](/d:/03_projects/suk/asu-trading-analysis/fraud-ai/src/main/java/me/asu/ta/ai/service/AiReportService.java) 驱动：

1. 读取案件详情
2. 加载 `SYSTEM`、`REPORT_FORMAT`、`CASE_RENDERER` 三类 active 模板
3. 渲染 prompt
4. 创建 `ai_generation_request_log`
5. 调用 LLM
6. 解析响应为 `InvestigationReport`
7. 在事务内更新 request log 状态并保存报告

当前 `CASE_RENDERER` 使用这些显式占位符：

- `{{caseId}}`
- `{{accountId}}`
- `{{riskLevel}}`
- `{{riskScore}}`
- `{{topReasonCodes}}`
- `{{riskSummary}}`
- `{{featureSummary}}`
- `{{ruleHits}}`
- `{{graphSummary}}`
- `{{timeline}}`
- `{{recommendedActions}}`

渲染逻辑都集中在 [PromptRenderer.java](/d:/03_projects/suk/asu-trading-analysis/fraud-ai/src/main/java/me/asu/ta/ai/prompt/PromptRenderer.java)，不会做隐藏转换。

## Storage Tables

模块当前使用这些表：

- `ai_prompt_template`
  - prompt 模板与版本管理
- `ai_generation_job`
  - 批量报告任务状态
- `ai_generation_request_log`
  - 每次 LLM 调用的审计日志
- `investigation_report`
  - 最终结构化调查报告
- `ai_generation_retry_log`
  - schema 中已预留，当前第一版未单独落细粒度 retry 记录

初始化模板可参考：

- [01_ai_prompt_template_init.sql](/d:/03_projects/suk/asu-trading-analysis/fraud-ai/sql/01_ai_prompt_template_init.sql)

## Single-Case vs Batch Generation

单案模式：

- 使用 [AiReportFacade.java](/d:/03_projects/suk/asu-trading-analysis/fraud-ai/src/main/java/me/asu/ta/ai/service/AiReportFacade.java) 的 `generateReport(caseId)`
- 也支持直接对 `InvestigationCaseBundle` 调用 `generateReportForCase(...)`

批量模式：

- 由 [AiGenerationJobRunner.java](/d:/03_projects/suk/asu-trading-analysis/fraud-ai/src/main/java/me/asu/ta/ai/job/AiGenerationJobRunner.java) 执行
- 默认 batch size 为 `500`
- 通过 [InvestigationCaseBatchReader.java](/d:/03_projects/suk/asu-trading-analysis/fraud-ai/src/main/java/me/asu/ta/ai/repository/InvestigationCaseBatchReader.java) 分批读取 `case_id`
- 单案失败不会阻断后续 case
- 最终 job 状态可能是：
  - `COMPLETED`
  - `PARTIAL_SUCCESS`
  - `FAILED`

## Auditability Strategy

模块把“可审计”放在第一优先级：

- 每次调用 LLM 前，先写 `ai_generation_request_log`
- request log 中保存：
  - `case_id`
  - `template_code`
  - `template_version`
  - `model_name`
  - `request_payload`
  - `status`
  - `error_message`
- 报告表保存：
  - 结构化字段
  - `raw_response`
  - `template_code`
  - `template_version`
  - `model_name`

当前普通应用日志只记录元数据，不打印完整 prompt 或完整模型响应；完整内容通过数据库审计表保留。

## Notes

- 当前 LLM client 是 OpenAI 兼容协议实现，使用 JDK `HttpClient`，保持依赖最小。
- parser 主路径仍要求返回合法 JSON 对象；近期已补“轻度容错”，允许部分字段别名和缺字段默认空串，但不会尝试解析任意自由文本。
