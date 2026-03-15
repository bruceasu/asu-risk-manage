# 输入数据（Input Data）

## 1. 账户基础数据（Account Profile Data）
这是账户的 静态或低频变化数据，通常来自用户系统或账户系统。
典型来源：
    • 用户注册系统
    • KYC系统
    • 账户管理系统
示例结构：
{
  "account_id": "A12345",
  "account_type": "individual",
  "country": "JP",
  "kyc_level": "basic",
  "account_status": "active",
  "registration_time": "2026-03-12T10:30:00Z",
  "registration_ip": "1.2.3.4"
}
典型字段：
字段	说明
account_id	账户ID
account_type	个人 / 企业
country	注册国家
kyc_level	KYC等级
registration_time	注册时间
registration_ip	注册IP
account_status	active / suspended
用途：
    • 构建 AccountProfile
    • 用于规则判断
    • 用于风险报告

## 2. 行为事件数据（核心输入）
这是系统最重要的输入数据。
来自：
    • 登录系统
    • 交易系统
    • 设备系统
    • 账户安全系统
统一进入 fraud-ingestion 模块。
统一事件格式：
{
  "account_id": "A123",
  "event_type": "LOGIN_EVENT",
  "event_time": "2026-03-13T08:00:00Z",
  "payload": {
    "ip": "10.1.2.3",
    "device_id": "device-991",
    "success": true
  }
}

### 2.1 登录事件（LOGIN_EVENT）
示例：
{
  "account_id": "A123",
  "event_type": "LOGIN_EVENT",
  "event_time": "2026-03-13T08:00:00Z",
  "payload": {
    "ip": "10.1.2.3",
    "device_id": "device-991",
    "success": true,
    "geo": "JP"
  }
}
用于计算：
loginCount24h
loginFailureRate
uniqueIpCount
deviceSwitchRate

### 2.2 交易事件（TRANSACTION_EVENT）
示例：
{
  "account_id": "A123",
  "event_type": "TRANSACTION_EVENT",
  "event_time": "2026-03-13T08:05:00Z",
  "payload": {
    "transaction_type": "deposit",
    "amount": 1000,
    "currency": "USD",
    "counterparty_account": "B889"
  }
}
用于计算：
transactionCount24h
totalTransactionAmount
withdrawDelayAfterDeposit
counterpartyCount

### 2.3 设备事件（DEVICE_EVENT）
示例：
{
  "account_id": "A123",
  "event_type": "DEVICE_EVENT",
  "event_time": "2026-03-13T08:02:00Z",
  "payload": {
    "device_id": "device-991",
    "device_type": "mobile",
    "os": "ios"
  }
}
用于：
uniqueDeviceCount
sharedDeviceAccounts
deviceSwitchRate

### 2.4 账户安全事件（PROFILE_CHANGE_EVENT）
示例：
{
  "account_id": "A123",
  "event_type": "PROFILE_CHANGE_EVENT",
  "event_time": "2026-03-13T08:03:00Z",
  "payload": {
    "change_type": "password_reset"
  }
}
用于检测：
rapidProfileChange
accountTakeoverPattern

## 3. 关系数据（Graph Data）
图谱分析需要账户之间的关联数据。
来源：
    • 交易系统
    • 设备系统
    • IP日志
    • 银行系统
示例：
设备共享
{
  "device_id": "device-991",
  "accounts": ["A123", "A777", "A991"]
}

IP共享
{
  "ip": "10.1.2.3",
  "accounts": ["A123", "A889"]
}

银行账户共享
{
  "bank_account": "bank-445",
  "accounts": ["A123", "A444"]
}

这些关系用于生成：
sharedDeviceAccounts
sharedIpAccounts
clusterSize
riskNeighborCount

## 4. 外部风险数据（External Risk Data）
这些数据通常来自外部系统或风控供应商。
例如：
高风险IP库
{
  "ip": "45.22.11.8",
  "risk_level": "high",
  "source": "threat_intel"
}

黑名单账户
{
  "account_id": "A999",
  "reason": "confirmed_fraud"
}

代理/VPN IP
{
  "ip": "23.11.55.77",
  "type": "vpn"
}

这些数据用于：
HighRiskIpRule
LinkedFraudAccountRule
GeoRiskRule

## 5. 数据输入最小集合（最小可运行系统）
如果先做 MVP版本，只需要这三类输入：
Account profile
Login events
Transaction events
就可以运行：
    • 特征生成
    • 规则检测
    • ML异常检测
    • 风险评分

## 6. 数据规模预估（现实系统）
实际系统规模通常是：
数据	规模
accounts	100k – 100M
events/day	1M – 1B
features/day	100k – 10M
cases/day	100 – 10000

## 7. 输入数据的一个重要设计原则
不要让输入数据直接变复杂。
系统只接收 统一事件格式：
{
  "account_id": "...",
  "event_type": "...",
  "event_time": "...",
  "payload": {...}
}
所有复杂逻辑都在：
feature engineering
rule engine
graph analysis
里完成。


# 离线导入推荐格式
建议使用 CSV 或 JSONL。
## CSV 示例
account_id,event_type,event_time,payload
A123,LOGIN_EVENT,2026-03-13T08:00:00Z,"{""ip"":""1.2.3.4"",""device_id"":""d1""}"
A123,TRANSACTION_EVENT,2026-03-13T08:05:00Z,"{""amount"":100}"

优点：
    • PostgreSQL COPY 非常快
    • Java/Python 都好处理

## JSONL 示例
{"account_id":"A123","event_type":"LOGIN_EVENT","event_time":"2026-03-13T08:00:00Z","payload":{"ip":"1.2.3.4"}}
{"account_id":"A123","event_type":"TRANSACTION_EVENT","event_time":"2026-03-13T08:05:00Z","payload":{"amount":100}}

优点：
    • payload 更灵活
    • schema 更容易扩展

    