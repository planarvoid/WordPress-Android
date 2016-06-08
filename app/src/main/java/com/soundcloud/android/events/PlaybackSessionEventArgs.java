package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.java.collections.PropertySet;
import org.jetbrains.annotations.NotNull;

@AutoValue
public abstract class PlaybackSessionEventArgs {

    public static PlaybackSessionEventArgs create(@NotNull PropertySet trackData,
                                    @NotNull Urn userUrn,
                                    @NotNull TrackSourceInfo trackSourceInfo,
                                    long progress,
                                    @NotNull String protocol,
                                    @NotNull String playerType,
                                    @NotNull String connectionType,
                                    boolean isOfflineTrack,
                                    boolean marketablePlay,
                                    @NotNull String uuid,
                                    @NotNull DateProvider dateProvider) {
        return new AutoValue_PlaybackSessionEventArgs(trackData, userUrn, trackSourceInfo, progress,
                protocol, playerType, connectionType, isOfflineTrack,
                marketablePlay, uuid, dateProvider);
    }

    @NotNull
    public abstract PropertySet getTrackData();

    @NotNull
    public abstract Urn getUserUrn();

    @NotNull
    public abstract TrackSourceInfo getTrackSourceInfo();

    public abstract long getProgress();

    @NotNull
    public abstract String getProtocol();

    @NotNull
    public abstract String getPlayerType();

    @NotNull
    public abstract String getConnectionType();

    public abstract boolean isOfflineTrack();

    public abstract boolean isMarketablePlay();

    @NotNull
    public abstract String getUuid();

    @NotNull
    public abstract DateProvider getDateProvider();
}
