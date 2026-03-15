完整的 Fraud Data Generator 设计方案。目标不是简单“随机造数”，而是生成一批可用于验证反欺诈系统的数据集：
    • 100k 账户
    • 10M 事件
    • 正常用户 + 多种欺诈模式混合
    • 可导入 PostgreSQL
    • 可直接驱动：
        ○ feature engineering
        ○ rule engine
        ○ graph analysis
        ○ anomaly detection
        ○ investigation case generation
这次先给的是 生成器总体设计 + 数据生成规则 + 输出文件规范 + 模块划分。这样你后面无论让 AI 生成代码，还是你自己分步实现，都不会跑偏。

一、生成器目标
这个生成器需要解决四个问题：
    1. 生成足够真实的正常数据
    2. 注入明确的欺诈模式
    3. 保持表之间关联一致
    4. 保证可重复生成
所以它不应该只是：
    • 随机账号
    • 随机交易
    • 随机登录
而应该是：
    • 有生命周期的账户
    • 有时间序列行为
    • 有设备/IP/银行卡复用关系
    • 有攻击链条和团伙结构

二、推荐实现方式
你前面已经收敛为：
    • Java 负责稳定和主系统
    • Python 负责动态和数据/ML
所以 Fraud Data Generator 最适合用 Python 实现。
推荐约束：
    • Python 3.12+
    • 尽量少依赖
    • 可以只用标准库先做第一版
    • 输出 CSV / JSONL
    • 最后用 PostgreSQL COPY 导入
第一版建议：
    • 仅标准库
    • 不依赖 pandas / faker
    • 用 csv, json, random, uuid, datetime, math, itertools
原因很简单：
这类数据生成器主要是逻辑驱动，不是数值计算密集任务。

三、生成器工程结构
建议项目结构：
data-generator/
├── README.md
├── config/
│   ├── default_config.json
│   └── scenario_config.json
├── generator/
│   ├── main.py
│   ├── config_loader.py
│   ├── id_factory.py
│   ├── time_utils.py
│   ├── csv_writer.py
│   ├── weighted_random.py
│   ├── entities/
│   │   ├── account_generator.py
│   │   ├── device_generator.py
│   │   ├── ip_generator.py
│   │   ├── bank_generator.py
│   │   └── payment_method_generator.py
│   ├── behaviors/
│   │   ├── normal_behavior.py
│   │   ├── login_behavior.py
│   │   ├── transaction_behavior.py
│   │   ├── security_behavior.py
│   │   └── graph_behavior.py
│   ├── fraud/
│   │   ├── fraud_base.py
│   │   ├── promotion_abuse.py
│   │   ├── credential_stuffing.py
│   │   ├── account_takeover.py
│   │   ├── money_mule.py
│   │   ├── arbitrage.py
│   │   └── collusive_ring.py
│   └── orchestrator/
│       ├── dataset_builder.py
│       ├── scenario_allocator.py
│       └── label_builder.py
└── output/

四、核心设计原则
1. 先生成“实体”，再生成“行为”
顺序建议固定为：
accounts
→ devices
→ IPs
→ bank accounts
→ account-device mappings
→ normal events
→ fraud injections
→ labels
→ graph edges
不要反过来。

2. 正常数据要远多于欺诈数据
建议分布：
    • 正常账户：97%–99%
    • 欺诈账户：1%–3%
例如 100k 账户：
    • 正常：97,500
    • 欺诈：2,500
其中欺诈内部再分配。

3. 欺诈不是“随机异常”，而是“模式异常”
每类欺诈要有清晰的行为模板，例如：
    • 羊毛党：大量新号，共享设备，领活动，快速提现
    • 撞库：大量失败登录，多个IP，少量成功
    • 盗号：异地登录，新设备，改密，提现
    • 洗钱：多账户资金分散汇集
    • 套利：高频、小额、重复金额
    • 团伙：共享设备 / 共享IP / 共享银行卡

