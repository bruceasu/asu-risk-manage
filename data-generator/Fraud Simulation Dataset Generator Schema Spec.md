
1. Generator Schema Spec
这部分定义每个输出文件的字段、类型、约束、生成规则。
目标是让生成器输出的 CSV 可以直接 COPY 进 PostgreSQL。
1.1 全局规则
文件格式
    • 编码：UTF-8
    • 格式：CSV
    • 第一行包含 header
    • 时间统一使用 ISO 8601 或 PostgreSQL 可直接解析的时间格式
    • 布尔值统一使用 true/false
    • 金额统一保留 2 位小数
    • 空值使用空字符串，不写 "null"
ID 规范
    • account_id: ACC0000001
    • device_id: DEV0000001
    • bank_account_id: BANK0000001
    • cluster_id: CLS000001
    • session_id: SES0000001
    • payment_id: PM0000001
时间范围
默认：
    • start_time: 2025-01-01 00:00:00
    • end_time: 2025-03-31 23:59:59

1.2 accounts.csv
作用
账户主表。
字段
字段名	类型	说明
account_id	varchar(64)	主键
account_type	varchar(16)	individual / business
country	varchar(8)	国家代码
kyc_level	int	0-3
created_at	timestamp	注册时间
registration_ip	varchar(64)	注册IP
status	varchar(16)	active / suspended / locked
生成规则
    • 90% individual, 10% business
    • kyc_level 分布建议：0:10%, 1:35%, 2:40%, 3:15%
    • status 默认 96% active, 2% suspended, 2% locked
    • created_at 在时间范围内均匀或轻微前倾分布
    • registration_ip 从 ip_addresses.csv 中选取

1.3 account_profiles.csv
字段名	类型	说明
account_id	varchar(64)	外键
email	varchar(128)	伪邮箱
phone	varchar(32)	伪手机号
birth_year	int	1940-2007
risk_score	int	初始画像分，默认0
生成规则
    • email 规则化生成，如 user00001@example.test
    • phone 使用伪格式，不碰真实号段
    • birth_year 正态近似分布到 1985-2000 区间为主

1.4 account_balance.csv
字段名	类型
account_id	varchar(64)
currency	varchar(8)
balance	numeric(18,2)
updated_at	timestamp
规则
    • currency 以 USD, JPY, EUR 为主
    • balance 正常账户分布长尾，欺诈账户可偏低或中等

1.5 account_limits.csv
字段名	类型
account_id	varchar(64)
daily_withdraw_limit	numeric(18,2)
daily_transfer_limit	numeric(18,2)
规则
    • 按账户类型和 KYC 分层
    • 新账户默认限额更低

1.6 devices.csv
字段名	类型
device_id	varchar(64)
device_type	varchar(32)
os	varchar(32)
created_at	timestamp
规则
    • device_type: mobile 65%, desktop 30%, tablet 5%
    • os: ios / android / windows / macos / linux
    • 欺诈设备池后续可由 cluster 注入共享

1.7 device_fingerprint.csv
字段名	类型
device_id	varchar(64)
browser	varchar(32)
os_version	varchar(32)
screen_resolution	varchar(32)
timezone	varchar(32)
规则
    • 与 devices.csv 一致
    • 欺诈设备农场可复用高度相似指纹

1.8 account_devices.csv
字段名	类型
account_id	varchar(64)
device_id	varchar(64)
first_seen	timestamp
last_seen	timestamp
规则
    • 正常账户平均 1.2–2.2 台设备
    • 欺诈 cluster 可强制多账户共享同设备
    • first_seen ≤ last_seen

1.9 device_clusters.csv
字段名	类型
cluster_id	varchar(64)
device_id	varchar(64)
规则
    • 正常设备多数不属于 cluster
    • 欺诈设备农场 cluster 大小可 5–50

1.10 ip_addresses.csv
字段名	类型
ip	varchar(64)
country	varchar(8)
isp	varchar(64)
规则
    • 可直接生成 IPv4 字符串
    • country 与账户地域部分相关，但不完全一致

1.11 ip_intelligence.csv
字段名	类型
ip	varchar(64)
risk_level	varchar(16)
is_vpn	boolean
is_proxy	boolean
is_datacenter	boolean
规则
    • risk_level: low / medium / high
    • 正常池中 80% low, 15% medium, 5% high
    • 欺诈场景中高风险 IP 占比提升

