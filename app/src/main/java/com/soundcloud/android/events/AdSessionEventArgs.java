package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.TrackSourceInfo;

import java.util.UUID;

@AutoValue
public abstract class AdSessionEventArgs {

    public static AdSessionEventArgs create(TrackSourceInfo trackSourceInfo,
                                            PlaybackStateTransition transition,
                                            String uuid) {
        return new AutoValue_AdSessionEventArgs(
                trackSourceInfo,
                transition.getProgress().getPosition(),
                transition.getProgress().getDuration(),
                getProtocol(transition),
                getPlayerType(transition),
                uuid);
    }

    public static AdSessionEventArgs createWithProgress(TrackSourceInfo trackSourceInfo,
                                                        PlaybackProgress playbackProgress,
                                                        PlaybackStateTransition transition) {
        return new AutoValue_AdSessionEventArgs(
                trackSourceInfo,
                playbackProgress.getPosition(),
                playbackProgress.getDuration(),
                getProtocol(transition),
                getPlayerType(transition),
                UUID.randomUUID().toString());
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
