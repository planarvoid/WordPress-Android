package com.soundcloud.android.playlists;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.view.adapters.CellPresenter;
import com.soundcloud.android.view.adapters.ItemAdapter;
import com.soundcloud.android.view.adapters.LegacyPlayableRowPresenter;
import dagger.Module;
import dagger.Provides;

import android.content.res.Resources;

import javax.inject.Provider;

@Module(addsTo = ApplicationModule.class,
        injects = {
                PlaylistDetailActivity.class,
                PlaylistFragment.class,
                AddToPlaylistDialogFragment.class,
                CreatePlaylistDialogFragment.class
        }
)
public class PlaylistsModule {

    static final int INITIAL_ADAPTER_SIZE = 20;

    @Provides
    public PlaylistDetailsController providePlaylistTracksAdapter(Resources resources,
                                                                  Provider<SplitScreenController> splitScreenController,
                                                                  Provider<DefaultController> defaultController) {
        if (resources.getBoolean(R.bool.split_screen_details_pages)) {
            return splitScreenController.get();
        } else {
            return defaultController.get();
        }
    }

    @Provides
    public ItemAdapter<Track> provideSplitScreenItemAdapter(CellPresenter<Track> playableRowPresenter) {
        return new ItemAdapter<Track>(playableRowPresenter);
    }

    @Provides
    public CellPresenter<Track> provideTrackRowPresenter(ImageOperations imageOperations) {
        return new LegacyPlayableRowPresenter<Track>(imageOperations);
    }
}
