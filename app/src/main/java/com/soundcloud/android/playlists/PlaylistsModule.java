package com.soundcloud.android.playlists;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.view.adapters.CellPresenter;
import com.soundcloud.android.view.adapters.ItemAdapter;
import com.soundcloud.android.view.adapters.TrackItemPresenter;
import com.soundcloud.propeller.PropertySet;
import dagger.Module;
import dagger.Provides;

import android.content.res.Resources;
import android.view.LayoutInflater;

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
    public ItemAdapter<PropertySet> provideSplitScreenItemAdapter(CellPresenter<PropertySet> playableRowPresenter) {
        return new ItemAdapter<PropertySet>(playableRowPresenter);
    }

    @Provides
    public CellPresenter<PropertySet> provideTrackRowPresenter(LayoutInflater layoutInflater, ImageOperations imageOperations) {
        return new TrackItemPresenter(layoutInflater, imageOperations);
    }
}
