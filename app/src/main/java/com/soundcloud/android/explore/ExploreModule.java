package com.soundcloud.android.explore;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.view.adapters.TrackCellPresenter;
import dagger.Module;
import dagger.Provides;

@Module(addsTo = ApplicationModule.class,
        injects = {
                ExploreFragment.class,
                ExploreTracksFragment.class,
                ExploreGenresFragment.class
        })
public class ExploreModule {

    @Provides
    public PagingItemAdapter<TrackSummary> provideTracksAdapter(TrackCellPresenter presenter) {
        return new PagingItemAdapter<TrackSummary>(presenter, Consts.CARD_PAGE_SIZE, R.layout.grid_loading_item);
    }
}
