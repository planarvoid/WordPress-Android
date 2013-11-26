package com.soundcloud.android.playback;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.tracking.eventlogger.TrackSourceInfo;

import android.net.Uri;

import java.util.List;

public class PlayQueueInitializer {

    public int startPosition;
    public Uri uri;
    public List<Track> initialTracklist;
    public boolean fetchRelated;

    private PlaySessionSource playSessionSource;
    private TrackSourceInfo trackSourceInfo;

    public PlayQueueInitializer(PlaySessionSource playSessionSource) {
        this.playSessionSource = playSessionSource;
    }

    public static PlayQueueInitializer fromTrack(Track track, PlaySessionSource playSessionSource){
        PlayQueueInitializer playQueueInitializer = new PlayQueueInitializer(playSessionSource);
        playQueueInitializer. initialTracklist = Lists.newArrayList(track);
        return playQueueInitializer;
    }

    public static PlayQueueInitializer fromExplore(Track track, PlaySessionSource playSessionSource, TrackSourceInfo trackSourceInfo) {
        PlayQueueInitializer playQueueInitializer = fromTrack(track, playSessionSource);
        playQueueInitializer.fetchRelated = true;
        playQueueInitializer.trackSourceInfo = trackSourceInfo;
        return playQueueInitializer;
    }

    public static PlayQueueInitializer fromUri(Uri uri, int startPosition, PlaySessionSource playSessionSource) {
        PlayQueueInitializer playQueueInitializer = new PlayQueueInitializer(playSessionSource);
        playQueueInitializer.uri = uri;
        playQueueInitializer.startPosition = startPosition;
        return playQueueInitializer;
    }

    public boolean isStoredCollection() {
        return uri != null;
    }
}
