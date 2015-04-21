package com.soundcloud.android.framework.helpers.mrlogga;

import static junit.framework.Assert.assertTrue;

public class MrLoggaVerifier {
    private final MrLoggaLoggaClient client;

    public MrLoggaVerifier(MrLoggaLoggaClient client) {
        this.client = client;
    }

    public void startLogging() {
        MrLoggaResponse response = client.startLogging();
        assertTrue("Failed to start MrLoggaLogga logging\n" + response.responseBody, response.success);
    }

    public void finishLogging() {
        MrLoggaResponse response = client.finishLogging();
        assertTrue("Failed to finish MrLoggaLogga logging\n" + response.responseBody, response.success);
    }

    public void isValid(String scenarioId) {
        final MrLoggaResponse response = client.validate(scenarioId);
        assertTrue("Error validating scenario: " + scenarioId + "\n" + response.responseBody, response.success);
    }


}
