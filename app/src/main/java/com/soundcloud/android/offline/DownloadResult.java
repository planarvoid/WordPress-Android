package com.soundcloud.android.offline;

import com.google.common.base.Objects;
import com.soundcloud.android.model.Urn;

public final class DownloadResult {

    private final Urn urn;
    private final boolean isSuccessful;
    private final long downloadedAt;

    public DownloadResult(boolean success, Urn urn) {
        this.urn = urn;
        this.isSuccessful = success;
        this.downloadedAt = System.currentTimeMillis();
    }

    public long getDownloadedAt() {
        return downloadedAt;
    }

    public Urn getUrn() {
        return urn;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("urn", urn)
                .add("isSuccessful", isSuccessful)
                .add("downloadedAt", downloadedAt).toString();
    }
}
