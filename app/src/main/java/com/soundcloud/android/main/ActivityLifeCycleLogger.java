package com.soundcloud.android.main;

import com.soundcloud.android.utils.Log;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import org.jetbrains.annotations.Nullable;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

public class ActivityLifeCycleLogger extends DefaultActivityLightCycle<AppCompatActivity> {

    private static final String TAG = "ActivityLifeCycle";

    @Inject
    public ActivityLifeCycleLogger() {
    }

    @Override
    public void onCreate(AppCompatActivity activity, @Nullable Bundle bundle) {
        logLifeCycle(activity, "OnCreate");
    }

    @Override
    public void onStart(AppCompatActivity activity) {
        logLifeCycle(activity, "OnStart");
    }

    @Override
    public void onResume(AppCompatActivity activity) {
        logLifeCycle(activity, "OnResume");
    }

    @Override
    public void onPause(AppCompatActivity activity) {
        logLifeCycle(activity, "OnPause");
    }

    @Override
    public void onStop(AppCompatActivity activity) {
        logLifeCycle(activity, "OnStop");
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        logLifeCycle(activity, "OnDestroy");
    }

    public void logLifeCycle(AppCompatActivity activity, String stage) {
        Log.d(TAG, "[Activity LifeCycle] "+ stage + " : " + activity.getClass().getSimpleName());
    }
}
