package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.tracks.TrackItem;

import android.webkit.URLUtil;

@AutoValue
public abstract class PlaybackSessionEventArgs {

    public static PlaybackSessionEventArgs create(TrackItem trackData,
                                                  TrackSourceInfo trackSourceInfo,
                                                  long progress,
                                                  String protocol,
                                                  String playerType,
                                                  boolean isOfflineTrack,
                                                  boolean marketablePlay,
                                                  String clientId,
                                                  String playId) {
        return new AutoValue_PlaybackSessionEventArgs(trackData, trackSourceInfo, progress,
                                                      protocol, playerType, isOfflineTrack,
                                                      marketablePlay, clientId, playId);
    }

    public static PlaybackSessionEventArgs createWithProgress(TrackItem trackData,
                                                              TrackSourceInfo trackSourceInfo,
                                                              PlaybackProgress progress,
                                                              PlaybackStateTransition transition,
                                                              boolean marketablePlay,
                                                              String clientId,
                                                              String playId) {
        return PlaybackSessionEventArgs.create(trackData, trackSourceInfo, progress.getPosition(),
                                               getProtocol(transition), getPlayerType(transition),
                                               isLocalStoragePlayback(transition), marketablePlay, clientId, playId);
    }

    public abstract TrackItem getTrackData();

    public abstract TrackSourceInfo getTrackSourceInfo();

    public abstract long getProgress();

    public abstract String getProtocol();

    public abstract String getPlayerType();

    public abstract boolean isOfflineTrack();

    public abstract boolean isMarketablePlay();

    public abstract String getClientEventId();

    public abstract String getPlayId();

    private static String getPlayerType(PlaybackStateTransition stateTransition) {
        return stateTransition.getExtraAttribute(PlaybackStateTransition.EXTRA_PLAYER_TYPE);
    }

    private static String getProtocol(PlaybackStateTransition stateTransition) {
        return stateTransition.getExtraAttribute(PlaybackStateTransition.EXTRA_PLAYBACK_PROTOCOL);
    }

    private static boolean isLocalStoragePlayback(PlaybackStateTransition stateTransition) {
        return URLUtil.isFileUrl(stateTransition.getExtraAttribute(PlaybackStateTransition.EXTRA_URI));
    }

}
