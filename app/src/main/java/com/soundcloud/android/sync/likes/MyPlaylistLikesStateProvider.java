package com.soundcloud.android.sync.likes;

import javax.inject.Inject;

public class MyPlaylistLikesStateProvider {

    @Inject
    public MyPlaylistLikesStateProvider() {
    }

    public boolean hasLocalChanges() {
        return false;
    }
}
