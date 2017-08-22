package com.soundcloud.android.comments;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UserHolder;
import com.soundcloud.java.optional.Optional;

import java.util.Date;

public interface CommentRecord extends UserHolder {

    Urn getUrn();

    Urn getTrackUrn();

    String getBody();

    Optional<Long> getTrackTime();

    Date getCreatedAt();

}
