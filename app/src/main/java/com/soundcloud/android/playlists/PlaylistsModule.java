package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import dagger.Module;
import dagger.Provides;

import android.content.res.Resources;

@Module(complete = false,
        injects = {
                PlaylistDetailActivity.class,
                PlaylistFragment.class,
                AddToPlaylistDialogFragment.class,
                CreatePlaylistDialogFragment.class
        }
)
public class PlaylistsModule {

    @Provides
    public PlaylistDetailsController providePlaylistTracksAdapter(Resources resources, ImageOperations imageOperations) {
        if (resources.getBoolean(R.bool.split_screen_details_pages)) {
            return new SplitScreenController(imageOperations);
        } else {
            return new DefaultController(imageOperations);
        }
    }
}
