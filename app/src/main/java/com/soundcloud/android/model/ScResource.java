package com.soundcloud.android.model;

import static com.soundcloud.android.SoundCloudApplication.*;

import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundcloud.android.SoundCloudApplication;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Track.class, name = "track"),
        @JsonSubTypes.Type(value = Playlist.class, name = "playlist"),
        @JsonSubTypes.Type(value = Comment.class, name = "comment"),
        @JsonSubTypes.Type(value = User.class, name = "user")})
public abstract class ScResource extends ScModel {


    public static final List<ScResource> EMPTY_LIST = new ArrayList<ScResource>();
    @JsonIgnore public long last_updated       = NOT_SET;

    public ScResource() {
    }

    public static @NotNull Activities getActivitiesFromStream(InputStream is,
                                                                    ObjectMapper mapper) throws IOException {
        return mapper.readValue(is, Activities.class);
    }

    @Deprecated // XXX this is slow (reflection)
    protected void readFromParcel(Parcel in) {
        Bundle data = in.readBundle(getClass().getClassLoader());
        for (String key : data.keySet()) {
            try {
                setFieldFromBundle(this, getClass().getDeclaredField(key), data, key);
            } catch (SecurityException e) {
                Log.e(TAG, "error ", e);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "error ", e);
            } catch (NoSuchFieldException e) {
                try {
                    setFieldFromBundle(this, getClass().getField(key), data, key);
                } catch (NoSuchFieldException ignored) {
                    Log.e(TAG, "error ", ignored);
                }
            }
        }
        this.id = data.getLong("id");
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

    public static class ScResourceHolder extends CollectionHolder<ScResource> {}
    public static class CommentHolder extends CollectionHolder<Comment> {}
}
