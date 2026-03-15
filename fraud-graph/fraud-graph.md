生产级 fraud-graph 设计，包含三部分：
    1. fraud-graph 模块设计
    2. graph_engine_schema.sql
    3. fraud-graph AI 代码生成 Prompt
这套设计严格贴合你现在的路线：
    • Java 25
    • Spring Boot 4.x
    • Maven
    • Spring JDBC
    • PostgreSQL
    • 少依赖
    • 不引入重型图库
    • Phase-1 批处理优先
    • Phase-2 再加增量更新
目标不是做一个通用图库，而是做一个反欺诈关系分析引擎，服务于：
    • 共享设备识别
    • 共享 IP 识别
    • 共享银行卡识别
    • 转账网络分析
    • 团伙簇识别
    • 高风险邻居扩散
    • Collector / funnel 模式识别

一、fraud-graph 的定位
fraud-graph 负责把“账户与账户之间的隐式关联”结构化出来，并生成可供风控使用的图谱信号。
它不直接做最终风险判定，而是输出：
    • 图边
    • 图簇
    • 图特征
    • 图风险信号
一句话：
fraud-graph 是反欺诈系统里的关系网络检测层。

二、核心设计原则
2.1 第一版只做“账户中心图”
不要一开始做复杂异构图库平台。
第一版只围绕 ACCOUNT 这个核心实体展开。
但边可以来自：
    • 共享设备
    • 共享IP
    • 共享银行卡
    • 转账关系
也就是说：
    • 节点主要是 account
    • 其他实体通过“关系推导”体现在边上
这样简单很多，也足够有效。

2.2 图分析优先“实用信号”，而不是炫技算法
第一版最有价值的不是 PageRank 或 GNN，而是这些：
    • shared device count
    • shared IP count
    • shared bank count
    • cluster size
    • risk neighbor count
    • transfer funnel pattern
    • collector account detection

2.3 批处理优先
Phase-1 不要追求实时全图维护。
第一版最稳的方式：
    • 每小时 / 每日批量构图
    • 生成账户级图特征
    • 回写到 Feature Store

2.4 不引入重量级图库
第一版不建议上：
    • JanusGraph
    • Neo4j
    • Spark GraphX
    • 图数据库集群
原因：
    • 系统复杂度大幅上升
    • 运维成本高
    • 你当前阶段还不需要
最稳路线是：
    • PostgreSQL 存边/簇/摘要
    • Java 读出批量分析
    • 用简单内存图结构做局部算法

三、模块职责
建议模块：
fraud-graph
├── model
├── builder
├── analysis
├── signal
├── repository
├── service
└── job

四、图模型设计

4.1 核心节点模型
第一版只保留账户节点：
GraphAccountNode
- accountId
- riskLevel(optional)
- isFlagged(optional)

4.2 边类型
建议支持以下边类型：
    • SHARED_DEVICE
    • SHARED_IP
    • SHARED_BANK_ACCOUNT
    • TRANSFER
    • REWARD_FLOW（可选）
    • COLLECTOR_FLOW（可选）

4.3 边的权重
边建议有简单权重，便于后续扩展：
    • 共享 1 个设备 vs 共享 5 个设备
    • 一次转账 vs 多次转账
例如：
    • weight = shared_count
    • weight = transfer_count
    • weight = transfer_amount_total

五、图构建思路

5.1 共享设备图
来源：
    • account_devices
规则：
    • 如果两个账户在时间窗口内使用同一设备
    • 则生成一条 SHARED_DEVICE 边
权重：
    • 共享设备数

5.2 共享 IP 图
来源：
    • login_logs
规则：
    • 两个账户在窗口内共享登录 IP
    • 则生成 SHARED_IP 边
注意：
    • 可排除低风险大 ISP 热点 IP，或至少降低权重
    • 第一版可以只对中高风险 IP 建图

5.3 共享银行卡图
来源：
    • bank_accounts
规则：
    • 多账户共享同一 bank_account_id
    • 则生成 SHARED_BANK_ACCOUNT 边
这个通常是高价值关系。

5.4 转账图
来源：
    • transfers
    • 或 transactions 中的 account-to-account flows
规则：
    • from_account -> to_account 生成 TRANSFER 边
可记录：
    • transfer_count
    • transfer_amount_sum
    • last_transfer_time

六、第一版图分析能力
建议第一版先做 6 类图分析能力。

6.1 Cluster Detection（连通簇）
用途：
    • 找团伙
    • 找共享基础设施群体