1.12 login_logs.csv
字段名	类型
id	bigint
account_id	varchar(64)
device_id	varchar(64)
ip	varchar(64)
success	boolean
login_time	timestamp
规则
    • 正常账户：低失败率，低切换率
    • 撞库：大量失败
    • ATO：新设备、新IP、随后安全事件

1.13 login_failures.csv
字段名	类型
id	bigint
account_id	varchar(64)
ip	varchar(64)
attempt_time	timestamp
规则
    • 可由 login_logs 中 success=false 单独抽出，或单独生成
    • 撞库场景重点使用

1.14 login_sessions.csv
字段名	类型
session_id	varchar(64)
account_id	varchar(64)
device_id	varchar(64)
login_time	timestamp
logout_time	timestamp
规则
    • logout_time ≥ login_time
    • 正常 session 更长更自然
    • 欺诈行为可表现为极短高频 session

1.15 transactions.csv
字段名	类型
transaction_id	bigint
account_id	varchar(64)
counterparty_account	varchar(64)
amount	numeric(18,2)
currency	varchar(8)
transaction_type	varchar(16)
created_at	timestamp
transaction_type 建议
    • deposit
    • withdraw
    • transfer_in
    • transfer_out
    • reward
    • trade
规则
    • 正常：金额长尾、间隔不规则
    • 欺诈：模板化、节奏紧、对手集中

1.16 deposits.csv
字段名	类型
deposit_id	bigint
account_id	varchar(64)
amount	numeric(18,2)
source	varchar(32)
created_at	timestamp
source
    • bank
    • card
    • internal
    • promo_reward

1.17 withdrawals.csv
字段名	类型
withdraw_id	bigint
account_id	varchar(64)
amount	numeric(18,2)
target	varchar(64)
created_at	timestamp
target
    • 银行卡ID或外部目标标识

1.18 transfers.csv
字段名	类型
transfer_id	bigint
from_account	varchar(64)
to_account	varchar(64)
amount	numeric(18,2)
created_at	timestamp
规则
    • 洗钱 / 团伙场景重点使用

1.19 payment_methods.csv
字段名	类型
payment_id	varchar(64)
account_id	varchar(64)
method_type	varchar(32)
created_at	timestamp
method_type
    • bank_card
    • virtual_card
    • bank_account
    • wallet

1.20 security_events.csv
字段名	类型
event_id	bigint
account_id	varchar(64)
event_type	varchar(32)
created_at	timestamp
event_type
    • password_change
    • email_change
    • phone_change
    • two_factor_disable
    • two_factor_enable
    • kyc_update

1.21 password_resets.csv
字段名	类型
reset_id	bigint
account_id	varchar(64)
reset_time	timestamp

1.22 bank_accounts.csv
字段名	类型
bank_account_id	varchar(64)
account_id	varchar(64)
created_at	timestamp
规则
    • 正常：一对一为主
    • 团伙：共享收口账户或少量目标账户

1.23 account_graph_edges.csv
字段名	类型
from_account	varchar(64)
to_account	varchar(64)
edge_type	varchar(32)
created_at	timestamp
edge_type
    • shared_device
    • shared_ip
    • shared_bank
    • transfer
    • cluster_member

1.24 account_clusters.csv
字段名	类型
cluster_id	varchar(64)
account_id	varchar(64)
规则
    • 欺诈 cluster 为主
    • 可少量正常家庭/办公室共享群体，制造噪声

1.25 fraud_labels.csv
字段名	类型
account_id	varchar(64)
fraud_type	varchar(32)
labeled_at	timestamp
fraud_type
    • promotion_abuse
    • credential_stuffing
    • account_takeover
    • money_mule
    • arbitrage
    • collusive_ring
规则
    • 不必覆盖全部异常账户
    • 建议保留部分“未标注但行为可疑”账户，模拟真实世界

2. Fraud Scenario Spec
下面定义每一种欺诈模式的生成参数、账户比例、行为链和核心输出特征。

2.1 总体分布建议
以 100,000 账户为例：
    • 正常账户：97,500
    • 欺诈账户：2,500
