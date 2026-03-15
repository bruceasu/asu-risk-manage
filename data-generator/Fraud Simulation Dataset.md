下面给出一套 生产级反欺诈模拟数据模型（≈25张表），用于在没有真实数据时构建 Fraud Simulation Dataset。设计目标：
    • 支持 100k+ accounts
    • 支持 10M+ events
    • 覆盖常见欺诈模式
        ○ 羊毛党（promotion abuse）
        ○ 撞库（credential stuffing）
        ○ 盗号（account takeover）
        ○ 洗钱 / 跑分（money mule）
        ○ 套利（arbitrage / bonus abuse）
        ○ 团伙（collusive rings）
同时满足：
    • SQL可生成
    • Feature工程可直接使用
    • Graph分析可直接使用
    • ML训练可直接使用
数据库假设：PostgreSQL

# 一、总体数据模型
表数量：25
主要类别：
类别	表数量
账户	4
设备	4
登录	3
交易	5
关系图谱	3
安全事件	2
风险标签	2
辅助数据	2

# 二、账户数据
## 1 accounts
```sql
CREATE TABLE accounts (
    account_id VARCHAR(64) PRIMARY KEY,
    account_type VARCHAR(16),
    country VARCHAR(8),
    kyc_level INT,
    created_at TIMESTAMP,
    registration_ip VARCHAR(64),
    status VARCHAR(16)
);
```
规模：
100,000 rows

## 2 account_profiles
```sql
CREATE TABLE account_profiles (
    account_id VARCHAR(64),
    email VARCHAR(128),
    phone VARCHAR(32),
    birth_year INT,
    risk_score INT DEFAULT 0
);
```

## 3 account_balance
```sql
CREATE TABLE account_balance (
    account_id VARCHAR(64),
    currency VARCHAR(8),
    balance NUMERIC(18,2),
    updated_at TIMESTAMP
);
```

## 4 account_limits
```sql
CREATE TABLE account_limits (
    account_id VARCHAR(64),
    daily_withdraw_limit NUMERIC,
    daily_transfer_limit NUMERIC
);
```

# 三、设备数据
## 5 devices
```sql
CREATE TABLE devices (
    device_id VARCHAR(64) PRIMARY KEY,
    device_type VARCHAR(32),
    os VARCHAR(32),
    created_at TIMESTAMP
);
```
## 6 device_fingerprint
```sql
CREATE TABLE device_fingerprint (
    device_id VARCHAR(64),
    browser VARCHAR(32),
    os_version VARCHAR(32),
    screen_resolution VARCHAR(32),
    timezone VARCHAR(32)
);
```
## 7 account_devices
```sql
CREATE TABLE account_devices (
    account_id VARCHAR(64),
    device_id VARCHAR(64),
    first_seen TIMESTAMP,
    last_seen TIMESTAMP
);
```
这个表非常关键。
用于：
shared_device_accounts
device clusters

## 8 device_clusters
```sql
CREATE TABLE device_clusters (
    cluster_id VARCHAR(64),
    device_id VARCHAR(64)
);
```
用于模拟：
device farms

# 四、登录行为
## 9 login_logs
```sql
CREATE TABLE login_logs (
    id BIGSERIAL PRIMARY KEY,
    account_id VARCHAR(64),
    device_id VARCHAR(64),
    ip VARCHAR(64),
    success BOOLEAN,
    login_time TIMESTAMP
);
```
规模：
5M rows

## 10 login_failures
```sql
CREATE TABLE login_failures (
    id BIGSERIAL,
    account_id VARCHAR(64),
    ip VARCHAR(64),
    attempt_time TIMESTAMP
);
```
用于模拟：
credential stuffing

## 11 login_sessions
```sql
CREATE TABLE login_sessions (
    session_id VARCHAR(64),
    account_id VARCHAR(64),
    device_id VARCHAR(64),
    login_time TIMESTAMP,
    logout_time TIMESTAMP
);
```
# 五、交易数据
## 12 transactions
```sql
CREATE TABLE transactions (
    transaction_id BIGSERIAL PRIMARY KEY,
    account_id VARCHAR(64),
    counterparty_account VARCHAR(64),
    amount NUMERIC(18,2),
    currency VARCHAR(8),
    transaction_type VARCHAR(16),
    created_at TIMESTAMP
);
```
规模：
3M rows

