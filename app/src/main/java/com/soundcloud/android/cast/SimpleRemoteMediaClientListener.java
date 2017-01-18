package com.soundcloud.android.cast;

import com.google.android.gms.cast.framework.media.RemoteMediaClient;

public class SimpleRemoteMediaClientListener implements RemoteMediaClient.Listener {
    @Override
    public void onStatusUpdated() {
        // no-op as default
    }

    @Override
    public void onMetadataUpdated() {
        // no-op as default
    }

    @Override
    public void onQueueStatusUpdated() {
        // no-op as default
    }

    @Override
    public void onPreloadStatusUpdated() {
        // no-op as default
    }

    @Override
    public void onSendingRemoteMediaRequest() {
        // no-op as default
    }

    @Override
    public void onAdBreakStatusUpdated() {
        // no-op as default
    }
}
