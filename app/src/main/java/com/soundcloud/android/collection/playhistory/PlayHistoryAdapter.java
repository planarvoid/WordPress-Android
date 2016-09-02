package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.android.collection.playhistory.PlayHistoryItem.Kind.PlayHistoryEmpty;
import static com.soundcloud.android.collection.playhistory.PlayHistoryItem.Kind.PlayHistoryHeader;
import static com.soundcloud.android.collection.playhistory.PlayHistoryItem.Kind.PlayHistoryTrack;

import com.soundcloud.android.collection.SimpleHeaderRenderer;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.view.adapters.PlayingTrackAware;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

class PlayHistoryAdapter extends PagingRecyclerItemAdapter<PlayHistoryItem, PlayHistoryAdapter.PlayHistoryViewHolder>
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
                    track.setIsPlaying(isCurrent);
                    notifyItemChanged(i);
                }
            }
        }
    }

    @Override
    protected PlayHistoryViewHolder createViewHolder(View itemView) {
        return new PlayHistoryViewHolder(itemView);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return getItem(position).getKind().ordinal();
    }

    static class PlayHistoryViewHolder extends RecyclerView.ViewHolder {
        PlayHistoryViewHolder(View itemView) {
            super(itemView);
        }
    }

    public void setMenuClickListener(SimpleHeaderRenderer.Listener listener) {
        this.headerRenderer.setListener(listener);
    }

    public void setTrackClickListener(TrackItemRenderer.Listener listener) {
        this.trackRenderer.setListener(listener);
    }
}
