package com.soundcloud.android.mrlocallocal;

import com.soundcloud.android.mrlocallocal.data.LoggedEvent;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SpecPrinter {
    private static final String ANONYMOUS_ID = "anonymous_id";
    private static final String TIMESTAMP = "ts";
    private static final String APP_VERSION = "app_version";
    private static final String CLIENT_EVENT_ID = "client_event_id";

    private final Logger logger;
    private final EventLogger eventLogger;

    public SpecPrinter(Logger logger, EventLogger eventLogger) {
        this.logger = logger;
        this.eventLogger = eventLogger;
    }

    public void printSpec(long startTimestamp, String... whiteListedEventsArray) throws IOException {
        final List<String> whiteListedEvents = Arrays.asList(whiteListedEventsArray);
        logger.info("Printing events");
        StringBuilder sb = new StringBuilder();
        sb.append("--- !ruby/object:MrLoggerLogger::ResultSpec").append("\n");

        sb.append("whitelisted_events:").append("\n");

        if (!whiteListedEvents.isEmpty()) {
            for (String whiteListedEvent : whiteListedEvents) {
                sb.append("- ").append(whiteListedEvent).append("\n");
            }
        } else {
            for (LoggedEvent loggedEvent : eventLogger.getLoggedEvents()) {
                sb.append("- ").append(loggedEvent.event()).append("\n");
            }
        }

        sb.append("expected_events:").append("\n");
        for (LoggedEvent loggedEvent : eventLogger.getLoggedEvents()) {
            if ((whiteListedEvents.isEmpty() || whiteListedEvents.contains(loggedEvent.event())) && isAfterStart(loggedEvent, startTimestamp)) {
                sb.append("- !ruby/object:MrLoggerLogger::Event").append("\n");
                sb.append("  name: ").append(loggedEvent.event()).append("\n");
                sb.append("  params: ").append("\n");
                printMap(sb, loggedEvent.payload(), 4);
                sb.append("  version: ").append("'1'").append("\n");
            }
        }
        logger.info(sb.toString());
    }

    private boolean isAfterStart(LoggedEvent loggedEvent, long startTimestamp) {
        if (loggedEvent.payload().containsKey(TIMESTAMP)) {
            return (Long) loggedEvent.payload().get(TIMESTAMP) >= startTimestamp;
        }
        return true;
    }

    private void printMap(final StringBuilder sb, final Map<String, Object> payload, final int offset) {
        for (String key : payload.keySet()) {
            for (int i = 0; i < offset; i++) {
                sb.append(" ");
            }
            final Object obj = payload.get(key);
            if (replaceWithWildCard(key)) {
                sb.append(key).append(": ").append(keyToWildCard(key)).append("\n");
            } else if (obj instanceof Map) {
                sb.append(key).append(": ").append("\n");
                final int newOffset = offset + 2;
                printMap(sb, ((Map<String, Object>) obj), newOffset);
            } else {
                sb.append(key).append(": ").append(obj).append("\n");
            }
        }
    }

    private boolean replaceWithWildCard(String key) {
        return key.equals(CLIENT_EVENT_ID) || key.equals(ANONYMOUS_ID) || key.equals(TIMESTAMP) || key.equals(APP_VERSION);
    }

    private String keyToWildCard(String key) {
        switch (key) {
            case CLIENT_EVENT_ID:
            case ANONYMOUS_ID:
                return "(\\w|-)+";
            case TIMESTAMP:
            case APP_VERSION:
                return "'[0-9]+'";
            default:
                throw new IllegalArgumentException("We cannot convert key to wildcard");
        }
    }
}
