package com.soundcloud.android.sync;

import android.content.SyncResult;
import android.net.Uri;
import android.support.annotation.VisibleForTesting;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Deprecated
public class LegacySyncResult {

    public static final int UNCHANGED = 0;
    public static final int REORDERED = 1;
    public static final int CHANGED   = 2;

    protected static final int GENERAL_ERROR_MINIMUM_DELAY = 10;
    protected static final int GENERAL_ERROR_DELAY_RANGE = 20;

    public final Uri uri;
    public final SyncResult syncResult = new SyncResult();

    /** One of {@link #UNCHANGED}, {@link #REORDERED}, {@link #CHANGED}. */
    public int change;

    public boolean success;

    public long synced_at;
    public int new_size;
    public String extra;

    public LegacySyncResult(Uri uri) {
        this.uri = uri;
    }

    @Deprecated //Use static factory methods (or add your own)
    public void setSyncData(boolean success, long synced_at, int new_size, int change){
        this.success = success;
        this.synced_at = synced_at;
        this.new_size = new_size;
        this.change = change;
    }

    public void setSyncData(long synced_at, int new_size){
        this.synced_at = synced_at;
        this.new_size = new_size;
    }

    public static LegacySyncResult fromSuccessfulChange(Uri uri) {
        LegacySyncResult result = new LegacySyncResult(uri);
        result.success = true;
        result.synced_at = System.currentTimeMillis();
        result.change = CHANGED;
        return result;
    }

    public static LegacySyncResult fromSuccessWithoutChange(Uri uri) {
        LegacySyncResult result = new LegacySyncResult(uri);
        result.success = true;
        result.synced_at = System.currentTimeMillis();
        result.change = UNCHANGED;
        return result;
    }

    public static LegacySyncResult fromAuthException(Uri uri) {
        LegacySyncResult r = new LegacySyncResult(uri);
        r.syncResult.stats.numAuthExceptions++;
        return r;
    }

    public static LegacySyncResult fromIOException(Uri uri) {
        LegacySyncResult r = new LegacySyncResult(uri);
        r.syncResult.stats.numIoExceptions++;
        return r;
    }

    public static LegacySyncResult fromUnexpectedResponse(Uri uri, int statusCode) {
        final LegacySyncResult legacySyncResult = new LegacySyncResult(uri);

        if (statusCode >= 500){
            setDelayUntilToOneSyncInterval(legacySyncResult);
        }

        return legacySyncResult;
    }

    public static LegacySyncResult fromServerError(Uri uri) {
        final LegacySyncResult legacySyncResult = new LegacySyncResult(uri);
        setDelayUntilToOneSyncInterval(legacySyncResult);
        return legacySyncResult;
    }

    public static LegacySyncResult fromClientError(Uri uri) {
        return new LegacySyncResult(uri);
    }

    private static void setDelayUntilToOneSyncInterval(LegacySyncResult legacySyncResult) {
        // http://developer.android.com/reference/android/content/SyncResult.html#delayUntil
        legacySyncResult.syncResult.delayUntil = SyncConfig.DEFAULT_SYNC_DELAY;
    }

    public static LegacySyncResult fromGeneralFailure(Uri uri) {
        return fromGeneralFailure(uri, new Random());
    }

    @VisibleForTesting
    static LegacySyncResult fromGeneralFailure(Uri uri, Random random) {
        final LegacySyncResult legacySyncResult = new LegacySyncResult(uri);
        legacySyncResult.syncResult.delayUntil = getRandomizedDelayTime(random, GENERAL_ERROR_MINIMUM_DELAY, GENERAL_ERROR_DELAY_RANGE);
        return legacySyncResult;
    }

    private static long getRandomizedDelayTime(Random random, int minimumDelay, int range) {
        return TimeUnit.MINUTES.toSeconds(minimumDelay + random.nextInt(range + 1));
    }

    @Override
    public String toString() {
        return "Result{" +
                "uri=" + uri +
                ", syncResult=" + syncResult +
                ", change=" + change +
                ", success=" + success +
                ", synced_at=" + synced_at +
                ", new_size=" + new_size +
                ", extra='" + extra + '\'' +
                '}';
    }
}
