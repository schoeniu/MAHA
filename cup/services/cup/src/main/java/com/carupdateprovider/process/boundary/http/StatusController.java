package com.carupdateprovider.process.boundary.http;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carupdateprovider.process.dao.StatusRepository;
import com.carupdateprovider.process.model.HistoryTimeFrame;
import com.carupdateprovider.process.model.persistence.StatusEntity;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * HTTP interface for the status history database.
 * Only active in the cup-history service.
 */
@Slf4j
@ConditionalOnProperty(prefix = "application", name = "history", havingValue = "true")
@RestController
@AllArgsConstructor
@RequestMapping("status")
public class StatusController {

    private StatusRepository statusRepository;

    /**
     * Get endpoint for the all raw status entities in the database
     *
     * @return list of all status entities in the database
     */
    @GetMapping("/raw")
    public String raw() {
        return statusRepository.findAll()
                               .toString();
    }

    /**
     * Get endpoint for providing a statistics summary about the status entities in the database.
     * The summary can be split in multiple time frames of equal length.
     *
     * @param frameLength length in milliseconds of the time frame
     * @return created statistics summary
     */
    @GetMapping("/summary")
    public String summary(@RequestParam(defaultValue = "86400000") final int frameLength) {

        StatusEntity firstRequested = statusRepository.findFirstBy(Sort.by("requested"));
        if (firstRequested == null) {
            return "Status history is empty.";
        }
        Timestamp initialStartTime = firstRequested.getRequested();

        List<HistoryTimeFrame> timeFrames = new ArrayList<>();
        int iteration = 0;
        List<StatusEntity> entities;
        do {
            Timestamp start = new Timestamp(initialStartTime.getTime() + (long) frameLength * iteration);
            Timestamp end = new Timestamp(start.getTime() + frameLength);
            entities = statusRepository.findAllByRequestedBetween(start, end);
            if (entities.isEmpty()) {
                continue;
            }
            HistoryTimeFrame timeFrame = createTimeFrame(start, frameLength, iteration, entities);
            if (timeFrame.getRequestedCount() > 0) {
                timeFrames.add(timeFrame);
            }

            iteration++;
        } while (!entities.isEmpty());
        StringBuilder sb = new StringBuilder();
        timeFrames.forEach(t -> sb.append(t.summary()));
        return sb.toString();
    }

    private HistoryTimeFrame createTimeFrame(final Timestamp startTimestamp,
                                             final int timeFrameLengthInMillis,
                                             final int iteration,
                                             final List<StatusEntity> entities) {
        final Timestamp endTimestamp = entities.stream()
                                               .map(StatusEntity::getRolled_out)
                                               .max(Timestamp::compareTo)
                                               .orElseThrow();
        final Timestamp lastUpdatedTimestamp = entities.stream()
                                                       .map(StatusEntity::getLast_update)
                                                       .max(Timestamp::compareTo)
                                                       .orElseThrow();
        final HistoryTimeFrame timeFrame = new HistoryTimeFrame(startTimestamp,
                                                                endTimestamp,
                                                                lastUpdatedTimestamp,
                                                                (long) iteration * timeFrameLengthInMillis,
                                                                (long) (iteration + 1) * timeFrameLengthInMillis);
        timeFrame.incRequestedCount(entities.size());
        entities.forEach(e -> {
            if (e.getRolled_out() != null) {
                timeFrame.incRolledOutCountCount(1);
                long duration = e.getRolled_out()
                                 .getTime() - e.getRequested()
                                               .getTime();
                timeFrame.addDuration(duration);
            }
            if (e.getTriggered() != null) {
                timeFrame.incTriggerCount(1);
            }
            if (e.getFetched() != null) {
                timeFrame.incCachedCount();
            }
            if (e.getUnfetchable() != null) {
                timeFrame.incUncachedCount();
            }
        });
        return timeFrame;
    }

    /**
     * Delete endpoint for completely emptying the database.
     */
    @DeleteMapping("/delete")
    public void delete() {
        statusRepository.deleteAll();
    }

    /**
     * Get endpoint for providing detailed timing on all processed sessions
     *
     * @return list of session timings
     */
    @GetMapping("/processing")
    public String processing() {

        StringBuilder sb = new StringBuilder();
        List<StatusEntity> entities = statusRepository.findAll(Sort.by("requested"));
        entities.forEach(e -> {
            StringBuilder eSb = new StringBuilder();
            Timestamp requested = e.getRequested();
            Timestamp triggered = e.getTriggered();
            Timestamp unfetchable = e.getUnfetchable();
            Timestamp fetched = e.getFetched();
            Timestamp created = e.getCreated();
            Timestamp rolled_out = e.getRolled_out();
            eSb.append(e.getUuid())
               .append(" requested at ")
               .append(formatLength(requested.toString()));
            eSb.append(String.format(" -> triggered times: (%d/%d)",
                                     diff(triggered, requested),
                                     diff(triggered, requested)));
            if (unfetchable != null) {
                eSb.append(String.format(" -> unfetchable times: (%d/%d)",
                                         diff(unfetchable, triggered),
                                         diff(unfetchable, requested)));
                eSb.append(String.format(" -> created times: (%d/%d)",
                                         diff(created, unfetchable),
                                         diff(created, requested)));
                eSb.append(String.format(" -> rolled_out times: (%d/%d)",
                                         diff(rolled_out, created),
                                         diff(rolled_out, requested)));
            } else {
                eSb.append(String.format(" -> fetched times: (%d/%d)",
                                         diff(fetched, triggered),
                                         diff(fetched, requested)));
                eSb.append(String.format(" -> rolled_out times: (%d/%d)",
                                         diff(rolled_out, fetched),
                                         diff(rolled_out, requested)));
            }
            sb.append(eSb.append("\n")
                         .toString());
        });

        return sb.toString();
    }

    private long diff(final Timestamp earlier, final Timestamp later) {
        return Math.abs(later.getTime() - earlier.getTime());
    }

    private String formatLength(final String s) {
        int wanted = 23;
        if (s.length() == wanted) {
            return s;
        }
        StringBuilder result = new StringBuilder(s);
        while (result.length() < wanted) {
            result.append(" ");
        }
        return result.toString();
    }

}
