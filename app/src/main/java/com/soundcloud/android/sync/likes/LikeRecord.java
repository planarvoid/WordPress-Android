package com.soundcloud.android.sync.likes;

import com.soundcloud.android.model.Urn;

import java.util.Date;

public interface LikeRecord {
    Urn getTargetUrn();

    Date getCreatedAt();
}
