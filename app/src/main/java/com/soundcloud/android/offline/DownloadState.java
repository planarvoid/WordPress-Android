package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.DownloadOperations.ConnectionState;
import static com.soundcloud.android.offline.MP3Helper.calculateFileSizeInBytes;

import com.soundcloud.android.Consts;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.objects.MoreObjects;

public final class DownloadState {

    private enum Status {PROGRESS, SUCCESS, CANCELLED, UNAVAILABLE, NOT_ENOUGH_SPACE, NOT_ENOUGH_MINIMUM_SPACE, CONNECTION_ERROR, ERROR}

    final Status status;
    final DownloadRequest request;
    final long timestamp;
    final ConnectionState connectionState;
    long progress = Consts.NOT_SET;

    private DownloadState(Status status, DownloadRequest request) {
        this(status, request, Consts.NOT_SET);
    }

    public DownloadState(Status status, DownloadRequest request, long progress) {
        this(status, request, progress, null);
    }

    private DownloadState(Status status, DownloadRequest request, ConnectionState connectionState) {
        this(status, request, Consts.NOT_SET, connectionState);
    }

    private DownloadState(Status status, DownloadRequest request, long progress, ConnectionState connectionState) {
        this.status = status;
        this.request = request;
        this.timestamp = System.currentTimeMillis();
        this.progress = progress;
        this.connectionState = connectionState;
    }

    public static DownloadState success(DownloadRequest request) {
        Log.d(OfflineContentService.TAG, "Successful download result: " + request.getTrack());
        return new DownloadState(Status.SUCCESS, request, calculateFileSizeInBytes(request.getDuration()));
    }

    public static DownloadState unavailable(DownloadRequest request) {
        Log.d(OfflineContentService.TAG, "Unavailable download result: " + request.getTrack());
        return new DownloadState(Status.UNAVAILABLE, request);
    }

    public static DownloadState connectionError(DownloadRequest request, ConnectionState connectionState) {
        Log.d(OfflineContentService.TAG, "Connection error download result: " + request.getTrack());
        return new DownloadState(Status.CONNECTION_ERROR, request, connectionState);
    }

    public static DownloadState inProgress(DownloadRequest request, long progress) {
        return new DownloadState(Status.PROGRESS, request, progress);
    }

    public static DownloadState notEnoughSpace(DownloadRequest request) {
        Log.d(OfflineContentService.TAG, "Not enough space download result: " + request.getTrack());
        return new DownloadState(Status.NOT_ENOUGH_SPACE, request);
    }

    public static DownloadState notEnoughMinimumSpace(DownloadRequest request) {
        Log.d(OfflineContentService.TAG, "Not enough minimum space");
        return new DownloadState(Status.NOT_ENOUGH_MINIMUM_SPACE, request);
    }

    public static DownloadState canceled(DownloadRequest request) {
        Log.d(OfflineContentService.TAG, "Download cancelled: " + request.getTrack());
        return new DownloadState(Status.CANCELLED, request);
    }

    public static DownloadState error(DownloadRequest request) {
        return new DownloadState(Status.ERROR, request);
    }

    public boolean isInProgress() {
        return status == Status.PROGRESS;
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

    public boolean isNotEnoughMinimumSpace() {
        return status == Status.NOT_ENOUGH_MINIMUM_SPACE;
    }

    public boolean isDownloadFailed() {
        return status == Status.ERROR;
    }

    public Urn getTrack() {
        return request.getTrack();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getProgress() {
        return progress;
    }

    public long getTotalBytes() {
        return calculateFileSizeInBytes(request.getDuration());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("status", status)
                .add("request", request)
                .add("timestamp", timestamp)
                .toString();
    }
}
