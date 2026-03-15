insert into risk_weight_profile (
    profile_name,
    rule_weight,
    graph_weight,
    anomaly_weight,
    behavior_weight,
    enabled,
    created_at,
    updated_at
) values
    (
        'DEFAULT',
        0.40,
        0.25,
        0.20,
        0.15,
        true,
        current_timestamp,
        current_timestamp
    ),
    (
        'NO_ML',
        0.55,
        0.30,
        0.00,
        0.15,
        true,
        current_timestamp,
        current_timestamp
    )
on conflict (profile_name) do update
set rule_weight = excluded.rule_weight,
    graph_weight = excluded.graph_weight,
    anomaly_weight = excluded.anomaly_weight,
    behavior_weight = excluded.behavior_weight,
    enabled = excluded.enabled,
    updated_at = current_timestamp;
