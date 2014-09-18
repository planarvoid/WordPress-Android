package com.soundcloud.android.playback.service;

import com.google.common.base.Objects;
import com.soundcloud.android.Consts;
import com.soundcloud.android.utils.ScTextUtils;

public class TrackSourceInfo {

    private final String originScreen;
    private final boolean userTriggered;

    private String source;
    private String sourceVersion;

    private long playlistId = Consts.NOT_SET;
    private long playlistOwnerId = Consts.NOT_SET;
    private int playlistPosition;

    public TrackSourceInfo(String originScreen, boolean userTriggered) {
        this.originScreen = originScreen;
        this.userTriggered = userTriggered;
    }

    public void setSource(String source, String sourceVersion) {
        this.source = source;
        this.sourceVersion = sourceVersion;
    }

    @Deprecated // use URNs
    public void setOriginPlaylist(long playlistId, int position, long playlistOwnerId) {
        this.playlistId = playlistId;
        this.playlistPosition = position;
        this.playlistOwnerId = playlistOwnerId;
    }

    public boolean getIsUserTriggered() {
        return userTriggered;
    }

    public String getOriginScreen() {
        return originScreen;
    }

    public String getSource() {
        return source;
    }

    public String getSourceVersion() {
        return sourceVersion;
    }

    @Deprecated // use URNs
    public long getPlaylistId() {
        return playlistId;
    }

    public int getPlaylistPosition() {
        return playlistPosition;
    }

    @Deprecated // use URNs
    public long getPlaylistOwnerId() {
        return playlistOwnerId;
    }

    public boolean hasSource() {
        return ScTextUtils.isNotBlank(source);
    }

    public boolean isFromPlaylist() {
        return playlistId > 0;
    }

    @Override
    public String toString() {
        final Objects.ToStringHelper toStringHelper = Objects.toStringHelper(TrackSourceInfo.class)
                .add("originScreen", originScreen)
                .add("userTriggered", userTriggered);

        if (hasSource()) {
            toStringHelper.add("source", source).add("sourceVersion", sourceVersion);
        }
        if (isFromPlaylist()) {
            toStringHelper.add("playlistId", playlistId)
                    .add("playlistPos", playlistPosition)
                    .add("playlistOwnerId", playlistOwnerId);
        }

        return toStringHelper.toString();
    }
}
