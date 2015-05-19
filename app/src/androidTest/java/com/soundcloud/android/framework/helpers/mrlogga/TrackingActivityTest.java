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

    protected MrLoggaVerifier mrLoggaVerifier;
    protected MrLoggaRecorder mrLoggaRecorder;
    private Context context;

    public TrackingActivityTest(Class<T> activityClass) {
        super(activityClass);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        context = getInstrumentation().getTargetContext();
        final MrLoggaLoggaClient client = new MrLoggaLoggaClient(context, new DeviceHelper(context, new BuildHelper()), new OkHttpClient());

        mrLoggaVerifier = new MrLoggaVerifier(client, waiter);
        mrLoggaRecorder = new MrLoggaRecorder(client);

        enableEventLoggerInstantFlush(context);
    }

    protected void enableEventLoggerInstantFlush(Context context) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putBoolean(SettingKey.DEV_FLUSH_EVENTLOGGER_INSTANTLY, true).apply();
    }

    @Override
    protected boolean shouldSkip() {
        final ApplicationProperties applicationProperties = new ApplicationProperties(context.getResources());
        return !applicationProperties.isDebugBuild();
    }

}
