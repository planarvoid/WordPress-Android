package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.java.collections.PropertySet;

import android.webkit.URLUtil;

@AutoValue
public abstract class PlaybackSessionEventArgs {

    public static PlaybackSessionEventArgs create(PropertySet trackData,
                                                  TrackSourceInfo trackSourceInfo,
                                                  long progress,
                                                  String protocol,
                                                  String playerType,
                                                  boolean isOfflineTrack,
                                                  boolean marketablePlay,
                                                  String uuid) {
        return new AutoValue_PlaybackSessionEventArgs(trackData, trackSourceInfo, progress,
                                                      protocol, playerType, isOfflineTrack, marketablePlay, uuid);
    }

    public static PlaybackSessionEventArgs create(PropertySet trackData,
                                                  TrackSourceInfo trackSourceInfo,
                                                  PlaybackStateTransition transition,
                                                  boolean marketablePlay,
                                                  String uuid) {
        return PlaybackSessionEventArgs.create(trackData, trackSourceInfo, transition.getProgress().getPosition(),
                                               getProtocol(transition), getPlayerType(transition),
                                               isLocalStoragePlayback(transition), marketablePlay, uuid);
    }

    public static PlaybackSessionEventArgs createWithProgress(PropertySet trackData,
                                                              TrackSourceInfo trackSourceInfo,
                                                              PlaybackProgress progress,
                                                              PlaybackStateTransition transition,
                                                              boolean marketablePlay,
                                                              String uuid) {
        return PlaybackSessionEventArgs.create(trackData, trackSourceInfo, progress.getPosition(),
                                               getProtocol(transition), getPlayerType(transition),
                                               isLocalStoragePlayback(transition), marketablePlay, uuid);
    }

    public abstract PropertySet getTrackData();

    public abstract TrackSourceInfo getTrackSourceInfo();

    public abstract long getProgress();

    public abstract String getProtocol();

    public abstract String getPlayerType();

    public abstract boolean isOfflineTrack();

    public abstract boolean isMarketablePlay();

    public abstract String getUuid();

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