4. 数据需要可复现
必须支持：
    • 固定随机种子
    • 同一配置重复生成相同数据集
否则模型和规则调试会很痛苦。

五、配置文件设计
建议生成器由 JSON 配置控制。
default_config.json
{
  "seed": 42,
  "start_time": "2025-01-01T00:00:00",
  "end_time": "2025-03-31T23:59:59",
  "account_count": 100000,
  "device_count": 120000,
  "ip_count": 50000,
  "bank_account_count": 80000,
  "target_login_logs": 5000000,
  "target_transactions": 3000000,
  "target_transfers": 1000000,
  "fraud_ratio": 0.025
}
scenario_config.json
{
  "fraud_distribution": {
    "promotion_abuse": 0.20,
    "credential_stuffing": 0.15,
    "account_takeover": 0.20,
    "money_mule": 0.20,
    "arbitrage": 0.10,
    "collusive_ring": 0.15
  }
}

六、主输出表与生成逻辑
下面给出每张关键表怎么生成。

1. accounts
目标
生成账户基础信息。
生成策略
    • account_id 连续或前缀化
    • 个人账户 90%，企业账户 10%
    • 国家分布按权重
    • created_at 在时间范围内随机分布
    • kyc_level 按新老账户、地区做分布
输出字段
    • account_id
    • account_type
    • country
    • kyc_level
    • created_at
    • registration_ip
    • status

2. account_profiles
目标
生成辅助属性。
生成策略
    • email / phone 采用规则化伪数据
    • birth_year 按合理范围
    • risk_score 初始为0
避免用真实格式数据，防止混淆。

3. devices
目标
生成设备池。
正常行为
    • 大部分设备只对应 1–2 个账户
    • 少数共享设备可达 3–5 个账户
欺诈行为
    • 团伙/羊毛设备可共享 10–50 个账户
字段：
    • device_id
    • device_type
    • os
    • created_at

4. account_devices
目标
建立账户-设备关系。
正常分布
    • 1 个账户平均 1.2–2.5 个设备
    • 大部分只用 1 个主设备
欺诈注入
    • 某些 fraud cluster 强制共享设备
字段：
    • account_id
    • device_id
    • first_seen
    • last_seen

5. ip_addresses / ip_intelligence
正常分布
    • 普通住宅 / ISP IP 为主
    • 少量 datacenter / VPN / proxy
欺诈注入
    • 撞库、团伙、盗号提升高风险IP占比
字段：
    • ip
    • country
    • isp
    • risk_level
    • is_vpn
    • is_proxy
    • is_datacenter

6. login_logs
正常行为模板
    • 登录频率低到中等
    • 设备相对稳定
    • IP 地理位置大致稳定
    • 失败率低
欺诈模式模板
撞库
    • 失败登录密集
    • 一个IP尝试多个账户
    • 少量成功
盗号
    • 新设备登录
    • 异常国家
    • 之后出现改密或提现
字段：
    • id
    • account_id
    • device_id
    • ip
    • success
    • login_time
规模建议：
    • 5M

7. transactions / deposits / withdrawals / transfers
正常行为
    • 交易间隔相对自然
    • 金额有长尾分布
    • 对手方适中
    • 提现不总是紧跟充值
欺诈行为模板
羊毛党
    • 注册后很快发生奖励相关入账
    • 小额入金/活动奖励
    • 快速提现
    • 对手方少
套利
    • 高频小额
    • 重复金额
    • 时间间隔规则化
洗钱 / 跑分
    • 多账户资金汇聚到少量收口账户
    • 账户间转移频繁
    • 单账户入出金接近
字段：
    • transaction_id
    • account_id
    • counterparty_account
    • amount
    • currency
    • transaction_type
    • created_at

8. security_events / password_resets
正常行为
    • 安全事件稀少
    • 改密不频繁
