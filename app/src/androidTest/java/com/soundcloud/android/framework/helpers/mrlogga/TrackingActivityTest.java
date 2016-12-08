package com.soundcloud.android.framework.helpers.mrlogga;

import com.soundcloud.android.api.json.JacksonJsonTransformer;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.settings.SettingKey;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.utils.BuildHelper;
import com.soundcloud.android.utils.DeviceHelper;
import okhttp3.OkHttpClient;

import android.app.Activity;
import android.content.Context;
import android.preference.PreferenceManager;

public abstract class TrackingActivityTest<T extends Activity> extends ActivityTest<T> {

    private MrLoggaVerifier verifier;
    private MrLoggaRecorder recorder;
    private boolean recordMode;
    private static boolean isRecording = false;

    public TrackingActivityTest(Class<T> activityClass) {
        super(activityClass);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // promoted tracking blocks eventlogger tests, so disable until we find a better way
        ConfigurationHelper.disablePromotedAnalytics(getInstrumentation().getTargetContext());
    }

    @Override
    protected void beforeLogIn() {
        super.beforeLogIn();

        final Context context = getInstrumentation().getTargetContext();
        final MrLoggaLoggaClient client = new MrLoggaLoggaClient(context,
                                                                 new DeviceHelper(context, new BuildHelper(), context.getResources()),
                                                                 new OkHttpClient(), new JacksonJsonTransformer());

        verifier = new MrLoggaVerifier(client, waiter);
        recorder = new MrLoggaRecorder(client);

        enableEventLoggerInstantFlush(context);
    }

    protected void startScenario(final String scenario) {
        if (isRecording) {
            startEventRecording(scenario);
        } else {
            startEventTracking();
        }
    }

    protected void endScenario(final String scenario) {
        if (isRecording) {
            // nothing, recorded scenario will automatically be closed
        } else {
            finishEventTracking(scenario);
        }
    }

    protected void startEventTracking() {
        verifier.start();
    }

    @SuppressWarnings("unused")
    protected void startEventRecording(String scenario) {
        recordMode = true;
        recorder.startRecording(scenario);
    }

    protected void finishEventTracking(String scenario) {
        verifier.assertScenario(scenario);
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
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(SettingKey.DEV_FLUSH_EVENTLOGGER_INSTANTLY, true)
                .apply();
    }

}
