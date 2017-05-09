package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.collection.playlists.LegacyPlaylistHeaderRenderer;
import com.soundcloud.android.collection.playlists.NewPlaylistHeaderRenderer;
import com.soundcloud.android.collection.playlists.PlaylistHeaderRenderer;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import dagger.Module;
import dagger.Provides;

import android.content.res.Resources;

import javax.inject.Named;

@Module
public class PlaylistsModule {

    static final String FULLSCREEN_PLAYLIST_DETAILS = "fullscreen_playlist_details";

    @Named(FULLSCREEN_PLAYLIST_DETAILS)
    @Provides
    boolean showFullscreenPlaylistDetails(Resources resources) {
        return resources.getBoolean(R.bool.show_fullscreen_playlist_details);
    }

    @Provides
    public PlaylistHeaderRenderer providePlaylistCollectionHeaderRenderer(FeatureFlags featureFlags,
                                                                          LegacyPlaylistHeaderRenderer headerRenderer,
                                                                          NewPlaylistHeaderRenderer newHeaderRenderer) {
        if (featureFlags.isEnabled(Flag.FILTER_COLLECTIONS)) {
            return newHeaderRenderer;
        } else {
            return headerRenderer;
        }
    }

}
