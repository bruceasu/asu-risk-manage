# fraud-rule-engine

`fraud-rule-engine` 是统一后的规则引擎模块，合并了原来的 core 和 library，两部分代码现在在同一个 Maven 模块中维护，但继续保留原有 Java 包名：

- 核心编排与配置加载：`me.asu.ta.rule.*`
- 具体规则实现：`me.asu.ta.rule.library.*`

这样可以减少其他模块的 import 改动，同时把规则引擎的代码、测试、SQL 和文档收口到一个模块中。

## Architecture Overview

模块内部仍按职责分层：

- `api`
  - 规则接口，例如 `FraudRule`
- `model`
  - 规则定义、版本、配置、评估上下文、评估结果等核心模型
- `engine`
  - `RuleRegistry`、`RuleEngine`、`RuleResultAggregator`
- `config`
  - Spring Boot 装配
- `repository`
  - 基于 Spring JDBC 的显式 SQL 访问
- `service`
  - `RuleConfigService`、`RuleEvaluationService`、`RuleEngineFacade` 等服务
- `library`
  - 按规则域组织的具体规则实现：
    - `login`
    - `transaction`
    - `device`
    - `security`
    - `graph`
    - `composite`

推荐对外入口是 [RuleEngineFacade.java](/d:/03_projects/suk/asu-trading-analysis/fraud-rule-engine/src/main/java/me/asu/ta/rule/service/RuleEngineFacade.java)。

## How Rule Logic Is Structured

规则逻辑依旧保持“配置在库里，逻辑在 Java 里”的结构：

- 规则接口定义在 [FraudRule.java](/d:/03_projects/suk/asu-trading-analysis/fraud-rule-engine/src/main/java/me/asu/ta/rule/api/FraudRule.java)
- 核心选择与执行由
  - [RuleRegistry.java](/d:/03_projects/suk/asu-trading-analysis/fraud-rule-engine/src/main/java/me/asu/ta/rule/engine/RuleRegistry.java)
  - [RuleEngine.java](/d:/03_projects/suk/asu-trading-analysis/fraud-rule-engine/src/main/java/me/asu/ta/rule/engine/RuleEngine.java)
  - [RuleResultAggregator.java](/d:/03_projects/suk/asu-trading-analysis/fraud-rule-engine/src/main/java/me/asu/ta/rule/engine/RuleResultAggregator.java)
  完成
- 具体规则实现集中在 `me.asu.ta.rule.library.*`

每条规则一般会：

1. 从 `RuleEvaluationContext.attributes().get("snapshot")` 读取 `AccountFeatureSnapshot`
2. 从 `RuleConfig` 读取阈值
3. 使用显式 Java 判断是否命中
4. 返回包含 `reasonCode`、`message`、`evidence`、`ruleVersion` 的 `RuleEvaluationResult`

## How Rule Configs Are Stored

规则配置分两层存储：

- `rule_definition`
  - 存规则编码、分类、严重级别、当前版本、是否启用
- `rule_version`
  - 存具体版本、`parameter_json`、`score_weight`、生效时间窗口

运行时：

- [RuleConfigService.java](/d:/03_projects/suk/asu-trading-analysis/fraud-rule-engine/src/main/java/me/asu/ta/rule/service/RuleConfigService.java) 从 PostgreSQL 加载生效配置
- [RuleParameterParser.java](/d:/03_projects/suk/asu-trading-analysis/fraud-rule-engine/src/main/java/me/asu/ta/rule/service/RuleParameterParser.java) 把 `parameter_json` 解析为：
  - 原始 `Map<String, Object>`
  - 校验后的 typed params

当前已支持的 typed 参数族包括：

- `LoginRuleParams`
- `TransactionRuleParams`
- `DeviceRuleParams`
- `SecurityRuleParams`
- `GraphRuleParams`
- `CompositeRuleParams`

## How Rule Versioning Works

版本控制依赖 `rule_version(rule_code, version)`：

- `rule_definition.current_version` 表示当前运营视角的版本号
- 实际评估时使用 `effective_from <= asOfTime < effective_to` 选出有效版本
- 只有 `enabled = true` 的版本会参与缓存和执行

这样可以在不改 Java 代码的前提下，通过新增或切换 `rule_version` 完成参数发布。

## How Rule Hot Reload Works

热加载仍由 [RuleConfigService.java](/d:/03_projects/suk/asu-trading-analysis/fraud-rule-engine/src/main/java/me/asu/ta/rule/service/RuleConfigService.java) 负责：

- JVM 内缓存当前有效配置
- `@Scheduled(fixedDelay = 30000)` 周期刷新
- `getConfig(ruleCode, asOfTime)` 遇到缓存缺失时可主动触发 `reload()`
- reload 结果写入 `rule_config_reload_log`

坏配置处理策略保持不变：

- 单条 `parameter_json` 非法时跳过该条规则版本
- 其余有效配置继续进入缓存
- reload 日志记录 `SUCCESS`、`PARTIAL_SUCCESS` 或 `FAILED`

## How To Add a New Rule

1. 在 `me.asu.ta.rule.library.*` 下新增一个实现 `FraudRule` 的类
2. 选择合适的 `RuleCategory` 并定义稳定的 `ruleCode()`
3. 如果需要新参数，优先复用现有 typed params；确实不够时再扩展 parser 和参数对象
4. 在初始化 SQL 或数据库中补充：
   - `rule_definition`
   - `rule_version`
5. 运行 `mvn verify`

如果需要新增外部上下文字段，优先通过 [RuleEngineFacadeContext.java](/d:/03_projects/suk/asu-trading-analysis/fraud-rule-engine/src/main/java/me/asu/ta/rule/model/RuleEngineFacadeContext.java) 的 `graphSignals` 或 `attributes` 透传，而不是修改底层 repository。

## How BATCH and REALTIME Evaluation Differ

两种模式共用同一套规则代码，但调度语义不同：

- `REALTIME`
  - 常用入口：`RuleEngineFacade.evaluateAccount(...)`
  - 多用于在线决策
- `BATCH`
  - 常用入口：`RuleEngineFacade.evaluateBatch(...)`
  - 多用于离线批量评估、审查或回溯

当前模式差异主要体现在：

- `RuleEvaluationContext.evaluationMode()`
- `rule_hit_log.evaluation_mode`

是否在某条规则里按模式分支，取决于具体规则实现。

## Initialization SQL

规则初始化脚本位于：

- [01_rule_definition_init.sql](/d:/03_projects/suk/asu-trading-analysis/fraud-rule-engine/sql/01_rule_definition_init.sql)
- [02_rule_version_init.sql](/d:/03_projects/suk/asu-trading-analysis/fraud-rule-engine/sql/02_rule_version_init.sql)
