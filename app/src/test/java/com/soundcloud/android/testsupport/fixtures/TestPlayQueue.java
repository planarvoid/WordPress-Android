package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.java.collections.PropertySet;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TestPlayQueue {

    public static PlayQueue fromTracks(PlaySessionSource playSessionSource, Map<Urn, Boolean> blockedTracksMap, PropertySet... tracks) {
        return PlayQueue.fromPlayableList(Arrays.asList(tracks), playSessionSource, blockedTracksMap);
    }

    public static PlayQueue fromTracks(PlaySessionSource playSessionSource, PropertySet... tracks) {
        return PlayQueue.fromPlayableList(Arrays.asList(tracks), playSessionSource, Collections.<Urn, Boolean>emptyMap());
    }

    public static PlayQueue fromUrns(PlaySessionSource playSessionSource, Map<Urn, Boolean> blockedTracksMap, Urn... trackUrns) {
        return PlayQueue.fromTrackUrnList(Arrays.asList(trackUrns), playSessionSource, blockedTracksMap);
    }

    public static PlayQueue fromUrns(PlaySessionSource playSessionSource, Urn... trackUrns) {
        return fromUrns(Arrays.asList(trackUrns), playSessionSource);
    }

    public static PlayQueue fromUrns(List trackUrns, PlaySessionSource playSessionSource) {
        return PlayQueue.fromTrackUrnList(trackUrns, playSessionSource, Collections.<Urn, Boolean>emptyMap());
    }

    public static PlayQueue fromTracks(List<PropertySet> tracks, PlaySessionSource playSessionSource) {
        return PlayQueue.fromPlayableList(tracks, playSessionSource, Collections.<Urn, Boolean>emptyMap());
    }
}