实现：
    • connected components / union-find / BFS
输出：
    • cluster_id
    • cluster_size

6.2 Risk Neighbor Count
用途：
    • 统计某账户一跳高风险邻居数量
输出：
    • risk_neighbor_count
来源：
    • risk_score_result 或 fraud_labels

6.3 Two-hop / Three-hop Risk Neighbors
用途：
    • 扩展关系风险感知
第一版可以只做 two-hop。

6.4 Collector Detection
用途：
    • 识别收口账户 / 资金汇集点
简单规则：
    • 入边多
    • 出边少
    • 短时间大量流入
输出：
    • collector_account_flag
    • funnel_in_degree
    • funnel_out_degree

6.5 Shared Infrastructure Density
用途：
    • 识别共享设备 / IP / 银行卡密集群
输出：
    • local_density_score

6.6 Cluster Risk Score
用途：
    • 给整个簇打一个风险分，供上层消费
可以基于：
    • cluster size
    • high-risk node ratio
    • shared bank edges
    • collector presence

七、图输出设计
图引擎最终要输出两类产物：

7.1 图明细结果
用于调试和案件解释：
    • 图边
    • 图簇成员
    • 邻居表

7.2 图风险信号
供 fraud-risk 和 fraud-case 使用：
GraphRiskSignal
- graphScore
- graphClusterSize
- riskNeighborCount
- sharedDeviceAccounts
- sharedBankAccounts
- collectorAccountFlag
- funnelInDegree
- funnelOutDegree

八、与 Feature Store 的关系
这是关键。
fraud-graph 不应只是自己玩一套结果。
它应该把关键结果回写 Feature Store。
例如回写：
    • shared_device_accounts_7d
    • shared_ip_accounts_7d
    • shared_bank_accounts_30d
    • graph_cluster_size_30d
    • risk_neighbor_count_30d
    • collector_account_flag_7d
这样：
    • 规则能直接用
    • ML 能直接用
    • AI 报告也能直接引用

九、graph_engine_schema.sql
下面给出推荐表结构。

1. graph_build_job
记录批量构图任务。
CREATE TABLE graph_build_job (
    job_id                  BIGSERIAL PRIMARY KEY,
    job_type                VARCHAR(32) NOT NULL,
    graph_window_start      TIMESTAMP NOT NULL,
    graph_window_end        TIMESTAMP NOT NULL,
    started_at              TIMESTAMP NOT NULL,
    finished_at             TIMESTAMP,
    status                  VARCHAR(32) NOT NULL,
    processed_account_count INT,
    generated_edge_count    INT,
    generated_cluster_count INT,
    error_message           TEXT
);
状态建议：
    • RUNNING
    • SUCCESS
    • FAILED
    • PARTIAL_SUCCESS

2. account_graph_edge
账户图边表。
CREATE TABLE account_graph_edge (
    edge_id                  BIGSERIAL PRIMARY KEY,
    from_account_id          VARCHAR(64) NOT NULL,
    to_account_id            VARCHAR(64) NOT NULL,
    edge_type                VARCHAR(64) NOT NULL,
    edge_weight              DOUBLE PRECISION NOT NULL,
    shared_count             INT,
    transfer_count           INT,
    transfer_amount_total    DOUBLE PRECISION,
    first_seen_at            TIMESTAMP,
    last_seen_at             TIMESTAMP,
    graph_window_start       TIMESTAMP NOT NULL,
    graph_window_end         TIMESTAMP NOT NULL,
    created_at               TIMESTAMP NOT NULL
);
索引：
CREATE INDEX idx_account_graph_edge_from
ON account_graph_edge(from_account_id);
CREATE INDEX idx_account_graph_edge_to
ON account_graph_edge(to_account_id);
CREATE INDEX idx_account_graph_edge_type
ON account_graph_edge(edge_type);
CREATE INDEX idx_account_graph_edge_window
ON account_graph_edge(graph_window_start, graph_window_end);
说明：
    • 对无向边建议统一排序存储：from_account_id < to_account_id
    • 转账边若保留方向性，可单独约定

