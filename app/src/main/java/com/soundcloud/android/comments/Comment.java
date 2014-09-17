package com.soundcloud.android.comments;

import com.soundcloud.android.api.legacy.model.PublicApiComment;
import com.soundcloud.android.users.UserUrn;

import java.util.Date;

public class Comment {

    private final PublicApiComment apiComment;

    Comment(PublicApiComment apiComment) {
        this.apiComment = apiComment;
    }

    UserUrn getUserUrn() {
        return apiComment.getUser().getUrn();
    }

    String getUsername() {
        return apiComment.getUser().getUsername();
    }

    long getTimeStamp() {
        return apiComment.timestamp;
    }

    String getText() {
        return apiComment.body;
    }

    Date getDate() {
        return apiComment.getCreatedAt();
    }
}
