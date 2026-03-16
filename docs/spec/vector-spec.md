1. 文档目标
本规范定义反欺诈系统中的账户级特征向量（Account Feature Vector），用于：
    • 风险规则判定
    • 异常检测模型输入
    • 图谱风险分析结果回写
    • 调查案件解释
    • AI 风险报告生成
本规范约束：
    • 特征实体类型：ACCOUNT
    • 主要存储位置：account_feature_snapshot / account_feature_history
    • 计算方式：以 batch 为主，后续支持 incremental
    • 默认时间基准：as_of_time
    • 未特殊说明时，所有窗口均为 rolling window

2. 通用字段规范
每个特征需要具备以下元信息：
字段	含义
feature_name	特征名
data_type	数据类型
category	特征类别
definition	业务定义
sql_semantics	SQL 计算口径
source_tables	来源表
window	时间窗口
null_default	空值默认值
consumers	使用方：rule / ml / graph / report
version	特征版本

3. 时间窗口规范
统一使用如下窗口：
窗口标识	含义
1h	最近1小时
24h	最近24小时
7d	最近7天
30d	最近30天
all	全历史
所有窗口以 as_of_time 为右边界：
(event_time > as_of_time - window) AND (event_time <= as_of_time)

4. 空值与默认值规范
为了保证规则和模型稳定，建议：
类型	默认值
INT	0
DOUBLE	0.0
BOOLEAN	false
STRING	空字符串或固定枚举 UNKNOWN
说明：
    • “没有事件”通常不是 NULL，而是 0
    • 比率分母为 0 时，统一输出 0.0
    • 特征计算异常应记录任务失败，不应 silently 输出空值

5. 特征分类总览
建议第一版分成 8 大类：
类别	说明	目标数量
account_base	账户基础属性	10
login_behavior	登录行为	20
transaction_behavior	交易行为	30
device_behavior	设备行为	15
security_behavior	安全行为	15
temporal_behavior	时间节奏行为	10
graph_features	图谱关系特征	15
statistical_anomaly	统计偏离特征	10
总量约：125

6. account_base 特征
这些特征主要来自 accounts, account_profiles, account_limits, ip_intelligence。

6.1 account_age_days
    • data_type: INT
    • definition: 账户创建到 as_of_time 的天数
    • source_tables: accounts
    • window: N/A
    • null_default: 0
    • consumers: rule, ml, report
SQL口径：
EXTRACT(DAY FROM (? - created_at))

6.2 account_age_hours
    • data_type: INT
    • definition: 账户创建到 as_of_time 的小时数
    • consumers: rule, ml

6.3 kyc_level_numeric
    • data_type: INT
    • definition: KYC等级数值化
    • source_tables: accounts
    • mapping:
        ○ 0 = none
        ○ 1 = basic
        ○ 2 = standard
        ○ 3 = enhanced

6.4 account_country_risk_score
    • data_type: DOUBLE
    • definition: 账户所属国家风险分
    • source_tables: accounts + 内部国家风险映射
    • consumers: rule, ml
说明：第一版可以用静态映射表，不必接外部情报源。

6.5 registration_hour
    • data_type: INT
    • definition: 注册时间小时（0-23）
    • consumers: ml, report

6.6 registration_day_of_week
    • data_type: INT
    • definition: 注册时间星期（1-7）

6.7 is_new_account_24h
    • data_type: BOOLEAN
    • definition: 账户年龄是否小于等于24小时
    • consumers: rule, ml

6.8 is_new_account_7d
    • data_type: BOOLEAN
    • definition: 账户年龄是否小于等于7天

6.9 account_status_flag
    • data_type: INT
    • definition: 状态数值化
    • mapping:
        ○ active = 1
        ○ suspended = 2
        ○ locked = 3
        ○ other = 9

6.10 registration_ip_risk_score
    • data_type: DOUBLE
    • definition: 注册IP对应的风险分
    • source_tables: accounts.registration_ip + ip_intelligence

7. login_behavior 特征
来源：login_logs, login_failures, login_sessions, ip_intelligence

7.1 login_count_1h
    • data_type: INT
    • definition: 最近1小时登录总次数
    • source_tables: login_logs
    • window: 1h
    • consumers: rule, ml
SQL口径：
COUNT(*) FILTER (
  WHERE login_time > ? - INTERVAL '1 hour'
    AND login_time <= ?
)

7.2 login_count_24h
同上，窗口为 24h。

7.3 login_count_7d
同上，窗口为 7d。

7.4 login_success_count_24h
    • definition: 最近24小时成功登录次数
    • sql_semantics: success = true

