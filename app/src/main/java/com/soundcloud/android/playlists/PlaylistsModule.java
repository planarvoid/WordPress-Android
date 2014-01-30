package com.soundcloud.android.playlists;

import com.soundcloud.android.api.ApiModule;
import com.soundcloud.android.storage.StorageModule;
import dagger.Module;

@Module(complete = false,
        injects = {PlaylistDetailActivity.class, PlaylistTracksFragment.class},
        includes = {StorageModule.class, ApiModule.class}
)
class PlaylistsModule {
}
