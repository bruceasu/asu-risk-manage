begin;

-- fraud-case 当前没有独立的 lookup 表。
-- 这份脚本提供可执行的参考数据集，方便初始化、联调和运营校对默认状态/动作码。

-- 1. case_status reference
with case_status_reference(case_status, description) as (
    values
        ('OPEN', 'New investigation case created and awaiting action'),
        ('UNDER_REVIEW', 'Case is being worked by an investigator'),
        ('ESCALATED', 'Case has been escalated for deeper review'),
        ('CLOSED_CONFIRMED_FRAUD', 'Case closed as confirmed fraud'),
        ('CLOSED_FALSE_POSITIVE', 'Case closed as false positive'),
        ('CLOSED_MONITOR_ONLY', 'Case closed with monitoring only')
)
select * from case_status_reference;

-- 2. case_generation_job status reference
with case_job_status_reference(job_status, description) as (
    values
        ('RUNNING', 'Batch job is actively processing accounts'),
        ('PARTIAL_SUCCESS', 'Batch job completed with one or more account-level failures'),
        ('COMPLETED', 'Batch job completed successfully'),
        ('FAILED', 'Batch job failed before it could complete')
)
select * from case_job_status_reference;

-- 3. recommendation action code reference
with case_action_reference(action_code, description) as (
    values
        ('FREEZE_ACCOUNT', 'Immediately restrict the account due to critical risk'),
        ('MANUAL_REVIEW', 'Route the case to an investigator'),
        ('HOLD_WITHDRAWAL', 'Pause withdrawals pending review'),
        ('STEP_UP_VERIFICATION', 'Require enhanced verification'),
        ('MONITOR_ACCOUNT', 'Apply enhanced monitoring'),
        ('RETAIN_FOR_AUDIT', 'Retain case artifacts for audit traceability'),
        ('REVIEW_SECURITY_CHANGES', 'Review recent password/security changes'),
        ('INVESTIGATE_LINKED_ACCOUNTS', 'Investigate linked accounts from shared-device exposure')
)
select * from case_action_reference;

-- 4. timeline event type reference
with case_timeline_event_reference(event_type, description) as (
    values
        ('CASE_CREATED', 'Case created from current evaluation outputs'),
        ('RULE_HIT', 'A fraud rule hit contributed to the case'),
        ('LOGIN_PATTERN', 'High-risk login pattern detected'),
        ('NEW_DEVICE_LOGIN', 'Login from a newly observed device'),
        ('PASSWORD_RESET', 'Recent password reset or profile/security change'),
        ('SECURITY_PATTERN', 'Security-sensitive change near withdrawal activity'),
        ('DEPOSIT_ACTIVITY', 'Recent deposit activity'),
        ('WITHDRAWAL_ACTIVITY', 'Recent withdrawal activity'),
        ('HIGH_VALUE_TRANSFER', 'Recent large-value transfer pattern')
)
select * from case_timeline_event_reference;

commit;
