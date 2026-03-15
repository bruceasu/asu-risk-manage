# fraud-graph

## Architecture Overview

`fraud-graph` is the relationship-analysis module in the fraud stack.
It builds account-to-account graph edges from shared infrastructure and transfer activity,
detects connected clusters, computes account-level graph risk signals, and persists the
results for downstream `fraud-feature`, `fraud-risk`, and `fraud-case` consumption.

The module keeps the implementation practical:

- PostgreSQL stores graph edges, cluster memberships, summaries, and job state
- Spring JDBC is used for all persistence and source-table queries
- Java in-memory structures handle cluster detection and simple graph analytics
- No JPA, MyBatis, Neo4j, JanusGraph, or heavy graph libraries are introduced

Package layout:

- `model`: graph tables and analysis snapshot models
- `builder`: edge construction from raw relationship sources
- `analysis`: connected components, risky-neighbor counting, collector detection, cluster scoring
- `signal`: account-level graph signal generation and persistence
- `repository`: explicit SQL repositories
- `service`: staged graph build orchestration and facade access
- `job`: batch graph build runner

## Supported Edge Types

The current implementation supports four edge types:

- `SHARED_DEVICE`
  - Derived from `account_devices`
  - Uses normalized undirected edges with `least(account_id)` / `greatest(account_id)`
- `SHARED_IP`
  - Derived from `login_logs`
  - Limited to non-`LOW` IP risk rows and bounded by hotspot thresholds
- `SHARED_BANK_ACCOUNT`
  - Derived from `bank_accounts`
  - Treated as high-value undirected relationships
- `TRANSFER`
  - Derived from `transfers`
  - Direction is preserved because collector and funnel analysis depend on it

For undirected sources, the builders normalize direction before persisting edges.
For directional transfer edges, the original `from_account_id -> to_account_id` flow is kept.

## Graph Build Flow

`GraphBuildService` executes the build in clear stages:

1. Read raw relationship sources inside a graph window
2. Build `GraphEdge` rows through:
   - `SharedDeviceGraphBuilder`
   - `SharedIpGraphBuilder`
   - `SharedBankGraphBuilder`
   - `TransferGraphBuilder`
3. Run connected-component detection to create cluster memberships
4. Load latest high-risk accounts from `risk_score_result`
5. Compute:
   - one-hop risky neighbors
   - two-hop risky neighbors
   - collector/funnel metrics
   - cluster-level risk summaries
6. Persist:
   - `account_graph_edge`
   - `account_graph_cluster`
   - `graph_risk_summary`
   - `account_graph_signal`
7. Record job execution in `graph_build_job`

The service replaces data by window to keep repeated batch builds stable and easy to audit.

## Connected Cluster Detection

`ConnectedComponentAnalyzer` uses a simple adjacency map and BFS traversal:

- Build adjacency from graph edges
- Traverse unvisited nodes component by component
- Generate a stable cluster id from sorted account membership plus graph window
- Persist each account as one `account_graph_cluster` row

This is intentionally simple and readable.
The current implementation marks discovered clusters as `MIXED`, which is enough for Phase-1.
If needed later, the dominant edge family can be derived and written as a more specific cluster type.

## Graph Signal Definitions

`GraphSignalBuilder` converts graph analysis outputs into account-level signals persisted in
`account_graph_signal`.

Fields currently produced:

- `graphScore`
  - Final graph score capped to `0-100`
- `graphClusterSize`
  - Connected cluster size for the account
- `riskNeighborCount`
  - One-hop risky neighbors
- `twoHopRiskNeighborCount`
  - Two-hop risky neighbors
- `sharedDeviceAccounts`
  - Distinct linked accounts via shared devices
- `sharedIpAccounts`
  - Distinct linked accounts via shared IPs
- `sharedBankAccounts`
  - Distinct linked accounts via shared bank accounts
- `collectorAccountFlag`
  - Whether the account looks like a collector or funnel destination
- `funnelInDegree`
  - Transfer inbound degree
- `funnelOutDegree`
  - Transfer outbound degree
- `localDensityScore`
  - Local relationship density score capped to `0-100`
- `clusterRiskScore`
  - Cluster-level risk score from `ClusterRiskScorer`

Current explicit score formula:

```text
graphScore =
  0.25 * normalize(clusterSize, 20)
  + 0.25 * normalize(oneHopRiskNeighbors, 10)
  + 0.15 * normalize(twoHopRiskNeighbors, 20)
  + 0.15 * normalize(sharedBankAccounts, 5)
  + 0.10 * localDensityScore
  + 0.10 * clusterRiskScore
  + collectorBonus
```

Where:

- `collectorBonus = 10` when `collectorAccountFlag = true`
- all normalized count components are capped to `0-100`
- final `graphScore` is capped to `100`

## Phase-1 Batch Mode vs Phase-2 Incremental Mode

### Phase-1 Batch Mode

The current module is implemented as a Phase-1 batch graph engine:

- build graph data by explicit time windows
- persist complete edge/cluster/signal outputs for the window
- prioritize stability, readability, and operational simplicity

This is the mode supported by:

- `GraphBuildService`
- `GraphBuildJobRunner`
- `graph_build_job`

### Phase-2 Incremental Mode

Phase-2 is intentionally not implemented yet.
If needed later, it should focus on:

- incremental edge inserts
- local cluster refresh
- account-scoped signal recomputation
- event-driven graph refresh for high-value actions

The current codebase does not attempt real-time full-graph maintenance.

## How Graph Outputs Feed Feature Store and fraud-risk

The graph module is designed to feed other fraud modules rather than make final fraud decisions itself.

Typical downstream usage:

- `fraud-feature`
  - consume graph outputs to write feature fields such as:
    - `shared_device_accounts_7d`
    - `shared_ip_accounts_7d`
    - `shared_bank_accounts_30d`
    - `graph_cluster_size_30d`
    - `risk_neighbor_count_30d`
    - `collector_account_flag_7d`
- `fraud-risk`
  - consume `account_graph_signal` as graph inputs to the final risk score
- `fraud-case`
  - consume graph cluster and summary data for investigation explanation and reporting

In short:

- `fraud-graph` produces relationship intelligence
- `fraud-feature` stores reusable graph-derived features
- `fraud-risk` converts graph signals into account risk
- `fraud-case` turns graph evidence into investigator-friendly summaries
