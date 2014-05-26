package com.soundcloud.android.search;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import com.soundcloud.android.model.PlaylistSummary;
import com.soundcloud.android.view.adapters.PlaylistCellPresenter;
import dagger.Module;
import dagger.Provides;

@Module(addsTo = ApplicationModule.class,
        injects = {
                TabbedSearchFragment.class,
                SearchResultsFragment.class,
                PlaylistTagsFragment.class,
                PlaylistResultsFragment.class
        })
public class SearchModule {

    @Provides
    public PagingItemAdapter<PlaylistSummary> playlistsResultAdapter(PlaylistCellPresenter presenter) {
        return new PagingItemAdapter<PlaylistSummary>(presenter, Consts.CARD_PAGE_SIZE, R.layout.grid_loading_item);
    }

}
