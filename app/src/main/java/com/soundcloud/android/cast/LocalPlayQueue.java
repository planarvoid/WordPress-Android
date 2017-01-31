package com.soundcloud.android.cast;

import com.google.android.gms.cast.MediaInfo;
import com.soundcloud.android.model.Urn;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

@Deprecated
class LocalPlayQueue {

    final JSONObject playQueueTracksJSON;
    final List<Urn> playQueueTrackUrns;
    final MediaInfo mediaInfo;
    final Urn currentTrackUrn;

    LocalPlayQueue(JSONObject playQueueTracksJSON,
                   List<Urn> playQueueTrackUrns,
                   MediaInfo mediaInfo,
                   Urn currentTrackUrn) {
        this.playQueueTracksJSON = playQueueTracksJSON;
        this.playQueueTrackUrns = Collections.unmodifiableList(playQueueTrackUrns);
        this.mediaInfo = mediaInfo;
        this.currentTrackUrn = currentTrackUrn;
    }

    public static LocalPlayQueue empty() {
        return new LocalPlayQueue(null, Collections.emptyList(), null, Urn.NOT_SET);
    }

    public boolean isEmpty() {
        return playQueueTrackUrns.isEmpty();
    }

}
