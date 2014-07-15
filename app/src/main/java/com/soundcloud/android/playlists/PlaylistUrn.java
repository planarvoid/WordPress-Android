package com.soundcloud.android.playlists;

import com.soundcloud.android.model.Urn;

public final class PlaylistUrn extends Urn {

    public PlaylistUrn(long id) {
        super(SOUNDCLOUD_SCHEME, PLAYLISTS_TYPE, id);
    }
}
