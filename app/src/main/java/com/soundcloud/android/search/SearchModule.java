package com.soundcloud.android.search;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.PlaylistSummary;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.view.adapters.CellPresenter;
import com.soundcloud.android.view.adapters.LegacyPlayableRowPresenter;
import com.soundcloud.android.view.adapters.LegacyUserRowPresenter;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import com.soundcloud.android.view.adapters.PlaylistGridPresenter;
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
    public PagingItemAdapter<PlaylistSummary> playlistsResultAdapter(PlaylistGridPresenter presenter) {
        return new PagingItemAdapter<PlaylistSummary>(R.layout.grid_loading_item, presenter);
    }

    @Provides
    public SearchResultsAdapter searchResultAdapter(ImageOperations imageOperations) {
        final CellPresenter[] cellPresenters = new CellPresenter[] {
                new LegacyUserRowPresenter(imageOperations, Screen.SEARCH_EVERYTHING),
                new LegacyPlayableRowPresenter<ScResource>(imageOperations)
        };
        return new SearchResultsAdapter(cellPresenters);
    }
}
