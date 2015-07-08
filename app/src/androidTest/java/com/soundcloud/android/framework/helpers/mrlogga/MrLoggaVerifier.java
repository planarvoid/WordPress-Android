package com.soundcloud.android.framework.helpers.mrlogga;

import static junit.framework.Assert.assertTrue;

import com.robotium.solo.Condition;
import com.soundcloud.android.framework.Waiter;

public class MrLoggaVerifier {
    private final MrLoggaLoggaClient client;
    private final Waiter waiter;
    private boolean isLogSessionActive;

    public MrLoggaVerifier(MrLoggaLoggaClient client, Waiter waiter) {
        this.client = client;
        this.waiter = waiter;
    }

    public void start() {
        startLoggingTrackingEvents();
    }

    public void stop() {
        // We need to stop the verifier from the tear down method in tests - otherwise we could end up
        // with unclosed session because the test failed before it assertScenario was called.
        //
        // On the other hand, mrloggalogga does not allow to stop a session that was already stopped - ie when
        // assertScenario was actually called. That's hy we need this condition.
        if (isLogSessionActive) {
            stopLoggingTrackingEvents();
        }
    }

    public void assertScenario(final String scenarioId) {
        // Do not check if it was successful or not since we still want to validate the responsce
        // to get a qualified error message
        waiter.waitForNetworkCondition(new IsScenarioComplete(client, scenarioId));
        stopLoggingTrackingEvents();
        assertScenarioImmediately(scenarioId);
    }

    private void startLoggingTrackingEvents() {
        MrLoggaResponse startLoggingResponse = client.startLogging();
        isLogSessionActive = startLoggingResponse.success;
        assertTrue("Failed to start MrLoggaLogga logging\n" + startLoggingResponse.responseBody, startLoggingResponse.success);
    }

    private void stopLoggingTrackingEvents() {
        MrLoggaResponse stopLoggingSession = client.stopLogging();
        isLogSessionActive = !stopLoggingSession.success;
        assertTrue("Failed to finish MrLoggaLogga logging\n" + stopLoggingSession.responseBody, stopLoggingSession.success);
    }

    private void assertScenarioImmediately(String scenarioId) {
        final MrLoggaResponse response = client.validate(scenarioId);
        assertTrue("Error validating scenario: " + scenarioId + " on device " + client.deviceUDID + " \n" + response.responseBody, response.success);
    }

    static class IsScenarioComplete implements Condition {
        private final MrLoggaLoggaClient client;
        private final String scenarioId;

        IsScenarioComplete(MrLoggaLoggaClient client, String scenarioId) {
            this.client = client;
            this.scenarioId = scenarioId;
        }

        @Override
        public boolean isSatisfied() {
            return client.isScenarioComplete(scenarioId);
        }
    }
}
