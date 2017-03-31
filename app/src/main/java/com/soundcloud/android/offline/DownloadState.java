package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.MP3Helper.calculateFileSizeInBytes;

import com.soundcloud.android.Consts;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.objects.MoreObjects;

public final class DownloadState {

    private enum Status {PROGRESS, SUCCESS, CANCELLED, UNAVAILABLE, NOT_ENOUGH_SPACE, NOT_ENOUGH_MINIMUM_SPACE, INACCESSIBLE_STORAGE, CONNECTIVITY_ERROR, ERROR}

    final DownloadRequest request;
    final long timestamp;
    final boolean isNetworkError;

    private final Status status;
    private long progress = Consts.NOT_SET;

    private DownloadState(Status status, DownloadRequest request) {
        this(status, request, Consts.NOT_SET);
    }

    public DownloadState(Status status, DownloadRequest request, long progress) {
        this(status, request, progress, false);
    }

    private DownloadState(Status status, DownloadRequest request, boolean isNetworkError) {
        this(status, request, Consts.NOT_SET, isNetworkError);
    }

    private DownloadState(Status status, DownloadRequest request, long progress, boolean isNetworkError) {
        this.status = status;
        this.request = request;
        this.timestamp = System.currentTimeMillis();
        this.progress = progress;
        this.isNetworkError = isNetworkError;
    }

    public static DownloadState success(DownloadRequest request) {
        Log.d(OfflineContentService.TAG, "Successful download result: " + request.getUrn());
        return new DownloadState(Status.SUCCESS, request, calculateFileSizeInBytes(request.getDuration()));
    }

    public static DownloadState unavailable(DownloadRequest request) {
        Log.d(OfflineContentService.TAG, "Unavailable download result: " + request.getUrn());
        return new DownloadState(Status.UNAVAILABLE, request);
    }

    static DownloadState disconnectedNetworkError(DownloadRequest request) {
        Log.d(OfflineContentService.TAG, "Connection error download result: " + request.getUrn());
        return new DownloadState(Status.CONNECTIVITY_ERROR, request, true);
    }

    static DownloadState invalidNetworkError(DownloadRequest request) {
        Log.d(OfflineContentService.TAG, "Invalid network error download result: " + request.getUrn());
        return new DownloadState(Status.CONNECTIVITY_ERROR, request, false);
    }

    static DownloadState inProgress(DownloadRequest request, long progress) {
        return new DownloadState(Status.PROGRESS, request, progress);
    }

    static DownloadState notEnoughSpace(DownloadRequest request) {
        Log.d(OfflineContentService.TAG, "Not enough space download result: " + request.getUrn());
        return new DownloadState(Status.NOT_ENOUGH_SPACE, request);
    }

    static DownloadState notEnoughMinimumSpace(DownloadRequest request) {
        Log.d(OfflineContentService.TAG, "Not enough minimum space");
        return new DownloadState(Status.NOT_ENOUGH_MINIMUM_SPACE, request);
    }

    static DownloadState inaccessibleStorage(DownloadRequest request) {
        Log.d(OfflineContentService.TAG, "Inaccessible storage");
        return new DownloadState(Status.INACCESSIBLE_STORAGE, request);
    }

    public static DownloadState canceled(DownloadRequest request) {
        Log.d(OfflineContentService.TAG, "Download cancelled: " + request.getUrn());
        return new DownloadState(Status.CANCELLED, request);
    }

    public static DownloadState error(DownloadRequest request) {
        return new DownloadState(Status.ERROR, request);
    }

    boolean isInProgress() {
        return status == Status.PROGRESS;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isCancelled() {
        return status == Status.CANCELLED;
    }

    boolean isConnectivityError() {
        return status == Status.CONNECTIVITY_ERROR;
    }

    public boolean isUnavailable() {
        return status == Status.UNAVAILABLE;
    }

    boolean isNotEnoughSpace() {
        return status == Status.NOT_ENOUGH_SPACE;
    }

    boolean isNotEnoughMinimumSpace() {
        return status == Status.NOT_ENOUGH_MINIMUM_SPACE;
    }

    boolean isInaccessibleStorage() {
        return status == Status.INACCESSIBLE_STORAGE;
    }

    boolean isDownloadFailed() {
        return status == Status.ERROR;
    }

    public Urn getTrack() {
        return request.getUrn();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getProgress() {
        return progress;
    }

    long getTotalBytes() {
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
