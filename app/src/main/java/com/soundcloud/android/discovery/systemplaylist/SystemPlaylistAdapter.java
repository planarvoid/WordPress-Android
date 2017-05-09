package com.soundcloud.android.discovery.systemplaylist;


import static com.soundcloud.android.discovery.systemplaylist.SystemPlaylistItem.Kind.HEADER;
import static com.soundcloud.android.discovery.systemplaylist.SystemPlaylistItem.Kind.TRACK;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.view.adapters.PlayingTrackAware;

import android.support.v7.widget.RecyclerView;
import android.view.View;

@AutoFactory(allowSubclasses = true)
class SystemPlaylistAdapter extends RecyclerItemAdapter<SystemPlaylistItem, RecyclerView.ViewHolder> implements PlayingTrackAware {

    SystemPlaylistAdapter(SystemPlaylistHeaderRenderer.Listener headerItemListener,
                          TrackItemRenderer.Listener trackItemListener,
                          @Provided SystemPlaylistHeaderRendererFactory headerRendererFactory,
                          @Provided SystemPlaylistTrackRendererFactory trackRendererFactory) {
        super(new CellRendererBinding<>(HEADER.ordinal(), headerRendererFactory.create(headerItemListener)),
              new CellRendererBinding<>(TRACK.ordinal(), trackRendererFactory.create(trackItemListener))
        );
    }

    @Override
    public int getBasicItemViewType(int position) {
        return getItem(position).kind().ordinal();
    }

    @Override
    public void updateNowPlaying(Urn currentlyPlayingUrn) {
        for (int i = 0; i < items.size(); i++) {
            SystemPlaylistItem systemPlaylistItem = items.get(i);
            if (systemPlaylistItem.isTrack()) {
                final TrackItem track = ((SystemPlaylistItem.Track) systemPlaylistItem).track();
                final boolean isPlaying = track.getUrn().equals(currentlyPlayingUrn);
                final TrackItem trackItem = track.withPlayingState(isPlaying);
                items.set(i, ((SystemPlaylistItem.Track) systemPlaylistItem).withTrackItem(trackItem));
            }
        }

        notifyDataSetChanged();
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }
}
