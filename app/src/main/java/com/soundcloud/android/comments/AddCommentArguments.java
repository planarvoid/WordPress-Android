package com.soundcloud.android.comments;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

import android.os.Parcelable;

@AutoValue
public abstract class AddCommentArguments implements Parcelable {

    public static AddCommentArguments create(String trackTitle, Urn trackUrn, String creatorName, Urn creatorUrn, long position, String commentText, String originScreen) {
        return new AutoValue_AddCommentArguments(trackTitle, trackUrn.getContent(), creatorName, creatorUrn.getContent(), position, commentText, originScreen);
    }

    public abstract String trackTitle();

    public Urn trackUrn() {
        return new Urn(stringTrackUrn());
    }

    abstract String stringTrackUrn();

    public abstract String creatorName();

    public Urn creatorUrn() {
        return new Urn(stringCreatorUrn());
    }

    abstract String stringCreatorUrn();

    public abstract long getPosition();

    public abstract String getCommentText();

    public abstract String getOriginScreen();

}
