package com.soundcloud.android.search;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.R;
import com.soundcloud.android.associations.AssociationsModule;
import com.soundcloud.android.model.PlaylistSummary;
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
        }, includes = AssociationsModule.class)
public class SearchModule {

    @Provides
    public PagingItemAdapter<PlaylistSummary> playlistsResultAdapter(PlaylistGridPresenter presenter) {
        return new PagingItemAdapter<PlaylistSummary>(R.layout.grid_loading_item, presenter);
    }
}
