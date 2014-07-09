package com.soundcloud.android.model;

public final class PlaylistUrn extends Urn {

    protected PlaylistUrn(long id) {
        super(SOUNDCLOUD_SCHEME, PLAYLISTS_TYPE, id);
    }
}
