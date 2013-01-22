package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.soundcloud.android.provider.BulkInsertMap;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import org.jetbrains.annotations.NotNull;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

import java.util.ArrayList;

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
public abstract class ScResource extends ScModel {

    @JsonIgnore
    public long last_updated = NOT_SET;

    public ScResource() {
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
        return id == resourceBase.id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    /**
     * @return whether this object has been saved to the database.
     */
    public boolean isSaved() {
        return id > NOT_SET;
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

    public Uri insert(ContentResolver contentResolver) {
        insertDependencies(contentResolver);
        return contentResolver.insert(toUri(),buildContentValues());
    }

    protected void insertDependencies(ContentResolver contentResolver) {
        final BulkInsertMap dependencies = new BulkInsertMap();
        putDependencyValues(dependencies);
        dependencies.insert(contentResolver);
    }

    public abstract Uri toUri();

    public abstract Uri getBulkInsertUri();

    public abstract User getUser();

    public abstract Playable getPlayable();

    public static class ScResourceHolder extends CollectionHolder<ScResource> {

        /**
         * Insert the collection resources using a given URI along with dependencies
         * @param resolver
         * @param contentUri
         * @return the total resources inserted, including dependencies
         */
        public int insert(ContentResolver resolver, @NotNull Uri contentUri) {
            BulkInsertMap map = new BulkInsertMap();
            for (ScResource r : this) {
                r.putDependencyValues(map);
                map.add(contentUri, r.buildContentValues());
            }
            return map.insert(resolver);
        }
    }

}
