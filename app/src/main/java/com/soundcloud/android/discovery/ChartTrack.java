package com.soundcloud.android.discovery;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

class ChartTrack implements ImageResource {
    private final Urn urn;
    private final Optional<String> imageUrlTemplate;

    private ChartTrack(Urn urn, Optional<String> imageUrlTemplate) {
        this.urn = urn;
        this.imageUrlTemplate = imageUrlTemplate;
    }

    static ChartTrack fromPropertySet(final PropertySet propertyBindings) {
        return new ChartTrack(propertyBindings.get(EntityProperty.URN),
                              propertyBindings.get(EntityProperty.IMAGE_URL_TEMPLATE));
    }

    @Override
    public Urn getUrn() {
        return urn;
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return imageUrlTemplate;
    }
}