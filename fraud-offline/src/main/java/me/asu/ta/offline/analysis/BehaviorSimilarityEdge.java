package me.asu.ta.offline.analysis;

public record BehaviorSimilarityEdge(
        String leftAccountId,
        String rightAccountId,
        double similarity,
        int leftRank,
        int rightRank,
        String note) {
}
