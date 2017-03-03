package com.soundcloud.android.comments;

import auto.parcel.AutoParcel;
import com.soundcloud.android.model.Urn;

@AutoParcel
public abstract class AddCommentArguments {

    public static AddCommentArguments create(String trackTitle, Urn trackUrn, String creatorName, Urn creatorUrn, long position, String commentText, String originScreen) {
        return new AutoParcel_AddCommentArguments(trackTitle, trackUrn, creatorName, creatorUrn, position, commentText, originScreen);
    }

    public abstract String trackTitle();

    public abstract Urn trackUrn();

    public abstract String creatorName();

    public abstract Urn creatorUrn();

    public abstract long getPosition();

    public abstract String getCommentText();

    public abstract String getOriginScreen();

}
