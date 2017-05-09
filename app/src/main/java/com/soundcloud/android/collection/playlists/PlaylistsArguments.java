package com.soundcloud.android.collection.playlists;

import android.os.Bundle;

final class PlaylistsArguments {
    private static final String ENTITIES_KEY = "entities_key";

    static Bundle from(PlaylistsOptions.Entities entities) {
        Bundle bundle = new Bundle();
        bundle.putInt(ENTITIES_KEY, entities.ordinal());
        return bundle;
    }

    static PlaylistsOptions.Entities entities(Bundle bundle) {
        return PlaylistsOptions.Entities.values()[bundle.getInt(ENTITIES_KEY, -1)];
    }
}
