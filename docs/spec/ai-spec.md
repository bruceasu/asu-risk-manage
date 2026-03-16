
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
[ai-schema.sql](../sql/ai-schema.sql)

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
[ai.prompt.md](../prompts/ai.prompt.md)