7.5 login_failure_count_24h
    • definition: 最近24小时失败登录次数
    • sql_semantics: success = false

7.6 login_failure_rate_24h
    • data_type: DOUBLE
    • definition: 最近24小时失败登录 / 总登录尝试
    • formula:
login_failure_count_24h / (login_success_count_24h + login_failure_count_24h)
分母为 0 时输出 0.0。

7.7 unique_ip_count_24h
    • definition: 最近24小时登录IP去重数
COUNT(DISTINCT ip)

7.8 unique_ip_count_7d
窗口 7d。

7.9 high_risk_ip_login_count_24h
    • definition: 最近24小时从高风险 IP 登录的次数
    • source_tables: login_logs + ip_intelligence
    • join logic: ip_intelligence.risk_level = 'high'

7.10 vpn_ip_login_count_24h
    • definition: 最近24小时 VPN IP 登录次数

7.11 proxy_ip_login_count_24h
    • definition: 最近24小时代理 IP 登录次数

7.12 datacenter_ip_login_count_24h
    • definition: 最近24小时数据中心 IP 登录次数

7.13 ip_switch_count_24h
    • data_type: INT
    • definition: 最近24小时登录记录中，相邻登录 IP 变化次数
    • 实现建议: 用窗口函数
    • consumers: ml, report

7.14 country_switch_count_7d
    • definition: 最近7天登录国家切换次数
    • 依赖: login_logs.ip -> ip_addresses.country

7.15 impossible_travel_flag_7d
    • data_type: BOOLEAN
    • definition: 最近7天是否存在不合理跨国跳跃登录
    • 第一版简化实现:
        ○ 若相邻两次成功登录国家不同
        ○ 且间隔 < 2小时
        ○ 则为 true

7.16 unique_device_count_login_24h
    • definition: 最近24小时登录用过的设备数

7.17 new_device_login_count_7d
    • definition: 最近7天使用“此前未见设备”的登录次数
    • 说明: 需要与历史 account_devices 比较

7.18 device_switch_count_login_24h
    • definition: 最近24小时相邻登录设备变化次数

7.19 night_login_ratio_7d
    • data_type: DOUBLE
    • definition: 最近7天夜间登录占比
    • 夜间定义: 00:00–06:00
    • formula:
night_logins / total_logins

7.20 weekend_login_ratio_30d
    • definition: 最近30天周末登录占比

8. transaction_behavior 特征
来源：transactions, deposits, withdrawals, transfers

8.1 transaction_count_1h
    • 最近1小时交易总数

8.2 transaction_count_24h
    • 最近24小时交易总数

8.3 transaction_count_7d
    • 最近7天交易总数

8.4 deposit_count_24h
    • 最近24小时入金次数
    • transaction_type in ('deposit', 'transfer_in') 或 deposits

8.5 withdraw_count_24h
    • 最近24小时出金次数
    • transaction_type in ('withdraw', 'transfer_out')

8.6 total_amount_1h
    • 最近1小时交易总金额（绝对值求和）

8.7 total_amount_24h
    • 最近24小时交易总金额

8.8 total_amount_7d
    • 最近7天交易总金额

8.9 avg_transaction_amount_24h
    • 最近24小时平均交易金额

8.10 max_transaction_amount_24h
    • 最近24小时最大单笔交易金额

8.11 min_transaction_amount_24h
    • 最近24小时最小单笔交易金额

8.12 transaction_amount_std_7d
    • 最近7天交易金额标准差
    • 说明: PostgreSQL 可用 STDDEV_POP

8.13 deposit_amount_24h
    • 最近24小时入金金额总和

8.14 withdraw_amount_24h
    • 最近24小时出金金额总和

8.15 deposit_withdraw_ratio_24h
    • definition: 出金金额 / 入金金额，或入出比例固定选一种
    • 建议统一:
withdraw_amount_24h / deposit_amount_24h
分母为 0 时输出 0.0。

8.16 unique_counterparty_count_24h
    • 最近24小时对手方去重数

8.17 unique_counterparty_count_7d
    • 最近7天对手方去重数

8.18 new_counterparty_count_7d
    • 最近7天首次出现的对手方数量
    • 需要和更早历史比较

8.19 counterparty_reuse_ratio_7d
    • definition: 重复对手方交易占比
    • 近似实现:
1 - unique_counterparty_count_7d / transaction_count_7d

8.20 avg_transaction_interval_minutes_24h
    • 最近24小时相邻交易平均间隔（分钟）

