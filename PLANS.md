# Current Plans

## 已完成

### 1. `fraud-offline` 架构收口

- 已拆分离线编排层、`analysis` 层、`io` 层
- 保留旧入口兼容
- 已支持：
  - replay
  - baseline
  - anomaly
  - cluster
  - bot indicators

### 2. `fraud-offline` 并入当前体系

- 已支持把离线分析结果映射到当前体系
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
  - 协同交易辅助信号
  - 非实体关系图

### 4. `fraud-risk` 模块边界收口

已完成以下内部收口：

- [GraphRiskSignalResolver.java](fraud-risk/src/main/java/me/asu/ta/risk/service/GraphRiskSignalResolver.java)
  - 统一图信号回退与上下文口径
- [BehaviorScorePolicy.java](fraud-risk/src/main/java/me/asu/ta/risk/scoring/BehaviorScorePolicy.java)
  - 统一行为评分阈值、分值、原因码
- [RiskReasonGenerator.java](fraud-risk/src/main/java/me/asu/ta/risk/reason/RiskReasonGenerator.java)
  - 已按来源拆开 reason candidate 生成逻辑
- [RiskScoreResultFactory.java](fraud-risk/src/main/java/me/asu/ta/risk/service/RiskScoreResultFactory.java)
  - 已抽出风险结果装配
- [RiskEvaluationService.java](fraud-risk/src/main/java/me/asu/ta/risk/service/RiskEvaluationService.java)
  - 当前已固定为评分编排层

### 5. 双入口运行模式落地

- 已新增 `fraud-batch`
  - 定位为定期批处理编排入口
- 已升级 `fraud-online`
  - 定位为查询 API、手动重算和事件驱动重算入口
- 已补齐读模型相关 repository/service 能力

### 6. Spring Boot 版本线收口

- 全仓 Spring Boot 已统一到 `3.5.7`
- `mvn verify` 已通过

## 当前建议

### 1. `fraud-risk`

当前不建议继续细拆。

现阶段更适合：
- 保持 `RiskEngineFacade` 作为对外稳定入口
- 保持 `RiskEvaluationService` 作为评分编排层
- 停止继续制造新的薄 facade

### 2. `fraud-rule-engine`

当前建议继续保持为：
- 独立模块
- 共享规则内核

不建议把它继续包装成新的独立业务主程序。

### 3. `fraud-online`

当前已经进入正式主链路，但下一阶段更值得继续完善的是：
- API 错误码规范
- 查询结果字段一致性
- 事件上下文映射边界

### 4. `fraud-batch`

当前已经具备主批处理入口形态，但要继续控制边界：
- 只负责编排
- 不复制 feature/risk/case 业务逻辑
- 不吸收 offline 分析职责

## 下一阶段建议

### A. 文档持续同步

建议持续维护：
- [README.md](README.md)
- [架构.md](docs/架构.md)
- 子模块 README

### B. `fraud-online` 完善

重点建议：
- 增加统一错误响应格式
- 增加 API 层测试覆盖
- 明确查询与重算的鉴权边界

### C. `fraud-batch` 完善

重点建议：
- 明确 job 参数约定
- 完善批任务运行摘要与监控输出
- 评估是否需要统一 job 视图，而不是直接暴露底层 repository 结构

### D. `fraud-feature` 持续防膨胀

继续坚持：
- `feature` 只负责特征
- `risk` 只负责评分
- `case` 只负责调查表达

## 验证基线

当前最近一轮调整后，已通过：

```bash
mvn verify
```

当前仍存在但未影响构建成功的既有告警：
- `fraud-offline` 的 `maven-shade-plugin` 版本未显式声明
- shade 打包存在重复资源告警
