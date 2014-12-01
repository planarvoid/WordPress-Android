package com.soundcloud.android.offline;

import com.soundcloud.android.model.Urn;

public final class DownloadResult {

    private final Urn urn;
    private final boolean isSuccessful;
    private final long downloadedAt;

    private DownloadResult(boolean success, Urn urn) {
        this.urn = urn;
        this.isSuccessful = success;
        this.downloadedAt = System.currentTimeMillis();
    }

    public static DownloadResult forSuccess(Urn trackUrn) {
        return new DownloadResult(true, trackUrn);
    }

    public static DownloadResult forFailure(Urn trackUrn) {
        return new DownloadResult(false, trackUrn);
    }

    public long getDownloadedAt() {
        return downloadedAt;
    }

    public Urn getUrn() {
        return urn;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }
}
