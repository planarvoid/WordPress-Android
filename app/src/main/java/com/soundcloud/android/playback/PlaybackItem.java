package com.soundcloud.android.playback;

import com.soundcloud.android.model.Urn;

import android.os.Parcelable;

public interface PlaybackItem extends Parcelable {

    Urn getUrn();

    long getStartPosition();

    PlaybackType getPlaybackType();

    long getDuration();

}