欺诈内部建议：
fraud_type	占欺诈账户比例	账户数
promotion_abuse	20%	500
credential_stuffing	15%	375
account_takeover	20%	500
money_mule	20%	500
arbitrage	10%	250
collusive_ring	15%	375
注意：
    • credential_stuffing 和 account_takeover 可以部分串联
    • collusive_ring 可以与 promotion_abuse / money_mule 重叠

2.2 正常用户场景
user_segment_1: low_activity_normal
    • 占正常账户 35%
    • 月登录 1–5 次
    • 月交易 0–3 次
    • 单设备为主
    • IP 稳定
    • 几乎无安全事件
user_segment_2: medium_activity_normal
    • 占 30%
    • 周登录 2–5 次
    • 周交易 1–5 次
    • 1–2 个设备
    • 少量跨IP切换
user_segment_3: high_activity_normal
    • 占 15%
    • 每日登录
    • 高频交易但行为自然
    • 多对手方
    • 金额方差大
user_segment_4: dormant_accounts
    • 占 10%
    • 很少登录
    • 几乎无交易
user_segment_5: new_users
    • 占 10%
    • 注册时间近
    • 活动较少
    • 行为短历史

2.3 promotion_abuse
目标画像
羊毛党、活动套利、批量薅奖励。
账户分布
    • cluster 数：10–30
    • 每 cluster：15–60 个账户
行为链
    1. 短时间密集注册
    2. 共用设备/共享IP
    3. 快速完成活动条件
    4. 获得 reward / bonus
    5. 快速提现或转移
生成参数
    • account_age_days 偏低
    • kyc_level 偏低
    • shared_device_accounts 高
    • reward 交易占比较高
    • withdraw_after_deposit_delay 低
核心规则信号
    • 新户高活跃
    • 共享设备
    • 奖励到账后快速提现
    • 集中时段活动
标签策略
    • 全标或 80% 标注皆可

2.4 credential_stuffing
目标画像
撞库攻击，密码喷洒。
账户结构
    • 不一定都是 fraud_labels 中的账户
    • 部分是受害目标
    • 部分只是被尝试
行为链
    1. 攻击IP池准备
    2. 对大量账户发起失败登录
    3. 少数账户成功登录
    4. 成功账户可进入 ATO 场景
生成参数
    • attack_ip_pool_size: 20–100
    • burst_window_minutes: 30–240
    • failure_to_success_ratio 极高
核心信号
    • login_failure_count 高
    • 多账户共享攻击 IP
    • 时间集中爆发
标签策略
    • 可只给“攻击成功且可归因账户”打标签
    • 被撞库但未成功的账户不一定打 fraud label

2.5 account_takeover
目标画像
正常账户被接管。
账户来源
    • 从正常账户中抽取
    • 账户历史要“像正常人”
行为链
    1. 新设备 / 新IP 成功登录
    2. 异地或高风险 IP
    3. 改密 / 改邮箱 / 关闭2FA
    4. 发起提现 / 转账
    5. 活动突然异常
生成参数
    • new_device_login_count 增大
    • impossible_travel_flag 概率高
    • security_event_count_24h 高
    • withdrawal shortly after login/security change
核心信号
    • 登录异常
    • 安全事件异常
    • 资金流突然变化
标签策略
    • 全部打标签较合理

2.6 money_mule
目标画像
洗钱 / 跑分中转账户。
账户结构
    • source accounts → mule accounts → collector accounts
    • 可形成 funnel
行为链
    1. 多账户资金流入 mule
    2. mule 短时间转出
    3. 最终集中到少量 collector
生成参数
    • deposit_withdraw_ratio 接近 1
    • transfer_out shortly after transfer_in
    • counterparties 不少，但结构集中
    • cluster graph 明显
核心信号
    • rapid_in_out
    • graph funnel
    • 共享收口账户或强转账网络
标签策略
    • 重点标签给 mule 和 collector
    • source 可视情况不全部标注

2.7 arbitrage
目标画像
规则套利、机械化小额高频行为。
行为链
    1. 高频小额交易
    2. 重复金额
    3. 低方差固定时间间隔
    4. 对手方少量重复