3. account_graph_cluster
簇表。
CREATE TABLE account_graph_cluster (
    cluster_id               VARCHAR(64) NOT NULL,
    account_id               VARCHAR(64) NOT NULL,
    cluster_type             VARCHAR(64) NOT NULL,
    cluster_size             INT NOT NULL,
    graph_window_start       TIMESTAMP NOT NULL,
    graph_window_end         TIMESTAMP NOT NULL,
    created_at               TIMESTAMP NOT NULL,
    PRIMARY KEY(cluster_id, account_id)
);
索引：
CREATE INDEX idx_account_graph_cluster_account
ON account_graph_cluster(account_id);
CREATE INDEX idx_account_graph_cluster_window
ON account_graph_cluster(graph_window_start, graph_window_end);
cluster_type 可取值：
    • SHARED_DEVICE
    • SHARED_IP
    • SHARED_BANK
    • TRANSFER_NETWORK
    • MIXED

4. account_graph_signal
账户级图风险信号表。
CREATE TABLE account_graph_signal (
    account_id               VARCHAR(64) PRIMARY KEY,
    graph_window_start       TIMESTAMP NOT NULL,
    graph_window_end         TIMESTAMP NOT NULL,
graph_score              DOUBLE PRECISION NOT NULL,
graph_cluster_size       INT,
    risk_neighbor_count      INT,
    two_hop_risk_neighbor_count INT,
shared_device_accounts   INT,
    shared_ip_accounts       INT,
    shared_bank_accounts     INT,
collector_account_flag   BOOLEAN,
    funnel_in_degree         INT,
    funnel_out_degree        INT,
local_density_score      DOUBLE PRECISION,
    cluster_risk_score       DOUBLE PRECISION,
generated_at             TIMESTAMP NOT NULL
);
索引：
CREATE INDEX idx_account_graph_signal_generated
ON account_graph_signal(generated_at);

5. graph_risk_summary
簇级风险摘要表。
CREATE TABLE graph_risk_summary (
    cluster_id               VARCHAR(64) PRIMARY KEY,
    cluster_type             VARCHAR(64) NOT NULL,
    cluster_size             INT NOT NULL,
    high_risk_node_count     INT NOT NULL,
    shared_device_edge_count INT,
    shared_ip_edge_count     INT,
    shared_bank_edge_count   INT,
    transfer_edge_count      INT,
    collector_present_flag   BOOLEAN,
    cluster_risk_score       DOUBLE PRECISION,
    graph_window_start       TIMESTAMP NOT NULL,
    graph_window_end         TIMESTAMP NOT NULL,
    generated_at             TIMESTAMP NOT NULL
);

十、Java 模型建议

10.1 GraphEdge
public record GraphEdge(
    String fromAccountId,
    String toAccountId,
    String edgeType,
    double edgeWeight,
    Integer sharedCount,
    Integer transferCount,
    Double transferAmountTotal,
    Instant firstSeenAt,
    Instant lastSeenAt
) {}

10.2 GraphClusterMembership
public record GraphClusterMembership(
    String clusterId,
    String accountId,
    String clusterType,
    int clusterSize
) {}

10.3 GraphRiskSignal
public record GraphRiskSignal(
    String accountId,
    double graphScore,
    int graphClusterSize,
    int riskNeighborCount,
    int twoHopRiskNeighborCount,
    int sharedDeviceAccounts,
    int sharedIpAccounts,
    int sharedBankAccounts,
    boolean collectorAccountFlag,
    int funnelInDegree,
    int funnelOutDegree,
    double localDensityScore,
    double clusterRiskScore,
    Instant generatedAt
) {}

10.4 GraphRiskSummary
public record GraphRiskSummary(
    String clusterId,
    String clusterType,
    int clusterSize,
    int highRiskNodeCount,
    int sharedDeviceEdgeCount,
    int sharedIpEdgeCount,
    int sharedBankEdgeCount,
    int transferEdgeCount,
    boolean collectorPresentFlag,
    double clusterRiskScore
) {}

十一、模块组件设计
建议组件如下：
fraud-graph
├── model
├── builder
├── analysis
├── signal
├── repository
├── service
└── job

11.1 builder
负责从数据库原始关系构建边。
建议类：
    • SharedDeviceGraphBuilder
    • SharedIpGraphBuilder
    • SharedBankGraphBuilder
    • TransferGraphBuilder

11.2 analysis
负责图算法与关系分析。
建议类：
    • ConnectedComponentAnalyzer
    • RiskNeighborAnalyzer
    • CollectorPatternAnalyzer
    • ClusterRiskScorer

11.3 signal
负责把图分析结果转为账户级信号。
建议类：
    • GraphSignalBuilder
    • GraphSignalNormalizer

11.4 service
总协调。
建议类：
    • GraphBuildService
    • GraphSignalService
    • GraphFacade

