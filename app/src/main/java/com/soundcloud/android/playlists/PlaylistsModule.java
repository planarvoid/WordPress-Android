package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.collection.playlists.LegacyPlaylistHeaderRenderer;
import com.soundcloud.android.collection.playlists.NewPlaylistHeaderRenderer;
import com.soundcloud.android.collection.playlists.NewPlaylistsFragment;
import com.soundcloud.android.collection.playlists.PlaylistHeaderRenderer;
import com.soundcloud.android.collection.playlists.PlaylistsFragment;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import dagger.Module;
import dagger.Provides;

import android.content.res.Resources;
import android.support.v4.app.Fragment;

import javax.inject.Named;

@Module
public class PlaylistsModule {

    public static final String PLAYLISTS_FRAGMENT = "PlaylistsFragment";
    static final String FULLSCREEN_PLAYLIST_DETAILS = "fullscreen_playlist_details";

    @Named(FULLSCREEN_PLAYLIST_DETAILS)
    @Provides
    boolean showFullscreenPlaylistDetails(FeatureFlags featureFlags, Resources resources) {
        return featureFlags.isEnabled(Flag.EDIT_PLAYLIST_V2) && resources.getBoolean(R.bool.show_fullscreen_playlist_details);
    }

    @Provides
    @Named(PLAYLISTS_FRAGMENT)
    public Fragment providePlaylistsFragment(FeatureFlags featureFlags) {
        if (featureFlags.isEnabled(Flag.FILTER_COLLECTIONS)) {
            return new NewPlaylistsFragment();
        } else {
            return new PlaylistsFragment();
        }
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
