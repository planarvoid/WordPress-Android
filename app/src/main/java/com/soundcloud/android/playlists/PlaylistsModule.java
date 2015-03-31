package com.soundcloud.android.playlists;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.R;
import com.soundcloud.android.commands.PagedQueryCommand;
import com.soundcloud.android.likes.ChronologicalQueryParams;
import com.soundcloud.propeller.PropellerDatabase;
import dagger.Module;
import dagger.Provides;

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
        }
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
    @Named("LoadPostedPlaylistsCommand")
    public PagedQueryCommand<ChronologicalQueryParams> provideLoadPostedPlaylistsCommand(PropellerDatabase database) {
        return new LoadPostedPlaylistsCommand(database);
    }
}
