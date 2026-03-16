package me.asu.ta.risk.scoring;

import me.asu.ta.OfflineBehaviorContextKeys;

final class BehaviorScorePolicy {
    static final SignalRule LOGIN_FAILURE_RATE_HIGH =
            new SignalRule("loginFailureRate24h", 0.8d, 15.0d, "BEHAVIOR_LOGIN_FAILURE_RATE_HIGH");
    static final SignalRule HIGH_RISK_IP_ACTIVITY =
            new SignalRule("highRiskIpLoginCount24h", 1.0d, 20.0d, "BEHAVIOR_HIGH_RISK_IP_ACTIVITY");
    static final SignalRule RAPID_WITHDRAW_PATTERN =
            new SignalRule("withdrawAfterDepositDelayAvg24h", 30.0d, 20.0d, "BEHAVIOR_RAPID_WITHDRAW_PATTERN");
    static final SignalRule SHARED_DEVICE_EXPOSURE =
            new SignalRule("sharedDeviceAccounts7d", 5.0d, 20.0d, "BEHAVIOR_SHARED_DEVICE_EXPOSURE");
    static final SignalRule SECURITY_CHANGE_BEFORE_WITHDRAW =
            new SignalRule("securityChangeBeforeWithdrawFlag24h", 1.0d, 25.0d, "BEHAVIOR_SECURITY_CHANGE_BEFORE_WITHDRAW");
    static final SignalRule OFFLINE_CLUSTER_DENSE =
            new SignalRule(OfflineBehaviorContextKeys.BEHAVIOR_CLUSTER_SIZE, 4.0d, 10.0d, "BEHAVIOR_OFFLINE_CLUSTER_DENSE");
    static final SignalRule OFFLINE_SIMILAR_ACCOUNTS =
            new SignalRule(OfflineBehaviorContextKeys.SIMILAR_ACCOUNT_COUNT, 3.0d, 10.0d, "BEHAVIOR_OFFLINE_SIMILAR_ACCOUNTS");
    static final SignalRule OFFLINE_COORDINATED_TRADING =
            new SignalRule(OfflineBehaviorContextKeys.COORDINATED_TRADING_SCORE, 60.0d, 15.0d, "BEHAVIOR_OFFLINE_COORDINATED_TRADING");

    private BehaviorScorePolicy() {
    }

    static final class SignalRule {
        private final String evidenceKey;
        private final double threshold;
        private final double score;
        private final String reasonCode;

        private SignalRule(String evidenceKey, double threshold, double score, String reasonCode) {
            this.evidenceKey = evidenceKey;
            this.threshold = threshold;
            this.score = score;
            this.reasonCode = reasonCode;
        }

        String evidenceKey() {
            return evidenceKey;
        }

        double threshold() {
            return threshold;
        }

        double score() {
            return score;
        }

        String reasonCode() {
            return reasonCode;
        }
    }
}