欺诈行为
盗号
    • 新设备登录后很快改密
    • 可能禁用2FA
    • 提现前发生安全变更
字段：
    • event_id
    • account_id
    • event_type
    • created_at

9. bank_accounts
正常行为
    • 一个银行账户只关联 1 个平台账户
    • 少量家庭共享可允许 2–3 个
欺诈行为
    • 团伙或跑分账户共享或汇聚到少量收款账户
字段：
    • bank_account_id
    • account_id
    • created_at

10. account_graph_edges / account_clusters
目标
显式保存图关系，便于图分析测试。
edge_type：
    • shared_device
    • shared_ip
    • shared_bank
    • transfer
    • cluster_member
生成方式
先按实体关系自动推导，再对欺诈团伙补充强关联。

11. fraud_labels
目标
给监督学习和评估用标签。
字段：
    • account_id
    • fraud_type
    • labeled_at
建议并非所有可疑账户都打标签。更真实的做法是：
    • 明确欺诈账户打标签
    • 边缘风险账户不打标签
    • 一部分噪声标签保留为未标注
这样更接近真实世界。

七、欺诈模式库设计
下面是每种欺诈模式的生成规则。

1. promotion_abuse / 羊毛党
典型特征
    • 新账户
    • 共享设备 / IP
    • 注册后很快参与活动
    • 奖励到账后快速提现
    • 金额偏小但频次较高
生成规则
    • 生成若干 cluster，每个 cluster 20–80 账户
    • 同 cluster 共享 2–5 个设备
    • 共享 IP 池
    • 入金少，奖励/活动型交易多
    • 提现延迟短
重点输出
    • shared_device_accounts ↑
    • account_age_days ↓
    • withdraw_after_deposit_delay ↓
    • cluster_size ↑

2. credential_stuffing / 撞库
典型特征
    • 大量登录失败
    • 一个或少数 IP 打多个账户
    • 某些账户最终被攻破
    • 时间上高度集中
生成规则
    • 选择攻击 IP 池
    • 批量生成失败登录
    • 失败后极少数成功
    • 某些成功后再衔接 ATO 场景
重点输出
    • login_failure_count ↑
    • shared_ip_accounts ↑
    • success_after_many_failures flag

3. account_takeover / 盗号
典型特征
    • 新设备登录
    • 异地 / 高风险 IP
    • 改密 / 改邮箱 / 禁用2FA
    • 快速提现
生成规则
    • 从正常账户中抽样受害账户
    • 插入攻击者设备/IP
    • 登录成功后短时间内产生安全事件和提现
重点输出
    • new_device_login_count ↑
    • high_risk_ip_login_count ↑
    • security_change_before_withdraw flag
    • impossible_travel_flag

4. money_mule / 洗钱跑分
典型特征
    • 资金快速流入又流出
    • 入出金额接近
    • 对手方集中
    • 多个账户向少数收口账户汇聚
生成规则
    • 建多个 funnel graph
    • N 个账户向 M 个中间账户转入
    • 再向 1–2 个收口账户集中
    • 时间间隔短
重点输出
    • deposit_withdraw_ratio ≈ 1
    • counterparty_count 中等偏低
    • transfer_cluster_size ↑
    • funnel flow pattern

5. arbitrage / 套利
典型特征
    • 高频
    • 小额
    • 重复金额
    • 周期性强
    • 对价格差或奖励规则进行利用
生成规则
    • 生成规则化交易节奏
    • 金额分布集中
    • 时间间隔低方差
    • 账户不一定团伙化，但行为机械
重点输出
    • transaction_count_24h ↑
    • repeated_amount_ratio ↑
    • transaction_interval_std ↓
    • periodic_pattern_score ↑

6. collusive_ring / 团伙
典型特征
    • 共享设备
    • 共享IP
    • 共享银行卡
    • 账户间有转账或共同指向同一外部资源
    • 子群落明显