生成参数
    • repeated_amount_ratio 高
    • periodic_pattern_score 高
    • transaction_interval_std 低
    • round_number_transaction_ratio 高
核心信号
    • 高频
    • 规则化
    • 不像自然人
标签策略
    • 数量可以少，但特征要非常清晰

2.8 collusive_ring
目标画像
团伙型账户群，基础设施共享。
图结构建议
    • star
    • ring
    • tree
    • multi-clique
行为链
    1. 共享设备/IP/银行卡
    2. 群内转账或共同指向外部目标
    3. 某些子节点承担特定角色
生成参数
    • cluster_size 10–100
    • shared_device_accounts 高
    • risk_neighbor_count 高
    • graph_density 中高
核心信号
    • shared infrastructure
    • cluster membership
    • high-risk neighbors
标签策略
    • 可全标，也可只标核心成员，制造监督学习难度

3. Python 代码生成 Prompt
下面这份 Prompt 可以直接用于让 AI 生成 fraud-data-generator 项目。

Prompt A：生成整个项目骨架
# Prompt - Generate fraud-data-generator Project
You are a senior Python engineer.
Generate a production-oriented fraud data generator project.
Purpose:
Create a synthetic fraud dataset for testing a fraud detection platform.
Constraints:
- Python 3.12+
- Prefer standard library only
- Do not add third-party dependencies unless there is a strong justification
- Output CSV files suitable for PostgreSQL COPY
- Keep code deterministic with configurable random seed
- Support small, medium, and large dataset sizes
- Keep memory usage reasonable by streaming rows to CSV instead of holding everything in memory
Project name:
fraud-data-generator
Target output:
- 100k accounts
- 10M total events
- multiple fraud scenarios
- 25 CSV output files
Required project structure:
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
│   ├── behaviors/
│   ├── fraud/
│   └── orchestrator/
└── output/
Requirements:
1. Generate the full directory structure.
2. Generate all Python modules with real code.
3. Implement configuration loading from JSON.
4. Implement deterministic random seed handling.
5. Implement chunked CSV writing.
6. Add CLI entry point.
7. Add README with usage instructions.
Output:
- full project source code
- config examples
- README

Prompt B：生成基础实体生成器
# Prompt - Generate Base Entity Generators
Generate the base entity generation modules for fraud-data-generator.
Modules to implement:
- account_generator.py
- device_generator.py
- ip_generator.py
- bank_generator.py
- payment_method_generator.py
- id_factory.py
- time_utils.py
- weighted_random.py
Requirements:
1. Use Python standard library only.
2. Generate deterministic synthetic entities from a provided random.Random instance.
3. Support configurable counts.
4. IDs must follow these formats:
   - ACC0000001
   - DEV0000001
   - BANK0000001
   - CLS000001
5. Time generation must support a global date range.
6. Country, KYC level, account type, device type, OS, and IP risk distributions must be configurable.
7. Output code should be clean and modular.
Output:
- full source code for the modules
- short explanation of design choices

Prompt C：生成正常行为生成器
# Prompt - Generate Normal Behavior Generators
Generate the normal behavior generation modules for fraud-data-generator.
Modules to implement:
- normal_behavior.py
- login_behavior.py
- transaction_behavior.py
- security_behavior.py
- graph_behavior.py
Purpose:
Generate realistic normal account behavior for multiple normal user segments.
Normal user segments:
- low_activity_normal
- medium_activity_normal
- high_activity_normal
- dormant_accounts
- new_users
Requirements:
1. Each segment must have distinct distributions for:
   - login frequency
   - transaction frequency
   - device usage
   - IP reuse
   - security events
2. Generate:
   - login_logs rows
   - login_sessions rows
   - transactions rows
   - deposits rows
   - withdrawals rows
   - transfers rows
   - security_events rows
3. Time distributions should look natural, with more daytime than nighttime activity.
4. Avoid purely uniform random behavior.
5. Use standard library only.
6. Output rows should be ready to write directly to CSV.
Output:
- full source code
- explanation of how segments differ

