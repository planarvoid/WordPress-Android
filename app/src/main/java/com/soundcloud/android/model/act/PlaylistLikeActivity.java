package com.soundcloud.android.model.act;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;

import android.database.Cursor;

public class PlaylistLikeActivity extends Activity {
    @JsonProperty public Playlist playlist;

    public PlaylistLikeActivity() {
        super();
    }

    public PlaylistLikeActivity(Cursor cursor) {
        super(cursor);
    }

    @Override
    public Type getType() {
        return Type.PLAYLIST_LIKE;
    }

    @Override
    public Track getTrack() {
        return null;
    }

    @Override
    public User getUser() {
        return null;
    }

    @Override
    public Playlist getPlaylist() {
        return playlist;
    }

    @Override
    public void setCachedTrack(Track track) {
    }

    @Override
    public void setCachedUser(User user) {
    }

    @Override
    public ScResource getRefreshableResource() {
        return null; // TODO
    }
}
