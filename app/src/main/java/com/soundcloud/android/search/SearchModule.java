package com.soundcloud.android.search;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.R;
import com.soundcloud.android.associations.AssociationsModule;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.PagingItemAdapter;
import com.soundcloud.android.view.adapters.PlaylistGridRenderer;
import dagger.Module;
import dagger.Provides;

import java.util.Random;

@Module(addsTo = ApplicationModule.class,
        injects = {
                TabbedSearchFragment.class,
                SearchActivity.class,
                SearchResultsFragment.class,
                PlaylistTagsFragment.class,
                PlaylistResultsFragment.class,
                PlayFromVoiceSearchActivity.class
        }, includes = AssociationsModule.class)
public class SearchModule {

    @Provides
    public PagingItemAdapter<PlaylistItem> playlistsResultAdapter(PlaylistGridRenderer renderer) {
        return new PagingItemAdapter<>(R.layout.grid_loading_item, renderer);
    }

    @Provides
    public Random provideRandom() {
        return new Random();
    }
}
