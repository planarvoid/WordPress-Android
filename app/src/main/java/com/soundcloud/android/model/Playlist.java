package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonView;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.json.Views;
import com.soundcloud.android.provider.BulkInsertMap;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import org.jetbrains.annotations.Nullable;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class Playlist extends Playable {

    public static final String EXTRA = "com.soundcloud.android.playlist";
    public static final String EXTRA_ID = "com.soundcloud.android.playlist_id";

    @JsonView(Views.Full.class) public String playlist_type;
    @JsonView(Views.Full.class) public String tracks_uri;
    @JsonView(Views.Full.class) @Nullable public List<Track> tracks;
    @JsonView(Views.Full.class) public int track_count;

    public static Playlist fromIntent(Intent intent) {
        Playlist playlist = intent.getParcelableExtra(EXTRA);
        if (playlist == null) {
            playlist = SoundCloudApplication.MODEL_MANAGER.getPlaylist(intent.getLongExtra(EXTRA_ID, 0));
            if (playlist == null) {
                throw new IllegalArgumentException("Could not obtain playlist from intent " + intent);
            }
        }
        return playlist;
    }

    public Playlist() {
        super();
    }

    public Playlist(Parcel in) {
        Bundle b = in.readBundle(getClass().getClassLoader());
        super.readFromBundle(b);

        playlist_type = b.getString("playlist_type");
        tracks_uri = b.getString("tracks_uri");
        track_count = b.getInt("track_count");
        tracks = b.getParcelableArrayList("tracks");
    }

    public Playlist(Cursor cursor) {
        super(cursor);
        tracks_uri = cursor.getString(cursor.getColumnIndex(DBHelper.Sounds.TRACKS_URI));
        track_count = cursor.getInt(cursor.getColumnIndex(DBHelper.Sounds.TRACK_COUNT));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle b = super.getBundle();
        b.putString("playlist_type", playlist_type);
        b.putString("tracks_uri", tracks_uri);
        b.putInt("track_count", track_count);
        b.putParcelableArrayList("tracks", (ArrayList<? extends Parcelable>) tracks);
        dest.writeBundle(b);
    }

    public Playlist updateFrom(Playlist updatedItem, CacheUpdateMode cacheUpdateMode) {
        super.updateFrom(updatedItem, cacheUpdateMode);
        tracks_uri = updatedItem.tracks_uri;
        track_count = updatedItem.track_count;
        playlist_type = updatedItem.playlist_type;
        if (cacheUpdateMode == CacheUpdateMode.FULL) {
            tracks = updatedItem.tracks;
        }
        return this;
    }

    @Override
    public String toString() {
        return "Playlist{" +
                "id=" + id +
                ", title='" + title + "'" +
                ", permalink_url='" + permalink_url + "'" +
                ", duration=" + duration +
                ", user=" + user +
                ", track_count=" + (track_count == -1 ? (tracks != null ? tracks.size() : "-1") : track_count) +
                ", tracks_uri='" + tracks_uri + '\'' +
                '}';
    }

    public ContentValues buildContentValues() {
        ContentValues cv = super.buildContentValues();
        cv.put(DBHelper.Sounds.TRACKS_URI, tracks_uri);
        cv.put(DBHelper.Sounds.TRACK_COUNT, track_count);
        return cv;
    }

    @Override
    public Uri getBulkInsertUri() {
        return Content.PLAYLISTS.uri;
    }

    @Override
    public ScResource getRefreshableResource() {
        return null;
    }

    @Override
    public void putDependencyValues(BulkInsertMap destMap) {
        super.putDependencyValues(destMap);
        if (tracks != null) {
            int i = 0;
            for (Track t : tracks) {
                t.putFullContentValues(destMap);

                // add to relationship table
                ContentValues cv = new ContentValues();
                cv.put(DBHelper.PlaylistTracks.TRACK_ID,t.id);
                cv.put(DBHelper.PlaylistTracks.POSITION,i);
                destMap.add(Content.PLAYLIST_TRACKS.forId(id), cv);
                i++;
            }
        }
    }

    @Override
    public Uri toUri() {
        return Content.PLAYLISTS.forId(id);
    }


    @Override
    public int getTypeId() {
        return DB_TYPE_PLAYLIST;
    }

    public static final Creator<Playlist> CREATOR = new Creator<Playlist>() {
        @Override
        public Playlist createFromParcel(Parcel source) {
            return new Playlist(source);
        }

        @Override
        public Playlist[] newArray(int size) {
            return new Playlist[size];
        }
    };
}
