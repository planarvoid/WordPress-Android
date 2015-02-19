package com.soundcloud.android.offline;

import com.google.common.base.Objects;
import com.soundcloud.android.model.Urn;

public final class DownloadResult {

    private enum Status {SUCCESS, UNAVAILABLE, FAILURE}

    private final Status status;
    private final Urn urn;
    private final long timestamp;

    private DownloadResult(Status status, Urn urn) {
        this.status = status;
        this.urn = urn;
        this.timestamp = System.currentTimeMillis();
    }

    public static DownloadResult failed(Urn urn) {
        return new DownloadResult(Status.FAILURE, urn);
    }

    public static DownloadResult unavailable(Urn urn) {
        return new DownloadResult(Status.UNAVAILABLE, urn);
    }

    public static DownloadResult success(Urn urn) {
        return new DownloadResult(Status.SUCCESS, urn);
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

    public Urn getUrn() {
        return urn;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("urn", urn)
                .add("timestamp", timestamp).toString();
    }
}
