package com.soundcloud.android.sync.posts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.PropertySet;

import java.util.Date;

public class ApiPost implements PropertySetSource {

    private final Urn targetUrn;
    private final Date createdAt;
    private final boolean isRepost;

    public ApiPost(@JsonProperty("target_urn") Urn targetUrn,
                   @JsonProperty("created_at") Date createdAt,
                   @JsonProperty("is_repost") boolean isRepost) {
        this.targetUrn = targetUrn;
        this.createdAt = createdAt;
        this.isRepost = isRepost;
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
                PostProperty.TARGET_URN.bind(targetUrn),
                PostProperty.CREATED_AT.bind(createdAt),
                PostProperty.IS_REPOST.bind(isRepost)
        );
    }

    public boolean isRepost() {
        return isRepost;
    }
}
