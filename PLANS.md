# Current Plans

## 已完成

### 1. `fraud-offline` 架构收口

- 已拆出离线编排层、analysis 层、io 层
- 保留旧入口兼容
- 已支持：
  - replay
  - baseline
  - anomaly
  - cluster
  - bot indicators

### 2. `fraud-offline` 并入当前体系

- 已支持把离线结果映射到当前体系
- 已打通：
  - `offline -> feature`
  - `offline -> risk`
  - `offline -> case` 的解释增强链路

### 3. 基于交易记录的行为分析

- 已新增：
  - 行为向量
  - 行为分群
  - 行为相似边
- 当前结果定位为：
  - 辅助分析输出
  - 弱关系/协同行为辅助信号
  - 非实体关系图

### 4. `fraud-risk` 模块边界收口

已完成以下内部收口：

- [GraphRiskSignalResolver.java](fraud-risk/src/main/java/me/asu/ta/risk/service/GraphRiskSignalResolver.java)
  - 统一图信号回退与上下文口径
- [BehaviorScorePolicy.java](fraud-risk/src/main/java/me/asu/ta/risk/scoring/BehaviorScorePolicy.java)
  - 统一行为评分阈值、分值、原因码
- [RiskReasonGenerator.java](fraud-risk/src/main/java/me/asu/ta/risk/reason/RiskReasonGenerator.java)
  - 已按来源拆开 candidate 生成逻辑
- [RiskScoreResultFactory.java](fraud-risk/src/main/java/me/asu/ta/risk/service/RiskScoreResultFactory.java)
  - 已抽出结果对象装配
- [RiskEvaluationService.java](fraud-risk/src/main/java/me/asu/ta/risk/service/RiskEvaluationService.java)
  - 当前已固定为评分编排层

## 当前建议

### 1. `fraud-risk`

当前不建议继续细拆。

现阶段更适合：

- 保持 `RiskEngineFacade` 作为对外稳定入口
- 保持 `RiskEvaluationService` 作为编排层
- 停止继续制造新的薄 facade

### 2. `fraud-rule-engine`

当前建议继续保持为：

- 独立模块
- 共享规则内核

不建议把它继续包装成新的独立业务主程序。

### 3. `fraud-online`

下一阶段更值得评估的是：

- `fraud-online` 与主评分链路的关系
- 它究竟是演示程序、适配层，还是要进入正式主链路

## 待做建议

### A. 文档持续同步

建议持续维护：

- [README.md](README.md)
- [docs/架构.md](docs/架构.md)
- 子模块 README

### B. `fraud-online` 评估

重点判断：

- 是否继续保留为演示入口
- 是否接入 `RiskEngineFacade`
- 是否需要独立程序化运行

### C. `fraud-feature` 持续防膨胀

继续坚持：

- feature 只负责特征
- risk 只负责评分
- case 只负责调查表达

## 验证基线

当前最近一轮调整后，已通过：

```bash
mvn -pl fraud-risk -am clean test -Dmaven.test.skip=false -DskipTests=false
mvn verify
```
