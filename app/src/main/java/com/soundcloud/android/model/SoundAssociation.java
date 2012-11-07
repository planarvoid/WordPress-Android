package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.provider.ScContentProvider;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;

import java.util.Date;

public class SoundAssociation extends ScResource{

    public int associationType;
    public Date created_at;

    public Track track;
    public Playlist playlist;

    public SoundAssociation() { //for deserialization
    }

    public SoundAssociation(Parcel in) {
        associationType = in.readInt();
        created_at = new Date(in.readLong());
        track = in.readParcelable(ClassLoader.getSystemClassLoader());
        playlist = in.readParcelable(ClassLoader.getSystemClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(associationType);
        dest.writeLong(created_at.getTime());
        dest.writeParcelable(track,0);
        dest.writeParcelable(playlist,0);
    }



    @Override
    public Uri getBulkInsertUri() {
        return null;
    }

    @Override
    public User getUser() {
        return track != null ? track.user : (playlist != null ? playlist.user: null);
    }

    @Override
    public Sound getSound() {
        return track != null ? track : playlist;
    }

    @JsonProperty("type")
    public void setType(String type){
        if (type.equalsIgnoreCase("track")) {
            associationType = ScContentProvider.CollectionItemTypes.TRACK;
        } else if (type.equalsIgnoreCase("track_repost")) {
            associationType = ScContentProvider.CollectionItemTypes.TRACK_REPOST;
        } else if (type.equalsIgnoreCase("track_like")) {
            associationType = ScContentProvider.CollectionItemTypes.TRACK_LIKE;
        } else if (type.equalsIgnoreCase("playlist")) {
            associationType = ScContentProvider.CollectionItemTypes.PLAYLIST;
        } else if (type.equalsIgnoreCase("playlist_repost")) {
            associationType = ScContentProvider.CollectionItemTypes.PLAYLIST_REPOST;
        } else if (type.equalsIgnoreCase("playlist_like")) {
            associationType = ScContentProvider.CollectionItemTypes.PLAYLIST_LIKE;

        }

    }
}
