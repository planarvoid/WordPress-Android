package com.soundcloud.android.collection.recentlyplayed;

import static com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedItem.Kind.RecentlyPlayedEmpty;
import static com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedItem.Kind.RecentlyPlayedHeader;
import static com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedItem.Kind.RecentlyPlayedPlaylist;
import static com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedItem.Kind.RecentlyPlayedProfile;
import static com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedItem.Kind.RecentlyPlayedStation;

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
                          SimpleHeaderRenderer.Listener listener,
                          @Provided RecentlyPlayedHeaderRenderer headerRenderer,
                          @Provided RecentlyPlayedPlaylistRendererFactory playlistRendererFactory,
                          @Provided RecentlyPlayedProfileRendererFactory profileRendererFactory,
                          @Provided RecentlyPlayedStationRendererFactory stationRendererFactory,
                          @Provided RecentlyPlayedEmptyRenderer emptyRenderer) {
        super(new CellRendererBinding<>(RecentlyPlayedEmpty.ordinal(), emptyRenderer),
              new CellRendererBinding<>(RecentlyPlayedHeader.ordinal(), headerRenderer),
              new CellRendererBinding<>(RecentlyPlayedPlaylist.ordinal(), playlistRendererFactory.create(fixedWidth)),
              new CellRendererBinding<>(RecentlyPlayedProfile.ordinal(), profileRendererFactory.create(fixedWidth)),
              new CellRendererBinding<>(RecentlyPlayedStation.ordinal(), stationRendererFactory.create(fixedWidth)));

        headerRenderer.setListener(listener);
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
