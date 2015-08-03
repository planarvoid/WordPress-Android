package com.soundcloud.android.framework.helpers.mrlogga;

import com.soundcloud.android.properties.ApplicationProperties;
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

    protected MrLoggaVerifier verifier;
    protected MrLoggaRecorder recorder;

    private Context context;

    private String scenarioName;
    private boolean recordMode;

    public TrackingActivityTest(Class<T> activityClass) {
        super(activityClass);
    }

    @Override
    protected void beforeStartActivity() {
        super.beforeStartActivity();

        context = getInstrumentation().getTargetContext();
        final MrLoggaLoggaClient client = new MrLoggaLoggaClient(context, new DeviceHelper(context, new BuildHelper()), new OkHttpClient());

        verifier = new MrLoggaVerifier(client, waiter);
        recorder = new MrLoggaRecorder(client);

        enableEventLoggerInstantFlush(context);
        if (recordMode) {
            recorder.startRecording(scenarioName);
        } else {
            verifier.start();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        if (recordMode) {
            recorder.stopRecording();
        } else {
            verifier.stop();
        }
        super.tearDown();
    }

    protected void recordScenario(String scenarioName) {
        // To record a scenario: override beforeStartActivity and call this method before super
        this.recordMode = true;
        this.scenarioName = scenarioName;
    }

    protected void enableEventLoggerInstantFlush(Context context) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putBoolean(SettingKey.DEV_FLUSH_EVENTLOGGER_INSTANTLY, true).apply();
    }

    @Override
    protected boolean shouldRunTest() {
        return new ApplicationProperties(context.getResources()).isDebugBuild();
    }

}
