package com.criteo.hadoop.garmadon.heuristics;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HeuristicResult {
    public static class HeuristicResultDetail {
        public final String name;
        public final String value;
        public final String details;

        public HeuristicResultDetail(String name, String value, String details) {
            this.name = name;
            this.value = value;
            this.details = details;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HeuristicResultDetail that = (HeuristicResultDetail) o;
            return Objects.equals(name, that.name) && Objects.equals(value, that.value) && Objects.equals(details, that.details);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value, details);
        }
    }

    public final String appId;
    public final Class<?> heuristicClass;
    public final int severity;
    public final int score;
    private final List<HeuristicResultDetail> details = new ArrayList<>();

    public HeuristicResult(String appId, Class<?> heuristicClass, int severity, int score) {
        this.appId = appId;
        this.heuristicClass = heuristicClass;
        this.severity = severity;
        this.score = score;
    }

    public void addDetail(HeuristicResultDetail detail) {
        details.add(detail);
    }

    public void addDetail(String name, String value) {
        addDetail(new HeuristicResultDetail(name, value, null));
    }

    public void addDetail(String name, String value, String details) {
        addDetail(new HeuristicResultDetail(name, value, details));
    }

    public int getDetailCount() {
        return details.size();
    }

    public HeuristicResultDetail getDetail(int index) {
        return details.get(index);
    }

    public static String formatTimestamp(long timestamp) {
        return DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("UTC")).format(Instant.ofEpochMilli(timestamp));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HeuristicResult that = (HeuristicResult) o;
        return severity == that.severity && score == that.score && Objects.equals(appId, that.appId) && Objects.equals(heuristicClass, that.heuristicClass) && Objects.equals(details, that.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appId, heuristicClass, severity, score, details);
    }
}
