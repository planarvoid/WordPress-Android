package com.soundcloud.android.analytics;

import static android.util.Log.INFO;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

/**
 * Purely used to log orientation to fabric so we can debug user journey's better
 */
public class OrientationLogger extends DefaultActivityLightCycle<AppCompatActivity> {

    @Inject
    public OrientationLogger() {
    }

    @Override
    public void onCreate(AppCompatActivity host, Bundle bundle) {
        super.onCreate(host, bundle);
        ErrorUtils.log(INFO, SoundCloudApplication.TAG, host + " created with orientation: " +
                (AndroidUtils.getScreenOrientation(host) == Configuration.ORIENTATION_LANDSCAPE ? "landscape" : "portrait"));
    }
}
