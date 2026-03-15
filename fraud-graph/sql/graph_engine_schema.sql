
-- 1. graph_build_job
-- 记录批量构图任务。
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
-- 状态建议：
--     • RUNNING
--     • SUCCESS
--     • FAILED
--     • PARTIAL_SUCCESS

-- 2. account_graph_edge
-- 账户图边表。
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
-- 索引：
CREATE INDEX idx_account_graph_edge_from
ON account_graph_edge(from_account_id);
CREATE INDEX idx_account_graph_edge_to
ON account_graph_edge(to_account_id);
CREATE INDEX idx_account_graph_edge_type
ON account_graph_edge(edge_type);
CREATE INDEX idx_account_graph_edge_window
ON account_graph_edge(graph_window_start, graph_window_end);
-- 说明：
--     • 对无向边建议统一排序存储：from_account_id < to_account_id
--     • 转账边若保留方向性，可单独约定

-- 3. account_graph_cluster
-- 簇表。
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
-- 索引：
CREATE INDEX idx_account_graph_cluster_account
ON account_graph_cluster(account_id);
CREATE INDEX idx_account_graph_cluster_window
ON account_graph_cluster(graph_window_start, graph_window_end);
-- cluster_type 可取值：
--     • SHARED_DEVICE
--     • SHARED_IP
--     • SHARED_BANK
--     • TRANSFER_NETWORK
--     • MIXED

-- 4. account_graph_signal
-- 账户级图风险信号表。
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
-- 索引：
CREATE INDEX idx_account_graph_signal_generated
ON account_graph_signal(generated_at);

-- 5. graph_risk_summary
-- 簇级风险摘要表。
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