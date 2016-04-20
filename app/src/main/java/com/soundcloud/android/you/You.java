package com.soundcloud.android.you;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

class You implements ImageResource {

    private final PropertySet source;

    You(PropertySet source) {
        this.source = source;
    }

    String getUsername() {
        return source.get(UserProperty.USERNAME);
    }

    @Override
    public Urn getUrn() {
        return source.get(EntityProperty.URN);
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return source.get(EntityProperty.IMAGE_URL_TEMPLATE);
    }
}
