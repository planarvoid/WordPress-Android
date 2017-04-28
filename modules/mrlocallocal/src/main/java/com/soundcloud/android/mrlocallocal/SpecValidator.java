package com.soundcloud.android.mrlocallocal;

import com.google.common.primitives.Longs;
import com.soundcloud.android.mrlocallocal.data.LoggedEvent;
import com.soundcloud.android.mrlocallocal.data.MrLocalLocalException;
import com.soundcloud.android.mrlocallocal.data.MrLocalLocalResult;
import com.soundcloud.android.mrlocallocal.data.Spec;
import com.soundcloud.android.mrlocallocal.data.SpecEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

class SpecValidator {
    static final String KEY_TIMESTAMP = "ts";
    static final String KEY_WHITELISTED_ALL = "all";
    private final Logger logger;
    private final SpecPayloadValidator specPayloadValidator;

    SpecValidator(Logger logger) {
        this.logger = logger;
        specPayloadValidator = new SpecPayloadValidator(logger);
    }

    MrLocalLocalResult verify(Spec spec, List<LoggedEvent> loggedEvents, long startTimestamp) throws MrLocalLocalException {
        loggedEvents = removeNonWhitelistedEvents(loggedEvents, spec.whitelistedEvents());
        loggedEvents = removeEventsOutOfTimeScope(loggedEvents, startTimestamp);
        loggedEvents = sortEventsByTimestamp(loggedEvents);

        MrLocalLocalResult result = verifyEventCounts(spec.expectedEvents(), loggedEvents);
        if (!result.wasSuccessful()) {
            return result;
        }

        boolean success = true;
        int size = loggedEvents.size();
        int i = 0;
        int j = 0;

        while (i < size) {
            LoggedEvent loggedEvent = loggedEvents.get(i);
            SpecEvent specEvent = spec.expectedEvents().get(j);

            boolean eventsMatch = verifyEvent(i, specEvent, loggedEvent);

            if (eventsMatch) {
                // Events match => Move forward
                i++;
                j++;
                logger.info("👍️ Events match!");
            } else {
                if (specEvent.optional()) {
                    // Spec event was optional => check next event in spec
                    logger.info(String.format("🤷‍♀️ Skipping optional event of type: %s", specEvent.name()));
                    j++;
                } else {
                    // Failure => Continue to complete logging
                    i++;
                    j++;
                    success = false;
                    logger.error("💥 Events don't match!️");
                }
            }
        }

        if (!success) {
            return MrLocalLocalResult.error(false, "🔥 MrLocalLocal validation failed... See logs above for details.");
        }

        return MrLocalLocalResult.success();
    }

    private boolean verifyEvent(int i, SpecEvent specEvent, LoggedEvent loggedEvent) {
        boolean eventNameMatches = verifyEventName(i, specEvent, loggedEvent);
        boolean payloadMatches = verifyEventPayload(i, specEvent, loggedEvent);
        return eventNameMatches && payloadMatches;
    }

    private boolean verifyEventName(int i, SpecEvent specEvent, LoggedEvent loggedEvent) {
        String specEventName = specEvent.name();
        String loggedEventName = loggedEvent.event();

        boolean matches = specEventName.equals(loggedEventName);
        if (matches) {
            logger.info(String.format("Matching event type spec: %s <=> logged: %s", specEventName, loggedEventName));
        } else {
            logger.error(String.format("Matching event type spec: %s <=> logged: %s", specEventName, loggedEventName));
        }
        return matches;
    }

    private MrLocalLocalResult verifyEventCounts(List<SpecEvent> expectedEvents, List<LoggedEvent> loggedEvents) throws MrLocalLocalException {
        int expectedEventCount = expectedEvents.size();
        int minimumExpectedEventCount = expectedEventCount;
        int loggedEventCount = loggedEvents.size();

        for (SpecEvent expectedEvent : expectedEvents) {
            if (expectedEvent.optional()) {
                minimumExpectedEventCount--;
            }
        }

        logger.info(String.format("Spec expects %d events (%d optional) - we logged %d events", expectedEventCount, expectedEventCount - minimumExpectedEventCount, loggedEventCount));
        if (loggedEventCount >= minimumExpectedEventCount && loggedEventCount <= expectedEventCount) {
            return MrLocalLocalResult.success();
        }

        if (loggedEventCount < minimumExpectedEventCount) {
            return MrLocalLocalResult.error(true, String.format("Expected at least %d events but logged %d", minimumExpectedEventCount, loggedEventCount));
        }

        return MrLocalLocalResult.error(false,
                                        String.format("Spec expects %d events (%d optional) - we logged %d events",
                                                      expectedEventCount,
                                                      expectedEventCount - minimumExpectedEventCount,
                                                      loggedEventCount));
    }

    private boolean verifyEventPayload(int i, SpecEvent specEvent, LoggedEvent loggedEvent) {
        Map<String, Object> payloadRequirements = specEvent.params();
        Map<String, Object> loggedPayload = loggedEvent.payload();

        return specPayloadValidator.matchPayload(i, payloadRequirements, loggedPayload);
    }

    private static List<LoggedEvent> removeEventsOutOfTimeScope(List<LoggedEvent> loggedEvents, long startTimestamp) throws MrLocalLocalException {
        if (startTimestamp == 0) {
            return loggedEvents;
        }

        List<LoggedEvent> survivors = new ArrayList<>(loggedEvents.size());
        for (LoggedEvent event : loggedEvents) {
            if (!event.payload().containsKey(KEY_TIMESTAMP)) {
                throw new MrLocalLocalException(String.format("Event doesn't contain timestamp ('%s') key in payload. (╯°□°)╯︵ ┻━┻", KEY_TIMESTAMP));
            }
            long timestamp = Long.valueOf(String.valueOf(event.payload().get(KEY_TIMESTAMP)));
            if (startTimestamp < timestamp) {
                survivors.add(event);
            }
        }
        return survivors;
    }

    private List<LoggedEvent> sortEventsByTimestamp(List<LoggedEvent> loggedEvents) {
        Collections.sort(loggedEvents, new Comparator<LoggedEvent>() {
            @Override
            public int compare(LoggedEvent event1, LoggedEvent event2) {
                long ts1 = Long.valueOf(String.valueOf(event1.payload().get(KEY_TIMESTAMP)));
                long ts2 = Long.valueOf(String.valueOf(event2.payload().get(KEY_TIMESTAMP)));
                return Longs.compare(ts1, ts2);
            }
        });
        return loggedEvents;
    }

    private List<LoggedEvent> removeNonWhitelistedEvents(List<LoggedEvent> loggedEvents, List<String> whitelisted) {
        if (whitelisted.isEmpty()) {
            logger.info("no events whitelisted, allowing all!");
            return loggedEvents;
        }

        if (whitelisted.contains(KEY_WHITELISTED_ALL)) {
            logger.info("all events whitelisted!");
            return loggedEvents;
        }

        List<LoggedEvent> survivors = new ArrayList<>();
        for (LoggedEvent event : loggedEvents) {
            if (whitelisted.contains(event.event())) {
                survivors.add(event);
            }
        }
        return survivors;
    }
}
