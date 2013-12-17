package com.soundcloud.android.playback.service;

import com.google.common.base.Objects;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.ScTextUtils;

public class TrackSourceInfo {

    private String mOriginScreen;
    private boolean mUserTriggered;

    private String mSource;
    private String mSourceVersion;

    private long mPlaylistId = Playable.NOT_SET;
    private long mPlaylistOwnerId = User.NOT_SET;
    private int mPlaylistPosition;

    public TrackSourceInfo(String originScreen, boolean userTriggered) {
        mOriginScreen = originScreen;
        mUserTriggered = userTriggered;
    }

    public void setSource(String source, String sourceVersion) {
        mSource = source;
        mSourceVersion = sourceVersion;
    }

    public void setOriginPlaylist(long playlistId, int position, long playlistOwnerId) {
        mPlaylistId = playlistId;
        mPlaylistPosition = position;
        mPlaylistOwnerId = playlistOwnerId;
    }

    public boolean getIsUserTriggered() {
        return mUserTriggered;
    }

    public String getOriginScreen() {
        return mOriginScreen;
    }

    public String getSource() {
        return mSource;
    }

    public String getSourceVersion() {
        return mSourceVersion;
    }

    public long getPlaylistId() {
        return mPlaylistId;
    }

    public int getPlaylistPosition() {
        return mPlaylistPosition;
    }

    public long getPlaylistOwnerId() {
        return mPlaylistOwnerId;
    }

    public boolean hasSource(){
        return ScTextUtils.isNotBlank(mSource);
    }

    public boolean isFromPlaylist(){
        return mPlaylistId > 0;
    }

    @Override
    public String toString() {
        final Objects.ToStringHelper toStringHelper = Objects.toStringHelper(TrackSourceInfo.class)
                .add("originScreen", mOriginScreen)
                .add("userTriggered", mUserTriggered);

        if (hasSource()) toStringHelper.add("source", mSource).add("sourceVersion", mSourceVersion);
        if (isFromPlaylist()) toStringHelper.add("playlistId", mPlaylistId).add("playlistPos", mPlaylistPosition);

        return toStringHelper.toString();
    }
}
