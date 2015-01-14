package com.soundcloud.android.sync.playlists;

public class PlaylistUpdateException extends Exception {

    public PlaylistUpdateException(String apiUpdateObject) {
        super(apiUpdateObject);
    }

}
