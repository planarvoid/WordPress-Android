package com.soundcloud.android.api.legacy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.soundcloud.android.api.legacy.model.behavior.Identifiable;

import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        defaultImpl = UnknownResource.class,
        property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PublicApiTrack.class, name = "track"),
        @JsonSubTypes.Type(value = PublicApiComment.class, name = "comment"),
        @JsonSubTypes.Type(value = PublicApiUser.class, name = "user"),
        @JsonSubTypes.Type(value = PublicApiPlaylist.class, name = "playlist")})
public abstract class PublicApiResource
        extends ScModel
        implements Identifiable {

    @JsonIgnore
    public long last_updated = NOT_SET;

    public PublicApiResource() {
    }

    public PublicApiResource(long id) {
        super(id);
    }

    public PublicApiResource(String urn) {
        super(urn);
    }

    public void setUpdated() {
        last_updated = System.currentTimeMillis();
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
