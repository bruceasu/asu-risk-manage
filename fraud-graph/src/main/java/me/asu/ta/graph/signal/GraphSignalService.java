package me.asu.ta.graph.signal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.asu.ta.graph.model.AccountGraphSignal;
import me.asu.ta.graph.model.GraphAnalysisSnapshot;
import me.asu.ta.graph.repository.AccountGraphSignalRepository;
import org.springframework.stereotype.Service;

@Service
public class GraphSignalService {
    private final GraphSignalBuilder graphSignalBuilder;
    private final AccountGraphSignalRepository accountGraphSignalRepository;

    public GraphSignalService(
            GraphSignalBuilder graphSignalBuilder,
            AccountGraphSignalRepository accountGraphSignalRepository) {
        this.graphSignalBuilder = graphSignalBuilder;
        this.accountGraphSignalRepository = accountGraphSignalRepository;
    }

    public List<AccountGraphSignal> buildSignals(GraphAnalysisSnapshot snapshot, Instant graphWindowStart, Instant graphWindowEnd) {
        return graphSignalBuilder.buildSignals(snapshot, graphWindowStart, graphWindowEnd);
    }

    public int persistSignals(List<AccountGraphSignal> signals, Instant graphWindowStart, Instant graphWindowEnd) {
        accountGraphSignalRepository.deleteByWindow(graphWindowStart, graphWindowEnd);
        return accountGraphSignalRepository.batchInsert(signals);
    }

    public Optional<AccountGraphSignal> findByAccountId(String accountId) {
        return accountGraphSignalRepository.findByAccountId(accountId);
    }
}
