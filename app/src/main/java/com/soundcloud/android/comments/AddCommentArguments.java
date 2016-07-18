package com.soundcloud.android.comments;

import auto.parcel.AutoParcel;
import com.soundcloud.java.collections.PropertySet;

@AutoParcel
public abstract class AddCommentArguments {

    public static AddCommentArguments create(PropertySet track, long position, String commentText, String originScreen) {
        return new AutoParcel_AddCommentArguments(track, position, commentText, originScreen);
    }

    public abstract PropertySet getTrack();

    public abstract long getPosition();

    public abstract String getCommentText();

    public abstract String getOriginScreen();

}