Prompt D：生成欺诈场景注入模块
# Prompt - Generate Fraud Scenario Injectors
Generate the fraud scenario injector modules for fraud-data-generator.
Modules to implement:
- fraud_base.py
- promotion_abuse.py
- credential_stuffing.py
- account_takeover.py
- money_mule.py
- arbitrage.py
- collusive_ring.py
- scenario_allocator.py
- label_builder.py
Purpose:
Inject realistic fraud patterns into a mostly normal synthetic dataset.
Fraud scenarios:
- promotion_abuse
- credential_stuffing
- account_takeover
- money_mule
- arbitrage
- collusive_ring
Requirements:
1. Each fraud scenario must have:
   - scenario name
   - target account selection logic
   - behavior injection logic
   - graph relationship injection logic
   - fraud label generation logic
2. Some scenarios may overlap, especially credential_stuffing and account_takeover.
3. Fraud scenarios should generate realistic event chains, not isolated random anomalies.
4. The generator must support configurable fraud ratios and scenario distribution.
5. Use standard library only.
6. Keep the logic explainable and deterministic.
Output:
- full source code
- concise explanation of each scenario’s event chain

Prompt E：生成数据集编排器
# Prompt - Generate Dataset Builder and CSV Orchestrator
Generate the dataset orchestration layer for fraud-data-generator.
Modules to implement:
- dataset_builder.py
- csv_writer.py
- main.py
Purpose:
Coordinate the full generation flow and write all CSV files.
Generation order:
1. accounts
2. account profiles / balances / limits
3. devices
4. IPs
5. account-device mappings
6. bank accounts
7. normal login behavior
8. normal transactions
9. normal security events
10. fraud scenario allocation
11. fraud behavior injection
12. graph edge generation
13. fraud labels
14. CSV output
Requirements:
1. Use chunked streaming writes to CSV.
2. Do not keep all event rows in memory if avoidable.
3. Support dataset sizes:
   - small
   - medium
   - large
4. Support CLI args:
   - --config
   - --size
   - --seed
   - --output-dir
5. Print progress logs.
6. Validate config before generation.
Output:
- full source code
- sample command lines
- explanation of streaming strategy

Prompt F：生成字段与 CSV schema 常量
# Prompt - Generate CSV Schema Constants and Validation
Generate Python modules that define CSV schema metadata and validation rules for fraud-data-generator.
Purpose:
Ensure every output file has a stable column order and consistent field validation.
Requirements:
1. Create constants for each CSV file’s columns.
2. Add lightweight validation for:
   - required fields
   - timestamp ordering
   - numeric ranges
   - enum values
3. Validation should run during generation where practical without large performance cost.
4. Use standard library only.
Output:
- full source code
- schema constants
- validation helpers

Prompt G：生成 README 和导入说明
# Prompt - Generate README and PostgreSQL Import Guide
Generate a README for fraud-data-generator.
Include:
1. Project purpose
2. Generated output files
3. Configuration files
4. How to run the generator
5. Meaning of small / medium / large dataset profiles
6. Fraud scenario descriptions
7. PostgreSQL COPY import examples for all major CSV files
8. Notes on deterministic generation
9. Notes on scalability and memory usage
Constraints:
- Keep the guide practical
- Assume the reader wants to generate data and import it quickly
Output:
- full README.md

4. 推荐执行顺序
建议你后续让 AI 按下面顺序生成：
1. Prompt A - project skeleton
2. Prompt B - base entity generators
3. Prompt C - normal behavior generators
4. Prompt D - fraud scenario injectors
5. Prompt F - CSV schema constants and validation
6. Prompt E - dataset builder and orchestrator
7. Prompt G - README and import guide

5. 下一步最有价值的内容
你现在已经有：
    • 反欺诈系统架构
    • AI Prompt 套件
    • 100+ feature vector 设计
    • 25表模拟数据模型
    • 欺诈场景规范
    • 数据生成器代码生成 Prompt
下一步最有价值的是把这两份也补齐：
A. feature_vector_spec.md
把前面那 100+ 特征整理成正式规范文件，包括：
    • 字段名
    • 类型
    • SQL口径
    • 窗口定义
    • 是否用于规则
    • 是否用于 ML
B. generator_config_spec.md
把生成器配置正式化，包括：
    • small / medium / large 三档
    • 每类欺诈参数
    • 正常账户分层占比
    • 行数目标
    • 资源占用建议
这两份一旦完成，AI 生成代码时几乎不会偏。
