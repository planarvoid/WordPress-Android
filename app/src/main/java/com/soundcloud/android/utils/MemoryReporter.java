package com.soundcloud.android.utils;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.os.Build;
import android.os.Debug;

public class MemoryReporter {
    private static final String LOG_TAG = "MEM";
    public static final ActivityManager.MemoryInfo MEMORY_INFO = new ActivityManager.MemoryInfo();
    private final ActivityManager activityManager;

    public MemoryReporter(ActivityManager activityManager) {
        this.activityManager = activityManager;
    }

    public void reportMemoryTrim(int level) {
        logTrim("Trim memory received with level " + level);
    }

    public void reportSystemMemoryStats() {
        int memoryClass = activityManager.getMemoryClass();
        logClass(memoryClass);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getAndLogLargeMemoryClass();
        }

        activityManager.getMemoryInfo(MEMORY_INFO);
        long memoryThresholdInMb = bytesToMb(MEMORY_INFO.threshold);
        logThreshold(memoryThresholdInMb);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void getAndLogLargeMemoryClass() {
        int largeMemoryClass = activityManager.getLargeMemoryClass();
        logLargeClass(largeMemoryClass);
    }

    public void reportOomStats() {
        Runtime runtime = Runtime.getRuntime();
        long freeKb = bytesToKb(runtime.freeMemory());
        long totalKb = bytesToKb(runtime.totalMemory());
        long maxKb = bytesToKb(runtime.maxMemory());
        logDalvikHeapStats(freeKb, totalKb, maxKb);

        long nativeFreeKb = bytesToKb(Debug.getNativeHeapFreeSize());
        long nativeSizeKb = bytesToKb(Debug.getNativeHeapSize());
        logNativeHeapStats(nativeFreeKb, nativeSizeKb);

        activityManager.getMemoryInfo(MEMORY_INFO);
        long availableSystemMb = MEMORY_INFO.availMem / 1024 / 1024;
        logAvailableSystemMemory(availableSystemMb);
        boolean lowSystem = MEMORY_INFO.lowMemory;
        logLowSystemMemoryState(lowSystem);
    }

    protected void logLowSystemMemoryState(boolean lowSystem) {
        Log.i(LOG_TAG, "system is in low memory state: " + lowSystem);
    }

    protected void logAvailableSystemMemory(long availableSystemMb) {
        Log.i(LOG_TAG, "available system memory (MB): " + availableSystemMb);
    }

    protected void logNativeHeapStats(long nativeFreeKb, long nativeSizeKb) {
        Log.i(LOG_TAG, "native heap free / total in kb: " + nativeFreeKb + "/" + nativeSizeKb);
    }

    protected void logDalvikHeapStats(long free, long total, long max) {
        Log.i(LOG_TAG, "dalvik heap free / current max / hard max in kb: " + free + "/" + total + "/" + max);
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
        Log.i(LOG_TAG, "memory class (our limit): " + memoryClass + "MB");
    }

    protected void logLargeClass(long largeMemoryClass) {
        Log.i(LOG_TAG, "large memory class: " + largeMemoryClass + "MB");
    }

    private long bytesToMb(long bytes) {
        return bytes / 1024 / 1024;
    }

    private long bytesToKb(long bytes) {
        return bytes / 1024;
    }
}