8.21 transaction_interval_std_24h
    • 最近24小时相邻交易间隔标准差

8.22 rapid_transactions_flag_1h
    • data_type: BOOLEAN
    • definition: 最近1小时交易密度是否超过阈值
    • 第一版阈值可配置，例如 transaction_count_1h >= 20

8.23 withdraw_after_deposit_delay_avg_24h
    • definition: 最近24小时内，入金后最近一次出金的平均间隔分钟数
    • 说明: 这是高价值反欺诈特征
    • 实现建议: 先用中间表或临时 CTE 配对，不要内嵌过度复杂 SQL

8.24 rapid_withdraw_after_deposit_flag_24h
    • definition: 是否存在“入金后X分钟内提现”
    • 阈值建议: 30分钟，可配置

8.25 small_amount_high_freq_ratio_24h
    • definition: 小额交易占比
    • 小额阈值: 按币种配置，第一版可统一金额阈值
    • formula:
small_amount_txn_count / transaction_count_24h

8.26 round_number_transaction_ratio_7d
    • definition: 金额为整数百/千等“整额”交易占比
    • 用途: 套利、机械化交易

8.27 repeated_amount_ratio_7d
    • definition: 重复金额交易占比
    • 近似实现:
        ○ 统计金额出现次数 > 1 的交易数 / 总交易数

8.28 reward_transaction_count_30d
    • definition: 最近30天奖励/活动类交易次数
    • 用途: 羊毛党识别

8.29 reward_amount_30d
    • 最近30天奖励金额总和

8.30 reward_withdraw_delay_avg_30d
    • 奖励到账到提现的平均延迟
    • 高价值，适合羊毛党规则

9. device_behavior 特征
来源：account_devices, devices, device_fingerprint, login_logs

9.1 unique_device_count_24h
    • 最近24小时活跃设备数

9.2 unique_device_count_7d
    • 最近7天活跃设备数

9.3 unique_device_count_30d
    • 最近30天活跃设备数

9.4 new_device_count_7d
    • 最近7天首次出现的新设备数

9.5 device_switch_count_24h
    • 最近24小时设备切换次数

9.6 device_switch_rate_7d
    • definition: 设备切换次数 / 登录次数
    • 分母 0 则 0.0

9.7 shared_device_accounts_7d
    • definition: 最近7天与当前账户共享设备的其他账户数量
    • 来源: account_devices
    • 高价值图前置特征

9.8 shared_device_high_risk_accounts_30d
    • definition: 共享设备账户中，已标记高风险账户数量
    • 来源: account_devices + risk_scores / fraud_labels

9.9 device_cluster_size_30d
    • definition: 账户所在设备簇大小
    • 第一版可近似为：该账户所有设备关联的去重账户数

9.10 mobile_device_ratio_30d
    • 最近30天设备中 mobile 占比

9.11 desktop_device_ratio_30d
    • 最近30天设备中 desktop 占比

9.12 emulator_device_flag_30d
    • BOOLEAN
    • definition: 是否出现模拟器/异常设备指纹
    • 第一版若无真实字段，可先留空或由模拟数据提供

9.13 device_usage_variance_30d
    • definition: 各设备使用频次的离散程度
    • 可用于区分“主设备稳定”与“批量切换设备”

9.14 identical_fingerprint_account_count_30d
    • definition: 共享近似相同 fingerprint 的账户数
    • 说明: 若第一版 fingerprint 数据不足，可延后实现

9.15 suspicious_device_reuse_flag_30d
    • BOOLEAN
    • definition: 共享设备账户数是否超过阈值

10. security_behavior 特征
来源：security_events, password_resets

10.1 password_change_count_24h
    • 最近24小时密码修改次数

10.2 password_change_count_7d
    • 最近7天密码修改次数

10.3 password_reset_count_7d
    • 最近7天密码重置次数

10.4 email_change_count_7d
    • 最近7天邮箱修改次数

10.5 phone_change_count_7d
    • 最近7天手机号修改次数

10.6 two_factor_disable_count_30d
    • 最近30天关闭2FA次数

10.7 two_factor_enable_count_30d
    • 最近30天启用2FA次数

10.8 security_event_count_24h
    • 最近24小时安全事件总数

10.9 security_event_count_7d
    • 最近7天安全事件总数

10.10 rapid_profile_change_flag_24h
    • BOOLEAN
    • definition: 最近24小时安全资料变更次数是否超过阈值

10.11 profile_change_after_login_flag_24h
    • BOOLEAN
    • definition: 是否存在登录后短时间内资料变更
    • 阈值建议: 30 分钟

