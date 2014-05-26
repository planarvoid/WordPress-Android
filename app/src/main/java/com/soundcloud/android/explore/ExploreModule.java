package com.soundcloud.android.explore;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.view.adapters.TrackGridPresenter;
import dagger.Module;
import dagger.Provides;

import android.view.View;

@Module(addsTo = ApplicationModule.class,
        injects = {
                ExploreFragment.class,
                ExploreTracksFragment.class,
                ExploreGenresFragment.class
        })
public class ExploreModule {

    @Provides
    public PagingItemAdapter<TrackSummary, View> provideTracksAdapter(TrackGridPresenter presenter) {
        return new PagingItemAdapter<TrackSummary, View>(presenter, Consts.CARD_PAGE_SIZE, R.layout.grid_loading_item);
    }
}
