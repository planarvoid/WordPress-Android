package com.soundcloud.android.framework.helpers.mrlogga;

import static junit.framework.Assert.assertTrue;

import com.soundcloud.android.framework.Waiter;

public class MrLoggaVerifier {
    private final MrLoggaLoggaClient client;
    private final Waiter waiter;

    public MrLoggaVerifier(MrLoggaLoggaClient client, Waiter waiter) {
        this.client = client;
        this.waiter = waiter;
    }

    public void startLogging() {
        MrLoggaResponse response = client.startLogging();
        assertTrue("Failed to start MrLoggaLogga logging\n" + response.responseBody, response.success);
        
        waiter.waitFiveSeconds();
    }

    public void assertScenario(String scenarioId) {
        final MrLoggaResponse response = client.validate(scenarioId);
        assertTrue("Error validating scenario: " + scenarioId + " on device " + client.deviceUDID + " \n" + response.responseBody, response.success);
    }

    public void stopLogging() {
        // This is a temporary solution
        // https://github.com/soundcloud/mr-logger-logger/issues/29
        waiter.waitFiveSeconds();
        waiter.waitFiveSeconds();

        MrLoggaResponse response = client.stopLogging();
        assertTrue("Failed to finish MrLoggaLogga logging\n" + response.responseBody, response.success);
    }

}