10.12 security_change_before_withdraw_flag_24h
    • BOOLEAN
    • definition: 是否存在安全事件后短时间内提现

10.13 security_event_burst_score_24h
    • DOUBLE
    • definition: 短时间安全事件密度评分
    • 第一版可简化为计数标准化值

10.14 first_security_event_after_dormancy_flag
    • BOOLEAN
    • definition: 长期沉默账户首次活跃即出现安全事件

10.15 suspicious_recovery_pattern_flag_7d
    • BOOLEAN
    • definition: 是否存在“密码重置 + 新设备 + 提现”的组合模式

11. temporal_behavior 特征
这些是节奏和分布特征。

11.1 night_activity_ratio_7d
    • 最近7天所有事件中夜间事件占比

11.2 weekend_activity_ratio_30d
    • 最近30天周末事件占比

11.3 activity_hour_entropy_30d
    • DOUBLE
    • definition: 按小时分布的信息熵
    • 用途: 区分自然行为与脚本化行为
    • 第一版可在 Java 中计算，不一定纯 SQL

11.4 activity_day_entropy_30d
    • 按日期/星期分布熵

11.5 avg_event_interval_minutes_24h
    • 最近24小时相邻事件平均间隔

11.6 event_interval_std_24h
    • 最近24小时相邻事件间隔标准差

11.7 burst_activity_flag_24h
    • BOOLEAN
    • definition: 是否存在短时间高密度事件爆发

11.8 periodic_pattern_score_7d
    • DOUBLE
    • definition: 行为节奏是否规则化
    • 第一版可简化为“间隔标准差越低分越高”

11.9 inactivity_then_spike_flag_7d
    • BOOLEAN
    • definition: 长时间沉默后突然活动暴增

11.10 session_duration_avg_7d
    • 最近7天平均会话时长
    • 来源：login_sessions

12. graph_features 特征
这些特征由图模块或关系查询得到，最终回写 Feature Store。

12.1 shared_ip_accounts_7d
    • 最近7天共享IP的其他账户数

12.2 shared_bank_accounts_30d
    • 共享银行卡/收款账户的其他账户数

12.3 transaction_neighbor_count_30d
    • 近30天转账网络邻居数

12.4 graph_degree_30d
    • 图度数

12.5 graph_cluster_size_30d
    • 所在连通簇大小

12.6 risk_neighbor_count_30d
    • 邻居中高风险账户数

12.7 two_hop_risk_neighbor_count_30d
    • 二跳高风险邻居数

12.8 three_hop_risk_neighbor_count_30d
    • 三跳高风险邻居数

12.9 graph_density_score_30d
    • 所在局部子图密度评分

12.10 graph_pagerank_score_30d
    • 图中心性指标
    • 第一版可延后

12.11 distance_to_fraud_account_min_30d
    • 到已确认欺诈账户的最短距离
    • 不存在可输出大值或固定 sentinel

12.12 funnel_in_degree_7d
    • 最近7天作为资金汇集点的入边数

12.13 funnel_out_degree_7d
    • 最近7天作为资金分发点的出边数

12.14 collector_account_flag_7d
    • BOOLEAN
    • 是否表现为收口账户

12.15 cluster_risk_score_30d
    • DOUBLE
    • 所在簇的综合风险分
    • 由图模块计算后回写

13. statistical_anomaly 特征
这些特征是统计偏离结果，不一定来自 Python ML，可先由 SQL/Java 统计得到。

13.1 zscore_login_count_24h
    • 登录次数相对同群体均值的 z-score
    • 第一版可按全量或分层账户群体

13.2 zscore_transaction_count_24h
    • 交易次数 z-score

13.3 zscore_total_amount_24h
    • 交易金额 z-score

13.4 zscore_unique_device_count_7d
    • 设备数 z-score

13.5 zscore_shared_device_accounts_7d
    • 共享设备账户数 z-score

13.6 behavior_deviation_score_7d
    • DOUBLE
    • 多个关键特征偏离度聚合分
    • 第一版可由 Java 风险侧计算并回写

13.7 anomaly_score_last
    • 最新一次 Python ML 异常分

13.8 anomaly_score_avg_7d
    • 最近7天平均异常分

13.9 anomaly_score_std_7d
    • 最近7天异常分标准差

13.10 anomaly_rank_percentile_24h
    • 当日异常排序百分位

14. 第一阶段建议上线的核心特征
虽然总设计有 100+，但第一阶段不应全部实现。
建议先上 40 个左右的核心特征。

