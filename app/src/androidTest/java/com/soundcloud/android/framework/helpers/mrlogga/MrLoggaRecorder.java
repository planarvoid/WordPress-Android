package com.soundcloud.android.framework.helpers.mrlogga;

import static junit.framework.Assert.assertNotNull;

public class MrLoggaRecorder {
    private final MrLoggaLoggaClient client;

    public MrLoggaRecorder(MrLoggaLoggaClient client) {
        this.client = client;
    }

    public void startRecording(String scenarioName) {
        assertNotNull("You have to name scenario you want to record. ", scenarioName);
        client.startRecording(scenarioName);
    }

    public void stopRecording() {
        client.finishRecording();
    }


}
