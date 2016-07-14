package com.soundcloud.android.collection.recentlyplayed;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.presentation.RecyclerItemAdapter;

import android.view.View;

@AutoFactory(allowSubclasses = true)
class RecentlyPlayedAdapter extends PagingRecyclerItemAdapter<RecentlyPlayedItem, RecyclerItemAdapter.ViewHolder> {

    RecentlyPlayedAdapter(boolean fixedWidth,
                          @Provided RecentlyPlayedPlaylistRendererFactory recentlyPlayedPlaylistRendererFactory,
                          @Provided RecentlyPlayedProfileRendererFactory recentlyPlayedProfileRendererFactory,
                          @Provided RecentlyPlayedStationRendererFactory recentlyPlayedStationRendererFactory) {
        super(new CellRendererBinding<>(RecentlyPlayedItem.TYPE_RECENTLY_PLAYED_PLAYLIST, recentlyPlayedPlaylistRendererFactory.create(fixedWidth)),
              new CellRendererBinding<>(RecentlyPlayedItem.TYPE_RECENTLY_PLAYED_PROFILE, recentlyPlayedProfileRendererFactory.create(fixedWidth)),
              new CellRendererBinding<>(RecentlyPlayedItem.TYPE_RECENTLY_PLAYED_STATION, recentlyPlayedStationRendererFactory.create(fixedWidth)));
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
