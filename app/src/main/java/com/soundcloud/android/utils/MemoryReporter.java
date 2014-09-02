package com.soundcloud.android.utils;

import android.app.ActivityManager;
import android.content.Context;

import javax.inject.Inject;

public class MemoryReporter {
    private static final String LOG_TAG = "MEM";

    @Inject
    public MemoryReporter() {
        // for Dagger
    }

    public void reportMemoryTrim(int level) {
        logTrim("Trim memory received with level " + level);
    }

    public void reportSystemMemoryStats(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        int memoryClass = activityManager.getMemoryClass();
        logClass(memoryClass);

        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        long memoryThresholdInMb = (memoryInfo.threshold / 1024 / 1024);
        logThreshold(memoryThresholdInMb);

        long maxMemoryFromRuntimeInMb = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        logMaximum(maxMemoryFromRuntimeInMb);
    }

    protected void logTrim(String message) {
        Log.i(LOG_TAG, message);
    }

    protected void logMaximum(long maxMemoryFromRuntimeInMb) {
        Log.i(LOG_TAG, "max memory from runtime: " + maxMemoryFromRuntimeInMb + "MB");
    }

    protected void logThreshold(long memoryThresholdInMb) {
        Log.i(LOG_TAG, "low memory threshold: " + memoryThresholdInMb + "MB");
    }

    protected void logClass(int memoryClass) {
        Log.i(LOG_TAG, "memory class: " + memoryClass + "MB");
    }
}
