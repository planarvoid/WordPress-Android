package com.soundcloud.android.playlists;

import com.soundcloud.android.api.ApiModule;
import com.soundcloud.android.storage.StorageModule;
import dagger.Module;

@Module(complete = false, library = true,
        injects = {PlaylistDetailActivity.class},
        includes = {StorageModule.class, ApiModule.class}
)
class PlaylistsModule {
}
