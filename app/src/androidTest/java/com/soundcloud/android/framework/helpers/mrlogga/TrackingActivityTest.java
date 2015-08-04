package com.soundcloud.android.framework.helpers.mrlogga;

import com.soundcloud.android.settings.SettingKey;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.utils.BuildHelper;
import com.soundcloud.android.utils.DeviceHelper;
import com.squareup.okhttp.OkHttpClient;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public abstract class TrackingActivityTest<T extends Activity> extends ActivityTest<T> {

    private MrLoggaVerifier verifier;
    private MrLoggaRecorder recorder;
    private String scenarioName;
    private boolean recordMode;

    public TrackingActivityTest(Class<T> activityClass) {
        super(activityClass);
    }

    @Override
    protected void beforeStartActivity() {
        super.beforeStartActivity();

        final Context context = getInstrumentation().getTargetContext();
        final MrLoggaLoggaClient client = new MrLoggaLoggaClient(context,
                new DeviceHelper(context, new BuildHelper()), new OkHttpClient());

        verifier = new MrLoggaVerifier(client, waiter);
        recorder = new MrLoggaRecorder(client);

        enableEventLoggerInstantFlush(context);
    }

    protected void startEventTracking(String scenario) {
        startEventTracking(scenario, false);
    }

    protected void startEventTracking(String scenario, boolean recordMode) {
        updateProperties(scenario, recordMode);

        if (recordMode) {
            recorder.startRecording(scenarioName);
        } else {
            verifier.start();
        }
    }

    private void updateProperties(String scenario, boolean recordMode) {
        this.scenarioName = scenario;
        this.recordMode = recordMode;
    }

    protected void finishEventTracking() {
        verifier.assertScenario(scenarioName);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (recordMode) {
            recorder.stopRecording();
        } else {
            verifier.stop();
        }
    }

    private void enableEventLoggerInstantFlush(Context context) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putBoolean(SettingKey.DEV_FLUSH_EVENTLOGGER_INSTANTLY, true).apply();
    }

}
