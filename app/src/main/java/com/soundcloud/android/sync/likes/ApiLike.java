package com.soundcloud.android.sync.likes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;

import java.util.Date;

public final class ApiLike implements PropertySetSource {

    private final Urn targetUrn;
    private final Date createdAt;

    public ApiLike(@JsonProperty("target_urn") Urn targetUrn, @JsonProperty("created_at") Date createdAt) {
        this.targetUrn = targetUrn;
        this.createdAt = createdAt;
    }

    public Urn getTargetUrn() {
        return targetUrn;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    @Override
    public PropertySet toPropertySet() {
        return PropertySet.from(
                LikeProperty.TARGET_URN.bind(targetUrn),
                LikeProperty.CREATED_AT.bind(createdAt)
        );
    }
}
