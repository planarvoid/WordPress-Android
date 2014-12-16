package com.soundcloud.android.offline;

import com.google.common.base.Objects;
import com.soundcloud.android.model.Urn;

final class DownloadRequest {
    public final String fileUrl;
    public final Urn urn;

    public DownloadRequest(Urn urn, String url) {
        this.fileUrl = url;
        this.urn = urn;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("urn", urn).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final DownloadRequest request = (DownloadRequest) o;
        return Objects.equal(fileUrl, request.fileUrl) && Objects.equal(urn, request.urn);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fileUrl, urn);
    }
}
