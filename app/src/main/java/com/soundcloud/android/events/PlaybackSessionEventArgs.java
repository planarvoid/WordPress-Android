package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.java.collections.PropertySet;

import android.webkit.URLUtil;

@AutoValue
public abstract class PlaybackSessionEventArgs {

    public static PlaybackSessionEventArgs create(PropertySet trackData,
                                                  Urn userUrn,
                                                  TrackSourceInfo trackSourceInfo,
                                                  long progress,
                                                  String protocol,
                                                  String playerType,
                                                  String connectionType,
                                                  boolean isOfflineTrack,
                                                  boolean marketablePlay,
                                                  String uuid,
                                                  DateProvider dateProvider) {
        return new AutoValue_PlaybackSessionEventArgs(trackData, userUrn, trackSourceInfo, progress,
                protocol, playerType, connectionType, isOfflineTrack,
                marketablePlay, uuid, dateProvider);
    }

    public static PlaybackSessionEventArgs create(PropertySet trackData,
                                                  Urn userUrn,
                                                  TrackSourceInfo trackSourceInfo,
                                                  PlaybackStateTransition transition,
                                                  boolean marketablePlay,
                                                  String uuid,
                                                  DateProvider dateProvider) {
        return PlaybackSessionEventArgs.create(trackData, userUrn, trackSourceInfo, transition.getProgress().getPosition(),
                getProtocol(transition), getPlayerType(transition), getConnectionType(transition),
                isLocalStoragePlayback(transition), marketablePlay, uuid, dateProvider);
    }

    public static PlaybackSessionEventArgs createWithProgress(PropertySet trackData,
                                                  Urn userUrn,
                                                  TrackSourceInfo trackSourceInfo,
                                                  PlaybackProgress progress,
                                                  PlaybackStateTransition transition,
                                                  boolean marketablePlay,
                                                  String uuid,
                                                  DateProvider dateProvider) {
        return PlaybackSessionEventArgs.create(trackData, userUrn, trackSourceInfo, progress.getPosition(),
                getProtocol(transition), getPlayerType(transition), getConnectionType(transition),
                isLocalStoragePlayback(transition), marketablePlay, uuid, dateProvider);
    }

    public abstract PropertySet getTrackData();

    public abstract Urn getUserUrn();

    public abstract TrackSourceInfo getTrackSourceInfo();

    public abstract long getProgress();

    public abstract String getProtocol();

    public abstract String getPlayerType();

    public abstract String getConnectionType();

    public abstract boolean isOfflineTrack();

    public abstract boolean isMarketablePlay();

    public abstract String getUuid();

    public abstract DateProvider getDateProvider();

    private static String getPlayerType(PlaybackStateTransition stateTransition) {
        return stateTransition.getExtraAttribute(PlaybackStateTransition.EXTRA_PLAYER_TYPE);
    }

    private static String getConnectionType(PlaybackStateTransition stateTransition) {
        return stateTransition.getExtraAttribute(PlaybackStateTransition.EXTRA_CONNECTION_TYPE);
    }

    private static String getProtocol(PlaybackStateTransition stateTransition) {
        return stateTransition.getExtraAttribute(PlaybackStateTransition.EXTRA_PLAYBACK_PROTOCOL);
    }

    private static boolean isLocalStoragePlayback(PlaybackStateTransition stateTransition) {
        return URLUtil.isFileUrl(stateTransition.getExtraAttribute(PlaybackStateTransition.EXTRA_URI));
    }

}
