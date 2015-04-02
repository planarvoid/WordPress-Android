package com.soundcloud.android.offline;

import com.google.common.base.Objects;
import com.soundcloud.android.model.Urn;

public final class DownloadResult {
    private enum Status {SUCCESS, UNAVAILABLE, FAILURE}

    private final Status status;
    private final DownloadRequest request;
    private final long timestamp;

    private DownloadResult(Status status, DownloadRequest request) {
        this.status = status;
        this.request = request;
        this.timestamp = System.currentTimeMillis();
    }

    public static DownloadResult failed(DownloadRequest request) {
        return new DownloadResult(Status.FAILURE, request);
    }

    public static DownloadResult unavailable(DownloadRequest request) {
        return new DownloadResult(Status.UNAVAILABLE, request);
    }

    public static DownloadResult success(DownloadRequest request) {
        return new DownloadResult(Status.SUCCESS, request);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isFailure() {
        return status == Status.FAILURE;
    }

    public boolean isUnavailable() {
        return status == Status.UNAVAILABLE;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public DownloadRequest getRequest() {
        return request;
    }

    public Urn getTrack() {
        return request.track;
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
