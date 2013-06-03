package com.soundcloud.android.service.sync;

import org.jetbrains.annotations.Nullable;

import android.content.SyncResult;
import android.net.Uri;

public class ApiSyncResult {
    public static final int UNCHANGED = 0;
    public static final int REORDERED = 1;
    public static final int CHANGED   = 2;

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

    public void setSyncData(boolean success, long synced_at, int new_size, int change){
        this.success = success;
        this.synced_at = synced_at;
        this.new_size = new_size;
        this.change = change;
    }

    public void setSyncData(long synced_at, int new_size, @Nullable String extra){
        this.synced_at = synced_at;
        this.new_size = new_size;
        this.extra = extra;
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
