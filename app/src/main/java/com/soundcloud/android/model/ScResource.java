package com.soundcloud.android.model;

import static com.soundcloud.android.SoundCloudApplication.*;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundcloud.android.model.act.Activities;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
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
        @JsonSubTypes.Type(value = Friend.class, name = "friend"),
        @JsonSubTypes.Type(value = Playlist.class, name = "playlist")})
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

    public abstract Track getTrack();

    public static class ScResourceHolder extends CollectionHolder<ScResource> {
    }

    public static class CommentHolder extends CollectionHolder<Comment> {
    }
}
