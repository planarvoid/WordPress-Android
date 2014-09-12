package com.soundcloud.android.explore;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.view.adapters.EndlessAdapter;
import com.soundcloud.android.view.adapters.TrackGridPresenter;
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
    public EndlessAdapter<ApiTrack> provideEndlessAdapter(TrackGridPresenter presenter) {
        return new EndlessAdapter<ApiTrack>(R.layout.grid_loading_item, presenter);
    }
}
