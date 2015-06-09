package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.DownloadOperations.ConnectionState;

import com.google.common.base.Objects;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.Log;

public final class DownloadResult {

    private enum Status {SUCCESS, CANCELLED, UNAVAILABLE, NOT_ENOUGH_SPACE, CONNECTION_ERROR, ERROR}

    final Status status;
    final DownloadRequest request;
    final long timestamp;
    final ConnectionState connectionState;

    private DownloadResult(Status status, DownloadRequest request) {
        this(status, request, null);
    }

    private DownloadResult(Status status, DownloadRequest request, ConnectionState connectionState) {
        this.status = status;
        this.request = request;
        this.timestamp = System.currentTimeMillis();
        this.connectionState = connectionState;
    }

    public static DownloadResult success(DownloadRequest request) {
        Log.d(OfflineContentService.TAG, "Successful download result: " + request.track);
        return new DownloadResult(Status.SUCCESS, request);
    }

    public static DownloadResult unavailable(DownloadRequest request) {
        Log.d(OfflineContentService.TAG, "Unavailable download result: " + request.track);
        return new DownloadResult(Status.UNAVAILABLE, request);
    }

    public static DownloadResult connectionError(DownloadRequest request, ConnectionState connectionState) {
        Log.d(OfflineContentService.TAG, "Connection error download result: " + request.track);
        return new DownloadResult(Status.CONNECTION_ERROR, request, connectionState);
    }

    public static DownloadResult notEnoughSpace(DownloadRequest request) {
        Log.d(OfflineContentService.TAG, "Not enough space download result: " + request.track);
        return new DownloadResult(Status.NOT_ENOUGH_SPACE, request);
    }

    public static DownloadResult canceled(DownloadRequest request) {
        Log.d(OfflineContentService.TAG, "Download cancelled: "+ request.track);
        return new DownloadResult(Status.CANCELLED, request);
    }

    public static DownloadResult error(DownloadRequest request) {
        return new DownloadResult(Status.ERROR, request);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isCancelled() {
        return status == Status.CANCELLED;
    }

    public boolean isConnectionError() {
        return status == Status.CONNECTION_ERROR;
    }

    public boolean isUnavailable() {
        return status == Status.UNAVAILABLE;
    }

    public boolean isNotEnoughSpace() {
        return status == Status.NOT_ENOUGH_SPACE;
    }

    public boolean isDownloadFailed() {
        return status == Status.ERROR;
    }

    public Urn getTrack() {
        return request.track;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("status", status)
                .add("request", request)
                .add("timestamp", timestamp)
                .toString();
    }
}
