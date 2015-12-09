package com.soundcloud.android.playlists;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.R;
import dagger.Module;
import dagger.Provides;

import android.content.res.Resources;

import javax.inject.Provider;

@Module(addsTo = ApplicationModule.class,
        injects = {
                PlaylistDetailActivity.class,
                PlaylistDetailFragment.class,
                AddToPlaylistDialogFragment.class,
                CreatePlaylistDialogFragment.class,
                DeletePlaylistDialogFragment.class
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

}
