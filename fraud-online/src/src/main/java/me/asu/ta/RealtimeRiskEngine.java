package me.asu.ta;
import me.asu.ta.dto.Event;
import me.asu.ta.util.EventCsvIO;
import me.asu.ta.util.SimpleCli;
import me.asu.ta.util.SpscRing;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;


/**
 * Realtime risk-engine entrypoint.
 *
 * Modes:
 * - stream (default): consume realtime injected request/order/quote stream from STDIN
 * - synthetic: generate events in-memory and run loop
 * - file: read events from CSV and replay
 */
public class RealtimeRiskEngine {


    public static void main(String[] args) throws Exception {
        if (hasHelpArg(args)) {
            printHelp();
            return;
        }

        RiskEngineConfig config = new RiskEngineConfig();

        SimpleCli cli = SimpleCli.parseArgs(args);

        config.ring = new SpscRing<>(1 << 16);
        config.symbolCount = cli.intv( "--symbol-count", RiskConfig.MAX_SYMBOLS);
        config.accountCount = cli.intv( "--account-count", RiskConfig.MAX_ACCOUNTS);
        config.riskMinN = cli.intv( "--risk-min-n", 30);
        String riskOut = cli.get("--risk-out");
        SnapshotFileWriter writer = null;
        if (riskOut != null && !riskOut.isBlank()) {
            writer = new SnapshotFileWriter(Paths.get(riskOut));
            config.snapshotWriter = writer;
        }
        RiskLoop loop = new RiskLoop(config);
        String mode = cli.get("--mode", "stream").trim().toLowerCase(Locale.ROOT);

        try {
            if ("file".equals(mode)) {
                Path eventsFile = Paths.get(cli.require("--events-file"));
                double replaySpeed = 0.0;
                if (cli.has("--replay-speed")) {
                    try {
                        replaySpeed = Double.parseDouble(cli.get("--replay-speed"));
                    } catch (Exception ignored) {
                    }
                }
                int maxEps = cli.intv("--max-eps", 0);
                runFromFile(loop, config.ring, eventsFile, replaySpeed, maxEps);
            } else {
                 int maxEps = cli.intv("--max-eps", 0);
                runStream(loop, config.ring, maxEps);
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }

        System.out.println("Finished.");
    }

    private static void runFromFile(RiskLoop loop, SpscRing<Event> ring, Path eventsFile,
            double replaySpeed, int maxEps) throws Exception {
        System.out.println("Loading events: " + eventsFile);
        if (replaySpeed > 0.0) System.out.println("Replay speed: " + replaySpeed);
        if (maxEps > 0) System.out.println("Max events/sec: " + maxEps);

        List<Event> events = EventCsvIO.loadTradeEvents(eventsFile);
        if (events.isEmpty()) {
            System.out.println("No events loaded.");
            return;
        }

        long lastEventTs = -1L;
        long lastEmitNano = System.nanoTime();
        long windowStartNano = System.nanoTime();
        int eventsThisWindow = 0;

        long used = 0;
        for (Event e : events) {
            long nowNano = System.nanoTime();

            long sleepNanosReplay = 0L;
            if (replaySpeed > 0.0 && lastEventTs >= 0L) {
                long deltaMs = e.ts() - lastEventTs;
                if (deltaMs < 0) deltaMs = 0;
                long desiredNanos = (long) (deltaMs * 1_000_000L / replaySpeed);
                long elapsedSinceLast = nowNano - lastEmitNano;
                sleepNanosReplay = desiredNanos - elapsedSinceLast;
                if (sleepNanosReplay < 0) sleepNanosReplay = 0;
            }

            long sleepNanosRate = 0L;
            if (maxEps > 0) {
                long windowElapsed = nowNano - windowStartNano;
                if (windowElapsed >= 1_000_000_000L) {
                    windowStartNano = nowNano;
                    eventsThisWindow = 0;
                }
                if (eventsThisWindow >= maxEps) {
                    sleepNanosRate = 1_000_000_000L - windowElapsed;
                }
            }

            long sleepNanos = Math.max(sleepNanosReplay, sleepNanosRate);
            if (sleepNanos > 0) {
                long ms = sleepNanos / 1_000_000L;
                int ns = (int) (sleepNanos % 1_000_000L);
                Thread.sleep(ms, ns);
            }

            while (!ring.offer(e)) {
                loop.runOnce(e.ts());
            }
            loop.runOnce(e.ts());

            lastEventTs = e.ts();
            lastEmitNano = System.nanoTime();
            if (maxEps > 0) eventsThisWindow++;

            used++;
        }

        loop.runOnce(lastEmitNano / 1_000_000L + 2000);
        System.out.println("Events replayed: " + used);
    }

    private static void runStream(RiskLoop loop, SpscRing<Event> ring, int maxEps) throws Exception {
        System.out.println("Start realtime stream mode. Events are supplied continuously into the provided SpscRing.");
        if (maxEps > 0) System.out.println("Max events/sec: " + maxEps);

        long windowStartNano = System.nanoTime();
        int eventsThisWindow = 0;

        try {
            while (true) {
                Event e = ring.poll();
                if (e != null) {
                    long nowNano = System.nanoTime();
                    if (maxEps > 0) {
                        long windowElapsed = nowNano - windowStartNano;
                        if (windowElapsed >= 1_000_000_000L) {
                            windowStartNano = nowNano;
                            eventsThisWindow = 0;
                        }
                        if (eventsThisWindow >= maxEps) {
                            long sleepNanos = 1_000_000_000L - windowElapsed;
                            long ms = sleepNanos / 1_000_000L;
                            int ns = (int) (sleepNanos % 1_000_000L);
                            Thread.sleep(ms, ns);
                            // reset window
                            windowStartNano = System.nanoTime();
                            eventsThisWindow = 0;
                        }
                    }

                    loop.runOnce(e.ts());
                    if (maxEps > 0) eventsThisWindow++;
                } else {
                    // No events right now — advance to current time to run periodic work.
                    loop.runOnce(System.currentTimeMillis());
                    // Small pause to avoid busy-spin when producer is quiet.
                    Thread.sleep(1);
                }
            }
        } catch (InterruptedException ie) {
            // Exit on interrupt (e.g., SIGINT) so the process can shut down cleanly.
            Thread.currentThread().interrupt();
        }
        // Final flush
        loop.runOnce(System.currentTimeMillis() + 2000);
    }

    private static boolean hasHelpArg(String[] args) {
        for (String a : args) {
            if ("-h".equals(a) || "--help".equals(a))
                return true;
        }
        return false;
    }

    private static void printHelp() {
        System.out.println(
                """
                        RealtimeRiskEngineDemo

                        Usage:
                          java -jar bin/app.jar [options]

                        Common options:
                          -h, --help                       Show help
                          --mode stream|synthetic|file     default: stream
                          --symbol-count <N>               default: 64
                          --account-count <N>              default: 4096
                          
                            Stream mode:
                                    Events are supplied by an external producer into the application's SpscRing.
                                    Optional flag:
                                        --max-eps <int>          Maximum events per second (consumer-side limit). Default: disabled

                            File mode options:
                                --events-file <file>             Required when --mode file
                                --replay-speed <float>           Timestamp-based replay multiplier for file replay (1.0 = real time). Default: disabled
                                --max-eps <int>                  Maximum events per second (consumer-side limit). Default: disabled
                                --risk-out <file>                Optional CSV output for risk accounts snapshot
                                --risk-min-n <N>                 Min samples before account can enter Top-N (default: 30)

                        Generate 10k test data:
                          java -cp classes DataGen10k

                        Examples:
                          java -jar bin/app.jar --mode synthetic --events 20000
                          java -jar bin/app.jar --mode file --events-file examples/events_10000.csv --risk-out risk_accounts.csv
                          type stream.txt | java -jar bin/app.jar --mode stream
                        """);
    }

    
}