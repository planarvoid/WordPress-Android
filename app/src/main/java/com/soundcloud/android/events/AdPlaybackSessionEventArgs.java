package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.TrackSourceInfo;

@AutoValue
public abstract class AdPlaybackSessionEventArgs {

    public static AdPlaybackSessionEventArgs create(TrackSourceInfo trackSourceInfo,
                                                    PlaybackStateTransition transition,
                                                    String uuid) {
        return new AutoValue_AdPlaybackSessionEventArgs(
                trackSourceInfo,
                transition.getProgress().getPosition(),
                transition.getProgress().getDuration(),
                getProtocol(transition),
                getPlayerType(transition),
                uuid);
    }

    public static AdPlaybackSessionEventArgs createWithProgress(TrackSourceInfo trackSourceInfo,
                                                                PlaybackProgress playbackProgress,
                                                                PlaybackStateTransition transition,
                                                                String uuid) {
        return new AutoValue_AdPlaybackSessionEventArgs(
                trackSourceInfo,
                playbackProgress.getPosition(),
                playbackProgress.getDuration(),
                getProtocol(transition),
                getPlayerType(transition),
                uuid);
    }

    public abstract TrackSourceInfo getTrackSourceInfo();

    public abstract long getProgress();

    public abstract long getDuration();

    public abstract String getProtocol();

    public abstract String getPlayerType();

    public abstract String getUuid();

    private static String getPlayerType(PlaybackStateTransition stateTransition) {
        return stateTransition.getExtraAttribute(PlaybackStateTransition.EXTRA_PLAYER_TYPE);
    }

    private static String getProtocol(PlaybackStateTransition stateTransition) {
        return stateTransition.getExtraAttribute(PlaybackStateTransition.EXTRA_PLAYBACK_PROTOCOL);
    }
}
