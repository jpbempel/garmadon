package com.criteo.hadoop.garmadon.heuristics;

import com.criteo.jvm.JVMStatisticsProtos;

import java.util.HashMap;
import java.util.Map;

import static com.criteo.hadoop.garmadon.heuristics.HeuristicHelper.createCounterHeuristic;

public class G1GC implements GCStatsHeuristic {
    private final HeuristicsResultDB heuristicsResultDB;
    private final Map<String, Map<String, FullGCCounters>> appFullGC = new HashMap<>();

    public G1GC(HeuristicsResultDB heuristicsResultDB) {
        this.heuristicsResultDB = heuristicsResultDB;
    }

    @Override
    public void process(String applicationId, String containerId, JVMStatisticsProtos.GCStatisticsData gcStats) {
        if (GCHelper.gcKind(gcStats.getCollectorName()) != GCHelper.GCKind.G1)
            return;
        GCHelper.GCGenKind gcGenKind = GCHelper.gcGenKind(gcStats.getCollectorName());
        if (gcGenKind == GCHelper.GCGenKind.MAJOR) {
            Map<String, FullGCCounters> containerFullGC = appFullGC.computeIfAbsent(applicationId, s -> new HashMap<>());
            FullGCCounters details = containerFullGC.computeIfAbsent(containerId, s -> new FullGCCounters(gcStats.getTimestamp(), gcStats.getPauseTime()));
            details.count++;
            if (details.count > 1)
                details.pauseTime += gcStats.getPauseTime();
            details.severity = HeuristicsResultDB.Severity.SEVERE;
        }
    }

    @Override
    public void onContainerCompleted(String applicationId, String containerId) {

    }

    @Override
    public void onAppCompleted(String applicationId) {
        createCounterHeuristic(applicationId, appFullGC, heuristicsResultDB, G1GC.class, counter -> {
            if (counter.count == 1)
                return "Timestamp: " + HeuristicResult.formatTimestamp(counter.timestamp) + ", pauseTime: " + counter.pauseTime + "ms";
            else
                return "Count: " + counter.count + ", Cumulative PauseTime: " + counter.pauseTime + "ms";
        });
    }

    private static class FullGCCounters extends BaseCounter {
        int count;
        long timestamp;
        long pauseTime;

        FullGCCounters(long timestamp, long pauseTime) {
            this.timestamp = timestamp;
            this.pauseTime = pauseTime;
        }
    }
}
