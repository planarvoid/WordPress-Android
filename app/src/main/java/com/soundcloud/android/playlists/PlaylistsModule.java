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
import javax.inject.Provider;

@Module
public class PlaylistsModule {

    public static final String PLAYLISTS_FRAGMENT = "PlaylistsFragment";

    @Provides
    public PlaylistDetailsController providePlaylistViewController(Resources resources,
                                                                   Provider<SplitScreenController> splitScreenController,
                                                                   Provider<DefaultController> defaultController) {
        if (resources.getBoolean(R.bool.split_screen_details_pages)) {
            return splitScreenController.get();
        } else {
            return defaultController.get();
        }
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
