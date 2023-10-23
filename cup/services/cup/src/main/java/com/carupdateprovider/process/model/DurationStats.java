package com.carupdateprovider.process.model;

import java.util.Map;

/**
 * Statistics record for calculating the percentiles how long messages too to be processed with a service.
 *
 * @param min         lowest processing time
 * @param max         highest processing time
 * @param percentiles percentiles of processing times
 */
public record DurationStats(long min, long max, Map<Integer, Long> percentiles) {

}
