package com.soundcloud.android.explore;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.R;
import com.soundcloud.android.tracks.TrackGridRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.presentation.PagingItemAdapter;
import dagger.Module;
import dagger.Provides;

@Module(addsTo = ApplicationModule.class,
        injects = {
                ExploreTracksCategoryActivity.class,
                ExploreFragment.class,
                ExploreTracksFragment.class,
                ExploreGenresFragment.class
        })
public class ExploreModule {

    @Provides
    public PagingItemAdapter<TrackItem> provideEndlessAdapter(TrackGridRenderer renderer) {
        return new PagingItemAdapter<>(R.layout.grid_loading_item, renderer);
    }
}
