package com.soundcloud.android.collection.recentlyplayed;

import static com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedItem.Kind.RecentlyPlayedEmpty;
import static com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedItem.Kind.RecentlyPlayedHeader;
import static com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedItem.Kind.RecentlyPlayedPlaylist;
import static com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedItem.Kind.RecentlyPlayedProfile;
import static com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedItem.Kind.RecentlyPlayedStation;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.collection.SimpleHeaderRenderer;
import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineProperties;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.List;

@AutoFactory(allowSubclasses = true)
class RecentlyPlayedAdapter extends PagingRecyclerItemAdapter<RecentlyPlayedItem, RecyclerView.ViewHolder> {

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
    protected ViewHolder createViewHolder(View view) {
        return new ViewHolder(view);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return getItem(position).getKind().ordinal();
    }

    void updateOfflineState(OfflineContentChangedEvent event) {
        final List<RecentlyPlayedItem> items = getItems();
        boolean changed = false;
        for (int i = 0, itemsSize = items.size(); i < itemsSize; i++) {

            final RecentlyPlayedItem recentlyPlayedItem = items.get(i);
            if (recentlyPlayedItem.getKind().equals(RecentlyPlayedItem.Kind.RecentlyPlayedPlaylist)) {
                final RecentlyPlayedPlayableItem playableItem = (RecentlyPlayedPlayableItem) recentlyPlayedItem;

                if (event.entities.contains(playableItem.getUrn())) {
                    if (!playableItem.getOfflineState().or(OfflineState.NOT_OFFLINE).equals(event.state)) {
                        playableItem.setOfflineState(event.state);
                        changed = true;
                    }
                }
            }
        }
        if (changed) {
            notifyDataSetChanged();
        }
    }

    void updateOfflineState(OfflineProperties states) {
        final List<RecentlyPlayedItem> items = getItems();
        for (int i = 0, itemsSize = items.size(); i < itemsSize; i++) {

            final RecentlyPlayedItem recentlyPlayedItem = items.get(i);
            if (recentlyPlayedItem.getKind().equals(RecentlyPlayedItem.Kind.RecentlyPlayedPlaylist)) {
                final RecentlyPlayedPlayableItem playableItem = (RecentlyPlayedPlayableItem) recentlyPlayedItem;
                final OfflineState offlineState = states.state(playableItem.getUrn());
                playableItem.setOfflineState(offlineState);
            }
        }
        notifyDataSetChanged();
    }

}
