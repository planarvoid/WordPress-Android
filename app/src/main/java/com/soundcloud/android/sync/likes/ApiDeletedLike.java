package com.soundcloud.android.sync.likes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;

final class ApiDeletedLike implements PropertySetSource {

    private final Urn targetUrn;

    public ApiDeletedLike(@JsonProperty("target_urn") Urn targetUrn) {
        this.targetUrn = targetUrn;
    }

    public Urn getTargetUrn() {
        return targetUrn;
    }

    @Override
    public PropertySet toPropertySet() {
        return PropertySet.from(LikeProperty.TARGET_URN.bind(targetUrn));
    }
}
