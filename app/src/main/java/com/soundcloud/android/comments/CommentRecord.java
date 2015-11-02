package com.soundcloud.android.comments;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UserHolder;

import java.util.Date;

public interface CommentRecord extends UserHolder {

    Urn getUrn();

    Urn getTrackUrn();

    String getBody();

    long getTrackTime();

    Date getCreatedAt();

}
