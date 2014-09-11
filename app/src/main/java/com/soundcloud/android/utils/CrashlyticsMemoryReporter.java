package com.soundcloud.android.utils;

import com.crashlytics.android.Crashlytics;

import android.app.ActivityManager;

import javax.inject.Inject;

public class CrashlyticsMemoryReporter extends MemoryReporter {

    @Inject
    public CrashlyticsMemoryReporter(ActivityManager activityManager) {
        super(activityManager);
    }

    @Override
    protected void logLowSystemMemoryState(boolean lowSystem) {
        super.logLowSystemMemoryState(lowSystem);
        Crashlytics.setBool("low system memory state", lowSystem);
    }

    @Override
    protected void logAvailableSystemMemory(long availableSystemMb) {
        super.logAvailableSystemMemory(availableSystemMb);
        Crashlytics.setLong("available system memory (MB)", availableSystemMb);
    }

    @Override
    protected void logNativeHeapStats(long nativeFreeKb, long nativeSizeKb) {
        super.logNativeHeapStats(nativeFreeKb, nativeSizeKb);
        Crashlytics.setString("native heap free / total in kb", nativeFreeKb + "/" + nativeSizeKb);
    }

    @Override
    protected void logDalvikHeapStats(long free, long total, long max) {
        super.logDalvikHeapStats(free, total, max);
        Crashlytics.setString("dalvik heap free / current max / hard max in kb", free + "/" + total + "/" + max);
    }

    @Override
    protected void logTrim(String message) {
        super.logTrim(message);
        Crashlytics.log(message);
    }

    @Override
    protected void logMaximum(long maxMemoryFromRuntimeInMb) {
        super.logMaximum(maxMemoryFromRuntimeInMb);
        Crashlytics.setLong("max memory reported by JVM (MB)", maxMemoryFromRuntimeInMb);
    }

    @Override
    protected void logThreshold(long memoryThresholdInMb) {
        super.logThreshold(memoryThresholdInMb);
        Crashlytics.setLong("low memory threshold (MB)", memoryThresholdInMb);
    }

    @Override
    protected void logClass(int memoryClass) {
        super.logClass(memoryClass);
        Crashlytics.setLong("memory class (MB)", (long) memoryClass);
    }

    @Override
    protected void logLargeClass(long largeMemoryClass) {
        super.logLargeClass(largeMemoryClass);
        Crashlytics.setLong("large memory class (MB)", largeMemoryClass);
    }
}