11.5 job
批量任务入口。
建议类：
    • GraphBuildJobRunner

十二、构图策略建议

12.1 Shared Device Graph
SQL 来源：
account_devices
构边逻辑：
    • 对相同 device_id 下的所有 account 两两成边
    • edge_type = SHARED_DEVICE
    • shared_count = 1..N
第一版可限制：
    • 同一设备账户数超过极大阈值时跳过或截断
    • 避免单设备爆炸性 join

12.2 Shared IP Graph
SQL 来源：
login_logs
建议：
    • 默认只对中高风险 IP 构边
    • 避免热点 NAT IP 造成大量噪声

12.3 Shared Bank Graph
SQL 来源：
bank_accounts
这是高价值边，优先保留。

12.4 Transfer Graph
SQL 来源：
transfers
方向性保留更有价值：
    • 收口账户识别
    • funnel 识别
    • collector 模式识别

十三、图分析算法建议
第一版不要过度复杂。

13.1 连通簇
用 BFS / DFS / Union-Find 即可。
输出：
    • cluster_id
    • cluster_size

13.2 风险邻居统计
基于当前 risk_score_result 或 fraud_labels：
    • 一跳高风险邻居数
    • 二跳高风险邻居数

13.3 Collector 检测
规则化实现即可：
如果一个账户满足：
    • funnel_in_degree >= X
    • funnel_out_degree <= Y
    • incoming_transfer_amount_total >= Z
则标记为 collector_account_flag

13.4 Cluster Risk Score
建议显式公式：
cluster_risk_score =
0.35 * high_risk_ratio +
0.25 * shared_bank_density +
0.20 * shared_device_density +
0.20 * collector_presence_score
第一版用简单归一化即可。

13.5 Graph Score
账户级图分建议显式计算，例如：
graph_score =
0.30 * normalized_cluster_size +
0.30 * normalized_risk_neighbor_count +
0.20 * normalized_shared_bank_accounts +
0.20 * collector_flag_score
然后 cap 到 100。

十四、Phase-1 / Phase-2 差异

Phase-1（离线）
流程：
events / account_devices / bank_accounts / transfers
    ↓
graph builders
    ↓
account_graph_edge
    ↓
component analysis
    ↓
account_graph_cluster
    ↓
account_graph_signal
    ↓
Feature Store 回写 / fraud-risk 消费
适合：
    • 每小时 / 每日批处理

Phase-2（实时）
不建议实时全图重建。
建议只做：
    • 新边增量插入
    • 小范围局部图更新
    • 高价值事件触发局部 graph refresh
第一版实时可只做到：
    • 当前账户的局部邻域重算

十五、fraud-graph AI 代码生成 Prompt
下面是一套可直接用于生成代码的 Prompt。

Prompt 1 — 生成模块骨架
You are a senior Java backend architect.
Generate a production-ready module named `fraud-graph`.
Environment:
- Java 25
- Spring Boot 4.x
- Maven
- Spring JDBC
- PostgreSQL
Constraints:
- Do NOT use JPA, Hibernate, MyBatis, Neo4j, JanusGraph, or heavy graph libraries.
- Keep dependencies minimal.
- Use explicit SQL only.
- Keep graph analysis practical and readable.
Responsibilities:
- build account relationship edges from shared device, shared IP, shared bank account, and transfer data
- detect connected clusters
- compute graph risk signals
- persist graph edges, clusters, and account-level graph signals
- support batch graph build jobs
Project structure:
fraud-graph
 ├── model
 ├── builder
 ├── analysis
 ├── signal
 ├── repository
 ├── service
 └── job
Generate:
- pom.xml
- package structure
- all source files

Prompt 2 — 生成模型类
Generate Java model classes for `fraud-graph`.
Required classes:
- GraphEdge
- GraphClusterMembership
- GraphRiskSignal
- GraphRiskSummary
- GraphBuildJob
Requirements:
- Use plain Java
- Use record where suitable
- Keep fields aligned with PostgreSQL schema
- Include field-level clarity

Prompt 3 — 生成 JDBC Repositories
Generate Spring JDBC repositories for `fraud-graph`.
Repositories required:
- GraphBuildJobRepository
- AccountGraphEdgeRepository
- AccountGraphClusterRepository
- AccountGraphSignalRepository
- GraphRiskSummaryRepository
Requirements:
- Use JdbcTemplate or NamedParameterJdbcTemplate
- Explicit SQL only
- Implement RowMapper classes
- Provide save(), batchInsert(), findByAccountId(), findByClusterId(), createJob(), updateJobStatus()