生成规则
    • 生成 ring / star / tree 三类图结构
    • 每个 cluster 10–100 账户
    • 强制共享多个基础设施实体
重点输出
    • cluster_size ↑
    • shared_device_accounts ↑
    • shared_bank_accounts ↑
    • risk_neighbor_count ↑

八、正常账户行为模型
为了让欺诈足够“显眼”，正常账户的数据也必须像真的。
正常账户分群建议
不要只有一种正常用户，建议至少分 5 类：
    1. 低活跃普通用户
    2. 中活跃日常用户
    3. 高活跃交易用户
    4. 长期沉默用户
    5. 新注册用户
这样模型不会把“高活跃正常用户”误判成异常。
每类区别
    • 登录频率
    • 交易频率
    • 夜间活跃占比
    • 对手方数量
    • 设备切换频率

九、时间建模
时间是反欺诈里非常关键的维度。
建议时间范围
先生成 90 天数据：
    • 2025-01-01 到 2025-03-31
时间分布规则
正常账户
    • 白天更活跃
    • 周末模式略有不同
    • 安全事件稀少
    • 行为有波动，但不极端
欺诈账户
    • 常集中在夜间 / 清晨
    • 爆发式
    • 事件链更紧密
    • 更容易出现“登录→改密→提现”短链条

十、输出文件设计
建议每张表生成一个 CSV。
output/
├── accounts.csv
├── account_profiles.csv
├── account_balance.csv
├── account_limits.csv
├── devices.csv
├── device_fingerprint.csv
├── account_devices.csv
├── device_clusters.csv
├── ip_addresses.csv
├── ip_intelligence.csv
├── login_logs.csv
├── login_failures.csv
├── login_sessions.csv
├── transactions.csv
├── deposits.csv
├── withdrawals.csv
├── transfers.csv
├── payment_methods.csv
├── security_events.csv
├── password_resets.csv
├── bank_accounts.csv
├── account_graph_edges.csv
├── account_clusters.csv
├── fraud_labels.csv
├── investigation_cases_seed.csv

十一、生成顺序
推荐固定顺序：
1. generate accounts
2. generate profiles / balances / limits
3. generate devices
4. generate IPs
5. assign devices/IPs to normal accounts
6. assign bank accounts
7. generate normal login behavior
8. generate normal transactions
9. generate normal security events
10. allocate fraud scenarios
11. inject fraud behavior
12. generate graph edges
13. generate fraud labels
14. write CSV files

十二、规模建议
第一版建议支持三档规模：
small
    • 10k accounts
    • 1M events
medium
    • 100k accounts
    • 10M events
large
    • 1M accounts
    • 100M events
通过配置切换，而不是写死。

十三、生成器内部对象模型
建议 Python 内部先用 dataclass 或简单 dict。
例如：
Account
Device
IpInfo
Transaction
LoginLog
SecurityEvent
FraudScenario
Cluster
但不要在内存里同时持有全部 10M 事件明细对象太久。
更好的办法是：
    • 分批生成
    • 边生成边写 CSV
    • 避免内存爆炸

十四、关键实现策略
1. 分块写文件
比如每 100k 或 500k 行 flush 一次。
2. ID 规则统一
例如：
    • account: ACC0000001
    • device: DEV0000001
    • ip: IP0000001 或直接 IPv4
    • bank: BANK0000001
3. 使用场景注入而不是后置修补
不要先完全生成完正常数据再硬改。
更好的方法是“预留一批账户作为 scenario participants”。
4. 标签单独输出
fraud_labels.csv 不要和业务数据混在一起。

十五、与主系统的衔接方式
你现在的反欺诈平台可以这样接入：
Fraud Data Generator
      ↓
CSV files
      ↓
PostgreSQL COPY
      ↓
fraud-ingestion / feature / risk / graph / ML
早期阶段完全没必要先做 API 导入。
离线导入是正确路线。


