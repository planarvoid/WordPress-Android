package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.android.collection.playhistory.PlayHistoryItem.Kind.PlayHistoryHeader;
import static com.soundcloud.android.collection.playhistory.PlayHistoryItem.Kind.PlayHistoryTrack;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.adapters.PlayingTrackAware;

import android.support.v7.widget.RecyclerView;
import android.view.View;

@AutoFactory(allowSubclasses = true)
class PlayHistoryAdapter extends PagingRecyclerItemAdapter<PlayHistoryItem, PlayHistoryAdapter.PlayHistoryViewHolder>
        implements PlayingTrackAware {

    interface PlayHistoryClickListener {
        void onClearHistoryClicked();
    }

    PlayHistoryAdapter(PlayHistoryAdapter.PlayHistoryClickListener listener,
                              @Provided PlayHistoryTrackRenderer trackRenderer,
                              @Provided PlayHistoryHeaderRendererFactory headerRendererFactory) {
        super(new CellRendererBinding<>(PlayHistoryTrack.ordinal(), trackRenderer),
              new CellRendererBinding<>(PlayHistoryHeader.ordinal(), headerRendererFactory.create(listener)));
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
}
