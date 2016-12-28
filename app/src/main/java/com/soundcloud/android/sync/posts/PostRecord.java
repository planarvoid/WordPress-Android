package com.soundcloud.android.sync.posts;

import com.soundcloud.android.model.Urn;

import java.util.Date;

public interface PostRecord {
    Urn getTargetUrn();

    Date getCreatedAt();

    boolean isRepost();
}

