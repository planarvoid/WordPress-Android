package com.soundcloud.android.sync;

import com.google.common.annotations.VisibleForTesting;

import android.content.SyncResult;
import android.net.Uri;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ApiSyncResult {

    public static final int UNCHANGED = 0;
    public static final int REORDERED = 1;
    public static final int CHANGED   = 2;

    protected static final int UNEXPECTED_RESPONSE_MINIMUM_DELAY = 20;
    protected static final int UNEXPECTED_RESPONSE_DELAY_RANGE = 40;

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

    public ApiSyncResult(Uri uri) {
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

    public static ApiSyncResult fromSuccessfulChange(Uri uri) {
        ApiSyncResult result = new ApiSyncResult(uri);
        result.success = true;
        result.synced_at = System.currentTimeMillis();
        result.change = CHANGED;
        return result;
    }

    public static ApiSyncResult fromSuccessWithoutChange(Uri uri) {
        ApiSyncResult result = new ApiSyncResult(uri);
        result.success = true;
        result.synced_at = System.currentTimeMillis();
        result.change = UNCHANGED;
        return result;
    }

    public static ApiSyncResult fromAuthException(Uri uri) {
        ApiSyncResult r = new ApiSyncResult(uri);
        r.syncResult.stats.numAuthExceptions++;
        return r;
    }

    public static ApiSyncResult fromIOException(Uri uri) {
        ApiSyncResult r = new ApiSyncResult(uri);
        r.syncResult.stats.numIoExceptions++;
        return r;
    }

    public static ApiSyncResult fromUnexpectedResponse(Uri uri, int statusCode) {
        return fromUnexpectedResponse(uri, statusCode, new Random());
    }

    public static ApiSyncResult fromServerError(Uri uri) {
        final ApiSyncResult apiSyncResult = new ApiSyncResult(uri);
        setDelayUntilToRandomDelay(new Random(), apiSyncResult);
        return apiSyncResult;
    }

    public static ApiSyncResult fromClientError(Uri uri) {
        return new ApiSyncResult(uri);
    }

    @VisibleForTesting
    static ApiSyncResult fromUnexpectedResponse(Uri uri, int statusCode, Random random) {
        final ApiSyncResult apiSyncResult = new ApiSyncResult(uri);

        if (statusCode >= 500){
            setDelayUntilToRandomDelay(random, apiSyncResult);
        }

        return apiSyncResult;
    }

    private static void setDelayUntilToRandomDelay(Random random, ApiSyncResult apiSyncResult) {
        // http://developer.android.com/reference/android/content/SyncResult.html#delayUntil
        apiSyncResult.syncResult.delayUntil = getRandomizedDelayTime(random, UNEXPECTED_RESPONSE_MINIMUM_DELAY, UNEXPECTED_RESPONSE_DELAY_RANGE);
    }

    public static ApiSyncResult fromGeneralFailure(Uri uri) {
        return fromGeneralFailure(uri, new Random());
    }

    @VisibleForTesting
    static ApiSyncResult fromGeneralFailure(Uri uri, Random random) {
        final ApiSyncResult apiSyncResult = new ApiSyncResult(uri);
        apiSyncResult.syncResult.delayUntil = getRandomizedDelayTime(random, GENERAL_ERROR_MINIMUM_DELAY, GENERAL_ERROR_DELAY_RANGE);
        return apiSyncResult;
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
