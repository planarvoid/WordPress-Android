package com.soundcloud.android.playlists;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.associations.RepostsModule;
import com.soundcloud.android.commands.PagedQueryCommand;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.likes.ChronologicalQueryParams;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.tracks.TrackItemPresenter;
import com.soundcloud.android.view.adapters.ItemAdapter;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropertySet;
import dagger.Lazy;
import dagger.Module;
import dagger.Provides;

import android.content.Context;
import android.content.res.Resources;

import javax.inject.Named;
import javax.inject.Provider;

@Module(addsTo = ApplicationModule.class,
        injects = {
                PlaylistsFragment.class,
                PlaylistDetailActivity.class,
                PlaylistDetailFragment.class,
                PlaylistLikesFragment.class,
                PlaylistPostsFragment.class,
                AddToPlaylistDialogFragment.class,
                CreatePlaylistDialogFragment.class
        },
        includes = RepostsModule.class
)
public class PlaylistsModule {

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
    public ItemAdapter<PropertySet> provideSplitScreenItemAdapter(TrackItemPresenter trackRowPresenter) {
        return new ItemAdapter<>(trackRowPresenter);
    }

    @Provides
    public PlaylistEngagementsView providePlaylistEngagementsView(Context context,
                                                                  Resources resources,
                                                                  PopupMenuWrapper.Factory popupMenuWrapperFactory,
                                                                  FeatureFlags featureFlags,
                                                                  FeatureOperations featureOperations) {
        if (featureFlags.isEnabled(Flag.NEW_PLAYLIST_ENGAGEMENTS)) {
            return new NewPlaylistEngagementsView(context, resources, popupMenuWrapperFactory, featureOperations);
        } else {
            return new LegacyPlaylistEngagementsView(context, resources);
        }
    }

    @Provides
    @Named("LoadPostedPlaylistsCommand")
    public PagedQueryCommand<ChronologicalQueryParams> provideLoadPostedPlaylistsCommand(FeatureFlags featureFlags,
                                                                                         PropellerDatabase database,
                                                                                         AccountOperations accountOperations) {
        if (featureFlags.isEnabled(Flag.NEW_POSTS_SYNCER)) {
            return new LoadPostedPlaylistsCommand(database);
        } else {
            return new LegacyLoadPostedPlaylistsCommand(database, accountOperations);
        }
    }

    @Provides
    PlaylistCreator providePlaylistCreator(FeatureFlags featureFlags,
                                           Lazy<PlaylistOperations> playlistOps,
                                           Lazy<LegacyPlaylistOperations> legacyPlauylistOps) {
        if (featureFlags.isEnabled(Flag.NEW_POSTS_SYNCER)) {
            return playlistOps.get();
        } else {
            return legacyPlauylistOps.get();
        }
    }
}
