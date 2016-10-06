package com.soundcloud.android.api.legacy.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.soundcloud.android.api.legacy.model.behavior.Identifiable;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.Nullable;

import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        defaultImpl = UnknownResource.class,
        property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PublicApiTrack.class, name = "track"),
        @JsonSubTypes.Type(value = PublicApiComment.class, name = "comment"),
        @JsonSubTypes.Type(value = PublicApiUser.class, name = "user")})
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

    public static class ResourceHolder<T extends PublicApiResource> extends CollectionHolder<T> {

        public ResourceHolder() {
        }

        public ResourceHolder(List<T> collection) {
            super(collection);
        }

        public ResourceHolder(List<T> collection, String nextHref) {
            super(collection, nextHref);
        }
    }
}
