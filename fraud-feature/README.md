# fraud-feature

`fraud-feature` 是特征域模块，不是统一风控编排入口。

它负责：

- 定义账户特征字段与版本
- 生成 `AccountFeatureSnapshot`
- 持久化 snapshot 与 history
- 向 `fraud-risk`、`fraud-case`、`fraud-offline` 提供稳定的特征读取接口
- 暴露图衍生特征，但不承载 anomaly 或最终风险语义

它不负责：

- 最终风险分公式与风险等级
- ML anomaly 信号及其解释
- 离线 replay/markout 分析
- 案件组装与调查时间线
- AI prompt/render/report 逻辑

## 当前内部结构

- `FeatureGenerationService`
  - 只负责生成快照
- `FeaturePersistenceService`
  - 只负责落 snapshot/history
- `FeatureQueryService`
  - 只负责读取最新特征
- `FeatureStoreService`
  - 兼容 facade，保留现有调用方式
- `FeatureGenerationJobRunner`
  - 只负责批量扫描账户并调用生成/持久化服务
