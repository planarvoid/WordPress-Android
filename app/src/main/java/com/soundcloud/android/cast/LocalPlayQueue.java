package com.soundcloud.android.cast;

import com.google.android.gms.cast.MediaInfo;
import com.soundcloud.android.model.Urn;
import org.json.JSONObject;

class LocalPlayQueue {

    public final JSONObject playQueueTracks;
    public final MediaInfo mediaInfo;
    public final Urn currentTrackUrn;

    public LocalPlayQueue(JSONObject playQueueTracks, MediaInfo mediaInfo, Urn currentTrackUrn) {
        this.playQueueTracks = playQueueTracks;
        this.mediaInfo = mediaInfo;
        this.currentTrackUrn = currentTrackUrn;
    }

}
