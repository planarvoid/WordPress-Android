package com.soundcloud.android.utils;

import com.crashlytics.android.Crashlytics;

public class CrashlyticsMemoryReporter extends MemoryReporter {

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

}
