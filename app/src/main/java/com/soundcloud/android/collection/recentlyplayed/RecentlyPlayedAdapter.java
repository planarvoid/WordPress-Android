package com.soundcloud.android.collection.recentlyplayed;

import com.soundcloud.android.collection.CollectionItem;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.presentation.RecyclerItemAdapter;

import android.view.View;

import javax.inject.Inject;

class RecentlyPlayedAdapter extends PagingRecyclerItemAdapter<CollectionItem, RecyclerItemAdapter.ViewHolder> {

    @Inject
    RecentlyPlayedAdapter(RecentlyPlayedPlaylistRenderer recentlyPlayedPlaylistRenderer,
                          RecentlyPlayedProfileRenderer recentlyPlayedProfileRenderer,
                          RecentlyPlayedStationRenderer recentlyPlayedStationRenderer) {
        super(new CellRendererBinding<>(CollectionItem.TYPE_RECENTLY_PLAYED_PLAYLIST, recentlyPlayedPlaylistRenderer),
                new CellRendererBinding<>(CollectionItem.TYPE_RECENTLY_PLAYED_PROFILE, recentlyPlayedProfileRenderer),
                new CellRendererBinding<>(CollectionItem.TYPE_RECENTLY_PLAYED_STATION, recentlyPlayedStationRenderer));
    }

    @Override
    protected RecyclerItemAdapter.ViewHolder createViewHolder(View view) {
        return new ViewHolder(view);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return getItem(position).getType();
    }
}
