insert into risk_reason_mapping (
    reason_code,
    reason_title,
    reason_description,
    severity,
    category,
    created_at,
    updated_at
) values
    (
        'HIGH_RISK_IP_LOGIN',
        'High Risk IP Login',
        'Login from a high risk IP reached the configured rule threshold.',
        'HIGH',
        'RULE',
        current_timestamp,
        current_timestamp
    ),
    (
        'LOGIN_FAILURE_BURST',
        'Login Failure Burst',
        'A burst of login failures and failure rate exceeded the configured threshold.',
        'HIGH',
        'RULE',
        current_timestamp,
        current_timestamp
    ),
    (
        'RAPID_WITHDRAW_AFTER_DEPOSIT',
        'Rapid Withdraw After Deposit',
        'Withdrawal activity followed deposits unusually quickly.',
        'HIGH',
        'RULE',
        current_timestamp,
        current_timestamp
    ),
    (
        'REWARD_WITHDRAW_ABUSE',
        'Reward Withdraw Abuse',
        'Reward-linked transactions were followed by suspiciously fast withdrawal behavior.',
        'MEDIUM',
        'RULE',
        current_timestamp,
        current_timestamp
    ),
    (
        'SHARED_DEVICE_CLUSTER',
        'Shared Device Cluster',
        'The account is linked to a device shared by multiple accounts.',
        'MEDIUM',
        'RULE',
        current_timestamp,
        current_timestamp
    ),
    (
        'DEVICE_SWITCH_SPIKE',
        'Device Switch Spike',
        'The account showed an unusual spike in device switching activity.',
        'MEDIUM',
        'RULE',
        current_timestamp,
        current_timestamp
    ),
    (
        'RAPID_PROFILE_CHANGE',
        'Rapid Profile Change',
        'Rapid profile or security-sensitive changes were detected.',
        'MEDIUM',
        'RULE',
        current_timestamp,
        current_timestamp
    ),
    (
        'SECURITY_CHANGE_BEFORE_WITHDRAW',
        'Security Change Before Withdraw',
        'A security-sensitive change occurred shortly before withdrawal.',
        'HIGH',
        'RULE',
        current_timestamp,
        current_timestamp
    ),
    (
        'HIGH_RISK_NEIGHBOR_CLUSTER',
        'High Risk Neighbor Cluster',
        'Graph links show a risky cluster with suspicious neighboring accounts.',
        'HIGH',
        'RULE',
        current_timestamp,
        current_timestamp
    ),
    (
        'ATO_SUSPICION_COMPOSITE',
        'ATO Suspicion Composite',
        'Multiple account-takeover indicators were hit together.',
        'CRITICAL',
        'RULE',
        current_timestamp,
        current_timestamp
    ),
    (
        'GRAPH_HIGH_RISK_NEIGHBORS',
        'Graph High Risk Neighbors',
        'Graph neighborhood contains too many risky linked accounts.',
        'HIGH',
        'GRAPH',
        current_timestamp,
        current_timestamp
    ),
    (
        'GRAPH_LARGE_CLUSTER',
        'Graph Large Cluster',
        'The account belongs to a large suspicious graph cluster.',
        'MEDIUM',
        'GRAPH',
        current_timestamp,
        current_timestamp
    ),
    (
        'GRAPH_SHARED_DEVICE_CLUSTER',
        'Graph Shared Device Cluster',
        'Graph signals show shared device exposure across multiple accounts.',
        'HIGH',
        'GRAPH',
        current_timestamp,
        current_timestamp
    ),
    (
        'GRAPH_SHARED_BANK_CLUSTER',
        'Graph Shared Bank Cluster',
        'Graph signals show suspicious bank account sharing.',
        'HIGH',
        'GRAPH',
        current_timestamp,
        current_timestamp
    ),
    (
        'ML_ANOMALY_HIGH',
        'ML Anomaly High',
        'The anomaly model produced a high normalized anomaly score.',
        'HIGH',
        'ML',
        current_timestamp,
        current_timestamp
    ),
    (
        'ML_ANOMALY_MEDIUM',
        'ML Anomaly Medium',
        'The anomaly model produced a medium normalized anomaly score.',
        'MEDIUM',
        'ML',
        current_timestamp,
        current_timestamp
    ),
    (
        'BEHAVIOR_LOGIN_FAILURE_RATE_HIGH',
        'Behavior Login Failure Rate High',
        'The login failure rate in the recent window is abnormally high.',
        'MEDIUM',
        'BEHAVIOR',
        current_timestamp,
        current_timestamp
    ),
    (
        'BEHAVIOR_HIGH_RISK_IP_ACTIVITY',
        'Behavior High Risk IP Activity',
        'Recent feature signals show high risk IP login activity.',
        'MEDIUM',
        'BEHAVIOR',
        current_timestamp,
        current_timestamp
    ),
    (
        'BEHAVIOR_RAPID_WITHDRAW_PATTERN',
        'Behavior Rapid Withdraw Pattern',
        'Feature signals show unusually short delay between deposit and withdrawal.',
        'MEDIUM',
        'BEHAVIOR',
        current_timestamp,
        current_timestamp
    ),
    (
        'BEHAVIOR_SHARED_DEVICE_EXPOSURE',
        'Behavior Shared Device Exposure',
        'Behavior features show elevated shared device exposure.',
        'MEDIUM',
        'BEHAVIOR',
        current_timestamp,
        current_timestamp
    ),
    (
        'BEHAVIOR_SECURITY_CHANGE_BEFORE_WITHDRAW',
        'Behavior Security Change Before Withdraw',
        'Behavior features show security changes immediately before withdrawal.',
        'HIGH',
        'BEHAVIOR',
        current_timestamp,
        current_timestamp
    )
on conflict (reason_code) do update
set reason_title = excluded.reason_title,
    reason_description = excluded.reason_description,
    severity = excluded.severity,
    category = excluded.category,
    updated_at = current_timestamp;
