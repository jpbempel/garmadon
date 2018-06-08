package com.criteo.hadoop.garmadon.heuristics;

import com.criteo.jvm.JVMStatisticsProtos;

import java.util.HashMap;
import java.util.Map;

public class Safepoints implements JVMStatsHeuristic {
    private final HeuristicsResultDB heuristicsResultDB;
    private final Map<String, Map<String, SafepointsCounters>> appCounters = new HashMap<>();

    public Safepoints(HeuristicsResultDB heuristicsResultDB) {
        this.heuristicsResultDB = heuristicsResultDB;
    }

    @Override
    public void process(String applicationId, String containerId, JVMStatisticsProtos.JVMStatisticsData jvmStats) {
        for (JVMStatisticsProtos.JVMStatisticsData.Section section : jvmStats.getSectionList()) {
            if ("safepoints".equals(section.getName())) {
                Map<String, SafepointsCounters> containerCounters = appCounters.computeIfAbsent(applicationId, s -> new HashMap<>());
                SafepointsCounters safepointsCounters = containerCounters.computeIfAbsent(containerId, s -> new SafepointsCounters());
                for (JVMStatisticsProtos.JVMStatisticsData.Property property : section.getPropertyList()) {
                    if ("count".equals(property.getName())) {
                        long lastCount = safepointsCounters.lastCount;
                        long lastTimestamp = safepointsCounters.lastTimestamp;
                        long currentCount = Long.parseLong(property.getValue());
                        long currentTimestamp = jvmStats.getTimestamp();
                        safepointsCounters.lastCount = currentCount;
                        safepointsCounters.lastTimestamp = currentTimestamp;
                        if (currentTimestamp == lastTimestamp) // avoid case of / 0
                            return;
                        if (lastTimestamp == 0)
                            return;
                        if (lastCount == 0)
                            return;
                        // ratio is number of safepoints/s
                        long ratio = (currentCount - lastCount) * 1000 / (currentTimestamp - lastTimestamp);
                        int severity = HeuristicsResultDB.Severity.NONE;
                        if (ratio > 3)
                            severity = HeuristicsResultDB.Severity.LOW;
                        if (ratio > 5)
                            severity = HeuristicsResultDB.Severity.MODERATE;
                        if (ratio > 7)
                            severity = HeuristicsResultDB.Severity.SEVERE;
                        if (ratio > 10)
                            severity = HeuristicsResultDB.Severity.CRITICAL;
                        safepointsCounters.ratio = Math.max(ratio, safepointsCounters.ratio);
                        safepointsCounters.severity = Math.max(severity, safepointsCounters.severity);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void onContainerCompleted(String applicationId, String containerId) {
        Map<String, SafepointsCounters> containerCounters = appCounters.get(applicationId);
        if (containerCounters == null)
            return;
        SafepointsCounters safepointsCounters = containerCounters.get(containerId);
        if (safepointsCounters == null)
            return;
        if (safepointsCounters.severity == HeuristicsResultDB.Severity.NONE)
            containerCounters.remove(containerId);
    }

    @Override
    public void onAppCompleted(String applicationId) {
        HeuristicHelper.createCounterHeuristic(applicationId, appCounters, heuristicsResultDB, Safepoints.class,
                counter -> "Max safepoint/s: " + counter.ratio);
    }

    private static class SafepointsCounters extends BaseCounter {
        long lastCount;
        long lastTimestamp;
        long ratio;
    }
}
