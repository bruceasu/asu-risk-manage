-- Reference values used by the current fraud-graph implementation.
-- This file does not modify schema and is intended as stable seed/reference data
-- for downstream validation, UI mapping, and operational documentation.

-- graph_build_job.status
-- RUNNING
-- SUCCESS
-- FAILED
-- PARTIAL_SUCCESS

-- graph_build_job.job_type
-- BATCH_GRAPH_BUILD

-- account_graph_edge.edge_type
-- SHARED_DEVICE
-- SHARED_IP
-- SHARED_BANK_ACCOUNT
-- TRANSFER

-- account_graph_cluster.cluster_type
-- SHARED_DEVICE
-- SHARED_IP
-- SHARED_BANK
-- TRANSFER_NETWORK
-- MIXED

-- Suggested default builder safeguards used by the current codebase
-- SharedDeviceGraphBuilder.MAX_ACCOUNTS_PER_DEVICE = 20
-- SharedIpGraphBuilder.MAX_ACCOUNTS_PER_IP = 15
-- SharedBankGraphBuilder.MAX_ACCOUNTS_PER_BANK_ACCOUNT = 10

-- Suggested default collector thresholds used by the current codebase
-- CollectorPatternAnalyzer.MIN_COLLECTOR_IN_DEGREE = 3
-- CollectorPatternAnalyzer.MAX_COLLECTOR_OUT_DEGREE = 1
-- CollectorPatternAnalyzer.MIN_COLLECTOR_TRANSFER_AMOUNT = 50000