14.1 第一批基础特征
- account_base
    • account_age_days
    • kyc_level_numeric
    • is_new_account_24h
    • is_new_account_7d
    • registration_ip_risk_score
- login_behavior
    • login_count_24h
    • login_failure_count_24h
    • login_failure_rate_24h
    • unique_ip_count_24h
    • high_risk_ip_login_count_24h
    • vpn_ip_login_count_24h
    • new_device_login_count_7d
    • night_login_ratio_7d
- transaction_behavior
    • transaction_count_24h
    • total_amount_24h
    • avg_transaction_amount_24h
    • deposit_count_24h
    • withdraw_count_24h
    • deposit_amount_24h
    • withdraw_amount_24h
    • deposit_withdraw_ratio_24h
    • unique_counterparty_count_24h
    • withdraw_after_deposit_delay_avg_24h
    • rapid_withdraw_after_deposit_flag_24h
    • reward_transaction_count_30d
    • reward_withdraw_delay_avg_30d
- device_behavior
    • unique_device_count_7d
    • device_switch_count_24h
    • shared_device_accounts_7d
    • suspicious_device_reuse_flag_30d
- security_behavior
    • password_change_count_7d
    • security_event_count_24h
    • rapid_profile_change_flag_24h
    • security_change_before_withdraw_flag_24h
- graph_features
    • shared_ip_accounts_7d
    • shared_bank_accounts_30d
    • graph_cluster_size_30d
    • risk_neighbor_count_30d
- anomaly
    • anomaly_score_last
这批已经足够支撑：
    • 基础规则
    • 第一版 ML
    • AI 调查报告

15. Feature Store 表分组建议
为了便于 Spring JDBC 映射与维护，建议宽表按逻辑分组排序：
    1. 主键与版本字段
    2. account_base
    3. login_behavior
    4. transaction_behavior
    5. device_behavior
    6. security_behavior
    7. temporal_behavior
    8. graph_features
    9. anomaly/statistical
这样：
    • Java RowMapper 易维护
    • SQL update 语句更清楚
    • 问题排查直观

16. 消费方映射建议
规则引擎优先消费
    • account_age_days
    • login_failure_rate_24h
    • high_risk_ip_login_count_24h
    • rapid_withdraw_after_deposit_flag_24h
    • shared_device_accounts_7d
    • security_change_before_withdraw_flag_24h
    • graph_cluster_size_30d
    • risk_neighbor_count_30d
ML 服务优先消费
    • login_count_24h
    • unique_ip_count_24h
    • unique_device_count_7d
    • transaction_count_24h
    • total_amount_24h
    • deposit_withdraw_ratio_24h
    • repeated_amount_ratio_7d
    • device_switch_count_24h
    • security_event_count_24h
    • graph_cluster_size_30d
AI 报告优先引用
    • account_age_days
    • high_risk_ip_login_count_24h
    • new_device_login_count_7d
    • withdraw_after_deposit_delay_avg_24h
    • shared_device_accounts_7d
    • graph_cluster_size_30d
    • risk_neighbor_count_30d
    • anomaly_score_last

17. SQL实现建议
生产实现里，不建议一个巨大 SQL 一次算完所有特征。
建议拆成多个 calculator：
    • LoginFeatureCalculator
    • TransactionFeatureCalculator
    • DeviceFeatureCalculator
    • SecurityFeatureCalculator
    • GraphFeatureCalculator
每个 calculator：
    • 查询原始聚合结果
    • 生成局部特征对象
    • 最终 merge 成 AccountFeatureSnapshot
这样更适合 Spring JDBC，也更容易测试。

18. 版本管理建议
建议从 feature_version = 1 开始。
以下情况必须升版本：
    • 定义公式变更
    • 时间窗口变更
    • 数据源变更
    • null/default 处理变更
    • 统计口径变更
不要在不升版本的情况下偷偷改口径。

19. 最终落地建议
你现在最适合的下一步不是继续抽象，而是落到两份工程化产物：
A. feature_store_schema.sql
包括：
    • feature_definition
    • account_feature_snapshot
    • account_feature_history
    • feature_generation_job
    • 必要索引
B. fraud-feature 模块代码生成 Prompt
直接让 AI 生成：
    • feature definition model
    • calculators
    • repositories
    • batch job
    • FeatureStoreService
这两份一落地，你的 Feature Store 就从“设计”进入“可开发”阶段了。
下一条我可以直接继续给你这两份：
1. feature_store_schema.sql
2. fraud-feature 模块的生产级 AI 代码生成 Prompt
