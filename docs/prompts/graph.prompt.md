
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
