package com.soundcloud.android.playlists;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.R;
import com.soundcloud.android.collections.views.PlayableRow;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.view.adapters.ItemAdapter;
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
    public ItemAdapter<Track, PlayableRow> provideItemAdapter(PlaylistTrackPresenter presenter) {
        return new ItemAdapter<Track, PlayableRow>(presenter, INITIAL_ADAPTER_SIZE);
    }
}
