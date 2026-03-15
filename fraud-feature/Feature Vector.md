目标是：在不依赖复杂模型的情况下，仅凭特征工程就能发现大部分异常账户。这也是很多金融风控系统的实践方式。
设计原则：
    • 可解释
    • 稳定
    • 易于SQL计算
    • 支持时间窗口
    • 支持图关系
    • 支持ML
所有特征都可以通过 SQL + Spring JDBC 计算。

# 一、Feature Vector 总体结构
建议统一存储为：
{
  "account_id": "A123",
  "generated_at": "2026-03-13T10:00:00Z",
  "features": {
    ...
  }
}
数据库建议：
features
---------
account_id
generated_at
feature_json

# 二、特征分类
总共约 120+特征，分为：
类别	数量
登录行为	20
交易行为	30
设备行为	20
账户安全行为	15
时间行为	10
关系图谱	15
账户基础属性	10
异常统计	10

# 三、账户基础特征（10）
这些特征来自 account profile。
account_age_days
account_age_hours
kyc_level_numeric
account_country_risk_score
registration_hour
registration_day_of_week
is_new_account_24h
is_new_account_7d
account_status_flag
registration_ip_risk_score
用途：
    • 新账号欺诈
    • 国家风险
    • 注册时间异常

# 四、登录行为特征（20）
来自 LOGIN_EVENT。
## 计数类
login_count_1h
login_count_24h
login_count_7d
login_success_count_24h
login_failure_count_24h
login_failure_rate_24h
## IP类
unique_ip_count_24h
unique_ip_count_7d
high_risk_ip_login_count
vpn_ip_login_count
ip_switch_count_24h
## 地理行为
country_switch_count
geo_distance_km_last_login
impossible_travel_flag
## 设备登录
unique_device_count_login_24h
device_switch_count_login
new_device_login_count
## 登录模式
night_login_ratio
weekend_login_ratio
login_interval_variance

# 五、交易行为特征（30）
来自 TRANSACTION_EVENT。
## 交易计数
transaction_count_1h
transaction_count_24h
transaction_count_7d
deposit_count_24h
withdraw_count_24h
## 交易金额
total_amount_1h
total_amount_24h
total_amount_7d
avg_transaction_amount
max_transaction_amount
min_transaction_amount
transaction_amount_std
## 交易节奏
avg_transaction_interval
transaction_interval_std
rapid_transactions_flag
## 入金出金行为
deposit_amount_24h
withdraw_amount_24h
deposit_withdraw_ratio
withdraw_after_deposit_delay_avg
rapid_withdraw_after_deposit_flag
## 对手方
unique_counterparty_count
new_counterparty_count
counterparty_reuse_ratio
## 可疑交易模式
small_amount_high_freq_ratio
round_number_transaction_ratio
repeated_amount_ratio

# 六、设备行为特征（20）
来自 DEVICE_EVENT。
## 设备数量
unique_device_count_24h
unique_device_count_7d
new_device_count_7d
## 设备切换
device_switch_count_24h
device_switch_rate
device_reuse_ratio
## 设备共享
shared_device_accounts
shared_device_high_risk_accounts
device_cluster_size
## 设备类型
mobile_device_ratio
desktop_device_ratio
emulator_device_flag
设备行为异常
device_login_pattern_entropy
device_usage_variance

# 七、账户安全行为特征（15）
来自 PROFILE_CHANGE_EVENT。
password_change_count_24h
password_change_count_7d
email_change_count_7d
phone_change_count_7d
2fa_disable_flag
2fa_enable_flag
profile_change_after_login_flag
security_change_before_withdraw_flag
rapid_profile_change_flag
security_event_count_24h
security_event_count_7d

# 八、时间行为特征（10）
用于检测自动化行为。
activity_hour_entropy
activity_day_entropy
night_activity_ratio
weekend_activity_ratio
activity_variance
burst_activity_flag
avg_event_interval
event_interval_std
periodic_pattern_score

# 九、关系图谱特征（15）
来自 fraud-graph 模块。
shared_device_accounts
shared_ip_accounts
shared_bank_accounts
transaction_neighbor_count
transaction_cluster_size
graph_degree
graph_pagerank
distance_to_fraud_account
risk_neighbor_count
cluster_density
cluster_risk_score
two_hop_risk_neighbors
three_hop_risk_neighbors

# 十、异常统计特征（10）
来自 ML 或统计。
zscore_transaction_amount
zscore_login_count
zscore_device_count
feature_vector_norm
feature_vector_distance_mean
anomaly_score_last
anomaly_score_avg_7d
anomaly_score_std
behavior_deviation_score

# 十一、最终 Feature Vector 示例
{
  "account_id": "A123",
  "generated_at": "2026-03-13T10:00:00Z",
  "features": {
    "account_age_days": 2,
    "login_count_24h": 18,
    "unique_ip_count_24h": 7,
    "device_switch_count_24h": 5,
    "transaction_count_24h": 12,
    "total_amount_24h": 8500,
    "withdraw_after_deposit_delay_avg": 3,
    "shared_device_accounts": 6,
    "night_activity_ratio": 0.78,
    "cluster_size": 14,
    "risk_neighbor_count": 2
  }
}

# 十二、特征生成策略（SQL可实现）
例如：
## login_count_24h
SELECT count(*)
FROM events
WHERE account_id = ?
AND event_type = 'LOGIN_EVENT'
AND event_time > now() - interval '24 hours';

## unique_ip_count
SELECT count(DISTINCT payload->>'ip')
FROM events
WHERE account_id = ?
AND event_type='LOGIN_EVENT'
AND event_time > now() - interval '24 hours';

## withdraw_after_deposit_delay
SELECT avg(extract(epoch from withdraw_time - deposit_time)/60)
FROM deposit_withdraw_pairs
WHERE account_id = ?

# 十三、推荐 Feature 数量
经验值：
|阶段|特征数|
|MVP|30–40|
|初级系统|80–120|
|成熟系统|200–500|
