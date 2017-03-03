package com.soundcloud.android.api.legacy.model;

import com.soundcloud.android.api.legacy.model.behavior.Identifiable;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.Nullable;

public abstract class PublicApiResource
        extends ScModel
        implements Identifiable {

    public PublicApiResource() {
    }

    public PublicApiResource(long id) {
        super(id);
    }

    public PublicApiResource(String urn) {
        super(urn);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PublicApiResource)) {
            return false;
        }

        PublicApiResource resourceBase = (PublicApiResource) o;
        return getId() == resourceBase.getId();
    }

    @Override
    public int hashCode() {
        return (int) (getId() ^ (getId() >>> 32));
    }

    /**
     * @return whether this object has been saved to the database.
     */
    public boolean isSaved() {
        return getId() > NOT_SET;
    }

    // terrible hack that makes public API resources support artwork templates
    protected Optional<String> imageUrlToTemplate(@Nullable String imageUrl) {
        if (imageUrl != null) {
            return Optional.of(imageUrl.replaceAll("-large", "-{size}"));
        }
        return Optional.absent();
    }
}
