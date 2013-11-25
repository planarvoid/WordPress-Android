package com.soundcloud.android.sync.exception;

public class PlaylistUpdateException extends Exception {

    public PlaylistUpdateException(String apiUpdateObject) {
        super(apiUpdateObject);
    }

}
