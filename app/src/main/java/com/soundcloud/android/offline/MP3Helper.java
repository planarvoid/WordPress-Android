package com.soundcloud.android.offline;

import java.util.concurrent.TimeUnit;

class MP3Helper {
    private static final int MP3_128_KBPS = 128;

    public static long calculateFileSizeInBytes(long trackDurationMillis) {
        long trackSeconds = TimeUnit.MILLISECONDS.toSeconds(trackDurationMillis);
        long fileSizeKB = trackSeconds * MP3_128_KBPS / 8L; //(KB is kilobytes, not kilobits, hence the 8).
        return fileSizeKB * 1024;
    }
}
