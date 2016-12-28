package com.soundcloud.android.comments;

import auto.parcel.AutoParcel;
import com.soundcloud.android.tracks.TrackItem;

@AutoParcel
public abstract class AddCommentArguments {

    public static AddCommentArguments create(TrackItem track, long position, String commentText, String originScreen) {
        return new AutoParcel_AddCommentArguments(track, position, commentText, originScreen);
    }

    public abstract TrackItem getTrack();

    public abstract long getPosition();

    public abstract String getCommentText();

    public abstract String getOriginScreen();

}