Prompt 4 — 生成图构建器
Generate graph builder classes for `fraud-graph`.
Required classes:
- SharedDeviceGraphBuilder
- SharedIpGraphBuilder
- SharedBankGraphBuilder
- TransferGraphBuilder
Responsibilities:
- query source tables
- generate GraphEdge rows
- normalize edge direction where appropriate
- support batch construction within a graph window
Requirements:
- keep SQL explicit
- avoid memory-heavy full graph loading where possible
- add safeguards for explosive fan-out (for example, shared IP hot spots)

Prompt 5 — 生成图分析组件
Generate analysis components for `fraud-graph`.
Required classes:
- ConnectedComponentAnalyzer
- RiskNeighborAnalyzer
- CollectorPatternAnalyzer
- ClusterRiskScorer
Responsibilities:
ConnectedComponentAnalyzer:
- detect connected components / clusters
RiskNeighborAnalyzer:
- compute one-hop and two-hop risky neighbors
CollectorPatternAnalyzer:
- detect collector or funnel account patterns from transfer edges
ClusterRiskScorer:
- calculate cluster-level risk score using explicit formula
Requirements:
- keep algorithms practical
- use simple in-memory structures
- favor clarity over advanced graph theory abstractions

Prompt 6 — 生成 GraphSignalBuilder
Generate GraphSignalBuilder and GraphSignalService for `fraud-graph`.
Responsibilities:
- convert graph analysis outputs into account-level GraphRiskSignal
- compute:
  - graphScore
  - graphClusterSize
  - riskNeighborCount
  - twoHopRiskNeighborCount
  - sharedDeviceAccounts
  - sharedIpAccounts
  - sharedBankAccounts
  - collectorAccountFlag
  - funnelInDegree
  - funnelOutDegree
  - localDensityScore
  - clusterRiskScore
- persist account_graph_signal rows
Requirements:
- keep formulas explicit
- cap normalized scores to 0-100 where appropriate
- ensure outputs are stable for downstream fraud-risk consumption

Prompt 7 — 生成 GraphBuildService 与 JobRunner
Generate GraphBuildService, GraphFacade, and GraphBuildJobRunner.
Responsibilities:
- orchestrate graph edge construction
- run connected component analysis
- compute account-level graph signals
- persist outputs
- create and update graph_build_job records
- support graph window start / end input
Requirements:
- process data in stages
- avoid loading all raw rows into memory if avoidable
- log progress clearly
- support batch execution

Prompt 8 — 生成初始化 SQL 与 README
Generate:
1. SQL initialization script for any required lookup values or defaults
2. README for `fraud-graph`
README should include:
- architecture overview
- supported edge types
- graph build flow
- connected cluster detection
- graph signal definitions
- Phase-1 batch mode vs Phase-2 incremental mode
- how graph outputs feed Feature Store and fraud-risk

Prompt 9 — 生成测试
Generate unit tests and focused integration tests for `fraud-graph`.
Test cases required:
- shared device edge generation
- shared IP edge generation with hotspot filtering
- shared bank edge generation
- transfer edge generation
- connected component detection
- risk neighbor analysis
- collector pattern detection
- graph signal calculation
- persistence of account_graph_signal
Requirements:
- keep tests deterministic
- use realistic synthetic graph examples

十六、第一阶段建议上线内容
第一阶段建议只做这些能力：
    1. SHARED_DEVICE 边
    2. SHARED_IP 边（加热点过滤）
    3. SHARED_BANK_ACCOUNT 边
    4. TRANSFER 边
    5. 连通簇检测
    6. 一跳高风险邻居数
    7. collector_account_flag
    8. account_graph_signal
这已经足够支撑：
    • 团伙识别
    • 共享基础设施识别
    • 收口账户识别
    • 风险引擎融合
    • AI 调查报告解释

十七、与其他模块的依赖关系
建议依赖方向：
event / account_devices / bank_accounts / transfers
    ↓
fraud-graph
    ↓
account_graph_signal
    ↓
fraud-feature (回写关键图特征，可选)
    ↓
fraud-risk
    ↓
fraud-case
关键点：
    • fraud-graph 不直接做最终风险决策
    • fraud-risk 消费 GraphRiskSignal
    • fraud-case 消费 GraphSummary
    • fraud-ai 可引用簇、邻居、共享实体等摘要



