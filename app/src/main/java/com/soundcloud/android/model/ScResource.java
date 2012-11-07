package com.soundcloud.android.model;

import android.content.ContentValues;
import android.net.Uri;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;

import java.util.ArrayList;
import java.util.List;

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
        @JsonSubTypes.Type(value = SoundAssociation.class, name = "stream_item")})
public abstract class ScResource extends ScModel {


    public static final List<ScResource> EMPTY_LIST = new ArrayList<ScResource>();
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

    public abstract Uri getBulkInsertUri();

    public abstract User getUser();

    public abstract Sound getSound();

    public static class ScResourceHolder extends CollectionHolder<ScResource> {
    }

    public static class CommentHolder extends CollectionHolder<Comment> {
    }
}
