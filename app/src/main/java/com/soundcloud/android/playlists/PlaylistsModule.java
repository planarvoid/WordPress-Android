package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import dagger.Module;
import dagger.Provides;

import android.content.res.Resources;

import javax.inject.Named;

@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod") // abstract to force @Provides methods to be static
@Module
public abstract class PlaylistsModule {

    static final String FULLSCREEN_PLAYLIST_DETAILS = "fullscreen_playlist_details";

    @Named(FULLSCREEN_PLAYLIST_DETAILS)
    @Provides
    static boolean showFullscreenPlaylistDetails(Resources resources) {
        return resources.getBoolean(R.bool.show_fullscreen_playlist_details);
    }

}
