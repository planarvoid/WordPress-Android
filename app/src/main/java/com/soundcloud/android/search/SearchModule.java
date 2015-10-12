package com.soundcloud.android.search;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.R;
import com.soundcloud.android.associations.AssociationsModule;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.presentation.ProgressCellRenderer;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.presentation.RecyclerItemAdapter.ViewHolder;
import com.soundcloud.android.view.adapters.PlaylistGridRenderer;
import dagger.Module;
import dagger.Provides;

import android.view.View;

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
    public PagingRecyclerItemAdapter<PlaylistItem, ViewHolder> playlistsResultAdapter(
            PlaylistGridRenderer itemRenderer) {
        return new PagingRecyclerItemAdapter<PlaylistItem, ViewHolder>(
                itemRenderer, new ProgressCellRenderer(R.layout.grid_loading_item)) {
            @Override
            protected ViewHolder createViewHolder(View itemView) {
                return new RecyclerItemAdapter.ViewHolder(itemView);
            }

            @Override
            public int getBasicItemViewType(int position) {
                return 0;
            }
        };
    }

    @Provides
    public Random provideRandom() {
        return new Random();
    }
}
