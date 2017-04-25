package com.soundcloud.android.mrlocallocal;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.regex.Pattern;

class SpecPayloadValidator {
    private final Logger logger;

    SpecPayloadValidator(Logger logger) {
        this.logger = logger;
    }

    boolean matchPayload(int i, Map<String, Object> payloadRequirements, Map<String, Object> loggedPayload) {
        return matchPayload(i, payloadRequirements, loggedPayload, true);
    }

    private boolean matchPayload(int i, Map<String, Object> payloadRequirements, Map<String, Object> loggedPayload, boolean success) {
        ImmutableMap<String, Object> requirements = ImmutableMap.copyOf(payloadRequirements);
        for (Map.Entry<String, Object> payloadRequirement : requirements.entrySet()) {
            String requiredField = payloadRequirement.getKey();

            boolean payloadMatches;
            boolean exists = checkIfKeyExists(loggedPayload, requiredField, payloadRequirement.getValue());
            if (exists) {
                Object payloadData = loggedPayload.get(requiredField);
                payloadMatches = matchPayloadData(i, success, payloadRequirement.getValue(), requiredField, payloadData);
                // Kill the field from the Map => We later check if something is left unchecked
                loggedPayload.remove(requiredField);
            } else {
                payloadMatches = false;
            }

            success = success && payloadMatches;
        }

        boolean hasUnmatchedEvents = hasUnmatchedFields(i, loggedPayload);

        return !hasUnmatchedEvents && success;
    }

    private boolean hasUnmatchedFields(int i, Map<String, Object> loggedPayload) {
        if (!loggedPayload.isEmpty()) {
            logger.info(String.format("Event #%d contains %d unmatched payload field(s)", i, loggedPayload.size()));
            for (String key : loggedPayload.keySet()) {
                logger.info(String.format("  🤷‍♀️(extra) %s", key));
            }
            return true;
        }
        return false;
    }

    private boolean matchPayloadData(int i, boolean success, Object payloadRequirement, String requiredField, Object payloadData) {
        if (payloadData instanceof Map || payloadRequirement instanceof Map) {
            return matchNestedPayload(i, success, payloadRequirement, requiredField, payloadData);
        } else {
            return matchStringParameter(success, String.valueOf(payloadRequirement), requiredField, payloadData);
        }
    }

    private boolean matchStringParameter(boolean success, String expectedPattern, String requiredField, Object payloadData) {
        String actualPayload = String.valueOf(payloadData);

        if (Pattern.compile(expectedPattern).matcher(actualPayload).matches()) {
            logger.info(String.format("  👌(match) %s: %s <=> %s", requiredField, expectedPattern, actualPayload));
        } else {
            logger.error(String.format("  💥(error) %s: %s <=> %s", requiredField, expectedPattern, actualPayload));
            success = false;
        }
        return success;
    }

    private boolean matchNestedPayload(int i, boolean success, Object payloadRequirement, String requiredField, Object payloadData) {
        if (payloadData instanceof Map && payloadRequirement instanceof Map) {
            logger.info(String.format("entering map: %s", requiredField));
            return matchPayload(i, (Map<String, Object>) payloadRequirement, (Map<String, Object>) payloadData, success);
        } else {
            if (payloadData instanceof Map) {
                logger.error(String.format("  got map - expected value with key: %s", requiredField));
            } else {
                logger.error(String.format("  got value - expected map with key: %s:", requiredField));
            }
            return false;
        }
    }

    private boolean checkIfKeyExists(Map<String, Object> loggedPayload, String requiredField, Object value) {
        if (!loggedPayload.containsKey(requiredField)) {
            logger.error(String.format("  🤷‍♀️(miss) %s: %s <=>", requiredField, value));
            return false;
        }
        return true;
    }
}
