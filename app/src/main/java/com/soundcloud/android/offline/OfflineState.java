package com.soundcloud.android.offline;

public enum OfflineState {
    NOT_OFFLINE, REQUESTED, DOWNLOADING, DOWNLOADED, UNAVAILABLE;

    public static OfflineState getOfflineState(boolean hasRequestedTracks,
                                               boolean hasDownloadedTracks,
                                               boolean hasUnavailableTracks) {
        if (hasRequestedTracks) {
            return REQUESTED;
        } else if (hasDownloadedTracks) {
            return DOWNLOADED;
        } else if (hasUnavailableTracks) {
            return UNAVAILABLE;
        } else {
            return REQUESTED;
        }
    }
}
