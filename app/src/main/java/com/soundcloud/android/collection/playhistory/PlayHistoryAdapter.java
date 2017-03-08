package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.android.collection.playhistory.PlayHistoryItem.Kind.PlayHistoryEmpty;
import static com.soundcloud.android.collection.playhistory.PlayHistoryItem.Kind.PlayHistoryHeader;
import static com.soundcloud.android.collection.playhistory.PlayHistoryItem.Kind.PlayHistoryTrack;

import com.soundcloud.android.collection.SimpleHeaderRenderer;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.view.adapters.PlayingTrackAware;

import android.view.View;

import javax.inject.Inject;

class PlayHistoryAdapter extends PagingRecyclerItemAdapter<PlayHistoryItem, RecyclerItemAdapter.ViewHolder>
        implements PlayingTrackAware {

    private final PlayHistoryTrackRenderer trackRenderer;
    private final PlayHistoryHeaderRenderer headerRenderer;

    @Inject
    PlayHistoryAdapter(PlayHistoryTrackRenderer trackRenderer,
                       PlayHistoryHeaderRenderer headerRenderer,
                       PlayHistoryEmptyRenderer emptyRenderer) {
        super(new CellRendererBinding<>(PlayHistoryTrack.ordinal(), trackRenderer),
              new CellRendererBinding<>(PlayHistoryHeader.ordinal(), headerRenderer),
              new CellRendererBinding<>(PlayHistoryEmpty.ordinal(), emptyRenderer)
        );
        this.trackRenderer = trackRenderer;
        this.headerRenderer = headerRenderer;
    }

    @Override
    public void updateNowPlaying(Urn currentlyPlayingUrn) {
        for (int i = 0; i < getItemCount(); i++) {
            final PlayHistoryItem item = getItem(i);

            if (PlayHistoryTrack.equals(item.getKind())) {
                final TrackItem track = ((PlayHistoryItemTrack) getItem(i)).trackItem();
                final boolean isCurrent = track.getUrn().equals(currentlyPlayingUrn);

                if (track.isPlaying() || isCurrent) {
                    final TrackItem trackItem = track.withPlayingState(isCurrent);
                    items.set(i, PlayHistoryItemTrack.create(trackItem));
                    notifyItemChanged(i);
                }
            }
        }
    }

    void setItem(int position, PlayHistoryItem item) {
        getItems().set(position, item);
        notifyItemChanged(position);
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return getItem(position).getKind().ordinal();
    }

    public void setMenuClickListener(SimpleHeaderRenderer.Listener listener) {
        this.headerRenderer.setListener(listener);
    }

    public void setTrackClickListener(TrackItemRenderer.Listener listener) {
        this.trackRenderer.setListener(listener);
    }
}
