package com.soundcloud.android.collection.recentlyplayed;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.collection.SimpleHeaderRenderer;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.presentation.RecyclerItemAdapter;

import android.view.View;

@AutoFactory(allowSubclasses = true)
class RecentlyPlayedAdapter extends PagingRecyclerItemAdapter<RecentlyPlayedItem, RecyclerItemAdapter.ViewHolder> {

    RecentlyPlayedAdapter(boolean fixedWidth,
                          SimpleHeaderRenderer.MenuClickListener listener,
                          @Provided RecentlyPlayedHeaderRendererFactory recentlyPlayedHeaderRendererFactory,
                          @Provided RecentlyPlayedPlaylistRendererFactory recentlyPlayedPlaylistRendererFactory,
                          @Provided RecentlyPlayedProfileRendererFactory recentlyPlayedProfileRendererFactory,
                          @Provided RecentlyPlayedStationRendererFactory recentlyPlayedStationRendererFactory) {
        super(new CellRendererBinding<>(RecentlyPlayedItem.Kind.RecentlyPlayedHeader.ordinal(),
                                        recentlyPlayedHeaderRendererFactory.create(listener)),
              new CellRendererBinding<>(RecentlyPlayedItem.Kind.RecentlyPlayedPlaylist.ordinal(),
                                        recentlyPlayedPlaylistRendererFactory.create(fixedWidth)),
              new CellRendererBinding<>(RecentlyPlayedItem.Kind.RecentlyPlayedProfile.ordinal(),
                                        recentlyPlayedProfileRendererFactory.create(fixedWidth)),
              new CellRendererBinding<>(RecentlyPlayedItem.Kind.RecentlyPlayedStation.ordinal(),
                                        recentlyPlayedStationRendererFactory.create(fixedWidth)));
    }

    @Override
    protected RecyclerItemAdapter.ViewHolder createViewHolder(View view) {
        return new ViewHolder(view);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return getItem(position).getKind().ordinal();
    }

}
