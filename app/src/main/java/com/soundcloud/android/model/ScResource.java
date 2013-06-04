package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.soundcloud.android.model.behavior.Identifiable;
import com.soundcloud.android.model.behavior.Persisted;
import com.soundcloud.android.provider.BulkInsertMap;
import org.jetbrains.annotations.NotNull;

import android.content.Intent;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        defaultImpl = UnknownResource.class,
        property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Track.class, name = "track"),
        @JsonSubTypes.Type(value = Comment.class, name = "comment"),
        @JsonSubTypes.Type(value = User.class, name = "user"),
        @JsonSubTypes.Type(value = Playlist.class, name = "playlist"),
        @JsonSubTypes.Type(value = SoundAssociation.class, name = "stream_item"),
        @JsonSubTypes.Type(value = Connection.class, name = "connection"),
        @JsonSubTypes.Type(value = Like.class, name = "like"),
        @JsonSubTypes.Type(value = Friend.class, name = "friend")})
public abstract class ScResource
        extends ScModel
        implements Identifiable, Persisted {

    @JsonIgnore
    public long last_updated = NOT_SET;

    public ScResource() {
    }

    public ScResource(long id) {
        super(id);
    }

    public void setUpdated() {
        last_updated = System.currentTimeMillis();
    }

    public enum CacheUpdateMode {
        NONE, MINI, FULL;

        public boolean shouldUpdate() {
            return this != NONE;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScResource)) return false;

        ScResource resourceBase = (ScResource) o;
        return mID == resourceBase.mID;
    }

    @Override
    public int hashCode() {
        return (int) (mID ^ (mID >>> 32));
    }

    /**
     * @return whether this object has been saved to the database.
     */
    public boolean isSaved() {
        return mID > NOT_SET;
    }

    /**
     * Add resource's dependencies to the given map.
     * Used for object persistence in DB.
     * @param destination
     */
    @JsonIgnore
    public void putDependencyValues(@NotNull BulkInsertMap destination){
        // no dependencies by default
    }

    /**
     * Put all necessary content values into the map, includeing the object itself
     * @param destination
     */
    public void putFullContentValues(@NotNull BulkInsertMap destination){
        putDependencyValues(destination);
        destination.add(getBulkInsertUri(), buildContentValues());
    }

    public Intent getViewIntent(){
        return null;
    }

    public static class ScResourceHolder<T extends ScResource> extends CollectionHolder<T> {}
}
