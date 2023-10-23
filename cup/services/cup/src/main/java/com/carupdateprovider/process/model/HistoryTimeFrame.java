package com.carupdateprovider.process.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Statistics summary time frame about the status entities in the database.
 * Only active in cup-history.
 */
@Slf4j
@ConditionalOnProperty(prefix = "application", name = "history", havingValue = "true")
@Data
public class HistoryTimeFrame {

    private int cachedCount, uncachedCount, triggerCount, rolledOutCount, requestedCount = 0;
    private long averageDurationInMillisAccumulator = 0;
    private long minDurationInMillis = Long.MAX_VALUE;
    private long maxDurationInMillis = Long.MIN_VALUE;
    private List<Long> durations = new ArrayList<>();

    private final Timestamp startTimestamp;
    private final Timestamp endTimestamp;
    private final Timestamp lastUpdatedTimestamp;
    private final long startTime;
    private final long endTime;

    /**
     * Increments triggerCount by given amount.
     *
     * @param toAdd amount to increment triggerCount.
     */
    public void incTriggerCount(int toAdd) {
        triggerCount += toAdd;
    }

    /**
     * Increments requestedCount by given amount.
     *
     * @param toAdd amount to increment requestedCount.
     */
    public void incRequestedCount(int toAdd) {
        requestedCount += toAdd;
    }

    /**
     * Increments rolledOutCount by given amount.
     *
     * @param toAdd amount to increment rolledOutCount.
     */
    public void incRolledOutCountCount(int toAdd) {
        rolledOutCount += toAdd;
    }

    /**
     * Increments cachedCount by 1.
     */
    public void incCachedCount() {
        cachedCount += 1;
    }

    /**
     * Increments uncachedCount by 1.
     */
    public void incUncachedCount() {
        uncachedCount += 1;
    }

    /**
     * Adds processing duration to the internal durations list.
     *
     * @param toAdd duration to add.
     */
    public void addDuration(long toAdd) {
        durations.add(toAdd);
    }

    /**
     * Creates {@link DurationStats} record from internal durations list.
     *
     * @return DurationStats
     */
    private DurationStats getDurationStats() {

        List<Long> sortedDurations = durations.stream()
                                              .sorted()
                                              .toList();
        int s = sortedDurations.size();
        Map<Integer, Long> percentiles = new HashMap<>();
        percentiles.put(5, sortedDurations.get((int) Math.round(0.05 * s)));
        percentiles.put(95, sortedDurations.get((int) Math.round(0.95 * s)));
        for (int i = 10; i <= 90; i += 10) {
            float p = Float.parseFloat("0." + i);
            percentiles.put(i, sortedDurations.get((int) Math.round(p * s)));
        }
        return new DurationStats(sortedDurations.get(0), sortedDurations.get(s - 1), percentiles);
    }

    /**
     * Creates summary report string based of the internal state.
     *
     * @return summary report string.
     */
    public String summary() {
        final int timeZoneAdjustment = 1000 * 60 * 60;
        DurationStats stats = getDurationStats();
        boolean isValid = triggerCount == rolledOutCount
                          && triggerCount == requestedCount
                          && (cachedCount + uncachedCount) == triggerCount;
        return """
                ### Timeframe from %d to %d ###
                First requested: %s
                Last rolled out: %s
                Last updated in history: %s
                Business flow duration in seconds: %f
                Total flow duration in seconds: %f
                Count requested: %d
                Count triggerd: %d
                Count rolled out: %d
                Count cached: %d
                Count uncached: %d
                Valid count: %b
                Min duration in millis: %d
                05th percentile duration in millis: %d
                10th percentile duration in millis: %d
                20th percentile duration in millis: %d
                30th percentile duration in millis: %d
                40th percentile duration in millis: %d
                Median duration in millis: %d
                60th percentile duration in millis: %d
                70th percentile duration in millis: %d
                80th percentile duration in millis: %d
                90th percentile duration in millis: %d
                95th percentile duration in millis: %d
                Max duration in millis: %d
                ################################
                        
                """.formatted(startTime,
                              endTime,
                              new Timestamp(startTimestamp.getTime() + timeZoneAdjustment),
                              new Timestamp(endTimestamp.getTime() + timeZoneAdjustment),
                              new Timestamp(lastUpdatedTimestamp.getTime() + timeZoneAdjustment),
                              (endTimestamp.getTime() - startTimestamp.getTime()) / 1000F,
                              (lastUpdatedTimestamp.getTime() - startTimestamp.getTime()) / 1000F,
                              requestedCount,
                              triggerCount,
                              rolledOutCount,
                              cachedCount,
                              uncachedCount,
                              isValid,
                              stats.min(),
                              stats.percentiles()
                                   .get(5),
                              stats.percentiles()
                                   .get(10),
                              stats.percentiles()
                                   .get(20),
                              stats.percentiles()
                                   .get(30),
                              stats.percentiles()
                                   .get(40),
                              stats.percentiles()
                                   .get(50),
                              stats.percentiles()
                                   .get(60),
                              stats.percentiles()
                                   .get(70),
                              stats.percentiles()
                                   .get(80),
                              stats.percentiles()
                                   .get(90),
                              stats.percentiles()
                                   .get(95),
                              stats.max());
    }

}