## 13 deposits
```sql
CREATE TABLE deposits (
    deposit_id BIGSERIAL,
    account_id VARCHAR(64),
    amount NUMERIC,
    source VARCHAR(32),
    created_at TIMESTAMP
);
```

## 14 withdrawals
```sql
CREATE TABLE withdrawals (
    withdraw_id BIGSERIAL,
    account_id VARCHAR(64),
    amount NUMERIC,
    target VARCHAR(64),
    created_at TIMESTAMP
);
```
## 15 transfers
```sql
CREATE TABLE transfers (
    transfer_id BIGSERIAL,
    from_account VARCHAR(64),
    to_account VARCHAR(64),
    amount NUMERIC,
    created_at TIMESTAMP
);
```

## 16 payment_methods
```sql
CREATE TABLE payment_methods (
    payment_id VARCHAR(64),
    account_id VARCHAR(64),
    method_type VARCHAR(32),
    created_at TIMESTAMP
);
```
# 六、IP 情报数据
## 17 ip_addresses
```sql
CREATE TABLE ip_addresses (
    ip VARCHAR(64) PRIMARY KEY,
    country VARCHAR(8),
    isp VARCHAR(64)
);
```
## 18 ip_intelligence
```sql
CREATE TABLE ip_intelligence (
    ip VARCHAR(64),
    risk_level VARCHAR(16),
    is_vpn BOOLEAN,
    is_proxy BOOLEAN,
    is_datacenter BOOLEAN
);
```
# 七、安全事件
## 19 security_events
```sql
CREATE TABLE security_events (
    event_id BIGSERIAL,
    account_id VARCHAR(64),
    event_type VARCHAR(32),
    created_at TIMESTAMP
);
```
例如：
password_change
email_change
2fa_disable

# 20 password_resets
```sql
CREATE TABLE password_resets (
    reset_id BIGSERIAL,
    account_id VARCHAR(64),
    reset_time TIMESTAMP
);
```
# 八、图谱关系
## 21 account_graph_edges
```sql
CREATE TABLE account_graph_edges (
    from_account VARCHAR(64),
    to_account VARCHAR(64),
    edge_type VARCHAR(32),
    created_at TIMESTAMP
);
```
edge_type：
transaction
shared_device
shared_ip
shared_bank

## 22 account_clusters
```sql
CREATE TABLE account_clusters (
    cluster_id VARCHAR(64),
    account_id VARCHAR(64)
);
```
用于模拟：
fraud rings

## 23 bank_accounts
```sql
CREATE TABLE bank_accounts (
    bank_account_id VARCHAR(64),
    account_id VARCHAR(64),
    created_at TIMESTAMP
);
```
# 九、风险标签
## 24 fraud_labels
```sql
CREATE TABLE fraud_labels (
    account_id VARCHAR(64),
    fraud_type VARCHAR(32),
    labeled_at TIMESTAMP
);
```
fraud_type：
  - bonus_abuse
  - credential_stuffing
  - account_takeover
  - money_mule
  - collusion_ring

## 25 investigation_cases
```sql
CREATE TABLE investigation_cases (
    case_id BIGSERIAL,
    account_id VARCHAR(64),
    risk_score DOUBLE PRECISION,
    created_at TIMESTAMP
);
```
# 十、数据规模建议
|表|行数|
|accounts|100k|
|devices|120k|
|login_logs|5M|
|transactions|3M|
|transfers|1M|
|security_events|200k|
|graph_edges|500k|

总计：
≈10M events

# 十一、欺诈模式模拟
## 1 羊毛党
特点：
大量新账号
共享设备
领取奖励
提现
SQL生成：
cluster_size 20–50

## 2 撞库
特点：
login failures
多个IP

## 3 盗号
特点：
新设备登录
密码修改
提现

## 4 洗钱
特点：
多个账户
资金转移
最终汇集

## 5 套利
特点：
高频交易
小额
快速套利

## 6 团伙
特点：
shared device
shared ip
shared bank

# 十二、模拟数据生成规模
推荐：
accounts           100k
devices            120k
login_logs         5M
transactions       3M
transfers          1M
graph_edges        500k
fraud_accounts     2k

# 十三、最终数据流
simulation generator
      ↓
csv files
      ↓
PostgreSQL COPY
      ↓
events / transactions
      ↓
feature generation
      ↓
ML anomaly detection
      ↓
risk scoring
      ↓
investigation cases

