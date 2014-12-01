package com.soundcloud.android.offline;

import com.soundcloud.android.model.Urn;

final class DownloadRequest {
    public final String fileUrl;
    public final Urn urn;

    public DownloadRequest(Urn urn, String url) {
        this.fileUrl = url;
        this.urn = urn;
    }
}
