package com.soundcloud.android.cast;

import com.google.android.gms.cast.MediaInfo;
import com.soundcloud.android.model.Urn;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

class LocalPlayQueue {

    public final JSONObject playQueueTracksJSON;
    public final List<Urn> playQueueTrackUrns;
    public final MediaInfo mediaInfo;
    public final Urn currentTrackUrn;

    public LocalPlayQueue(JSONObject playQueueTracksJSON, List<Urn> playQueueTrackUrns, MediaInfo mediaInfo, Urn currentTrackUrn) {
        this.playQueueTracksJSON = playQueueTracksJSON;
        this.playQueueTrackUrns = Collections.unmodifiableList(playQueueTrackUrns);
        this.mediaInfo = mediaInfo;
        this.currentTrackUrn = currentTrackUrn;
    }

}
