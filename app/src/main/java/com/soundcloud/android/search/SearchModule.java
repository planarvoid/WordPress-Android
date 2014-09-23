package com.soundcloud.android.search;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.associations.AssociationsModule;
import com.soundcloud.android.view.adapters.EndlessAdapter;
import com.soundcloud.android.view.adapters.PlaylistGridPresenter;
import dagger.Module;
import dagger.Provides;

@Module(addsTo = ApplicationModule.class,
        injects = {
                TabbedSearchFragment.class,
                SearchActivity.class,
                SearchResultsFragment.class,
                PlaylistTagsFragment.class,
                PlaylistResultsFragment.class
        }, includes = AssociationsModule.class)
public class SearchModule {

    @Provides
    public EndlessAdapter<ApiPlaylist> playlistsResultAdapter(PlaylistGridPresenter presenter) {
        return new EndlessAdapter<>(R.layout.grid_loading_item, presenter);
    }
}
