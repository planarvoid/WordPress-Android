package com.soundcloud.android.comments;

import com.soundcloud.android.api.legacy.model.PublicApiComment;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.java.collections.PropertySet;

import java.util.Date;

public class Comment implements ListItem {

    private final PublicApiComment apiComment;

    Comment(PublicApiComment apiComment) {
        this.apiComment = apiComment;
    }

    @Override
    public Comment update(PropertySet sourceSet) {
        // NOP
        return this;
    }

    @Override
    public Urn getEntityUrn() {
        return apiComment.getUrn();
    }

    Urn getUserUrn() {
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
