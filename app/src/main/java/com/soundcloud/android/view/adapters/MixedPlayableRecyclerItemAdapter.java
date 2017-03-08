package com.soundcloud.android.view.adapters;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;

import android.view.View;

import javax.inject.Inject;

public class MixedPlayableRecyclerItemAdapter
        extends PagingRecyclerItemAdapter<PlayableItem, RecyclerItemAdapter.ViewHolder>
        implements PlayingTrackAware {

    private static final int TRACK_ITEM_TYPE = 0;
    private static final int PLAYLIST_ITEM_TYPE = 1;

    private final TrackItemRenderer trackRenderer;

    @Inject
    public MixedPlayableRecyclerItemAdapter(TrackItemRenderer trackRenderer, PlaylistItemRenderer playlistRenderer) {
        super(new CellRendererBinding<>(TRACK_ITEM_TYPE, trackRenderer),
              new CellRendererBinding<>(PLAYLIST_ITEM_TYPE, playlistRenderer));
        this.trackRenderer = trackRenderer;
    }

    @Override
    public int getBasicItemViewType(int position) {
        if (getItem(position).getUrn().isTrack()) {
            return TRACK_ITEM_TYPE;
        } else {
            return PLAYLIST_ITEM_TYPE;
        }
    }

    public TrackItemRenderer getTrackRenderer() {
        return trackRenderer;
    }

    @Override
    public void updateNowPlaying(Urn currentlyPlayingUrn) {
        for (int i = 0; i < items.size(); i++) {
            final PlayableItem playableItem = items.get(i);
            final Urn urn = playableItem.getUrn();
            if (urn.isTrack()) {
                final boolean isCurrentlyPlayingUrn = urn.equals(currentlyPlayingUrn);
                final TrackItem trackItem = (TrackItem) playableItem;

                if (trackItem.isPlaying() != isCurrentlyPlayingUrn) {
                    final TrackItem updatedItem = trackItem.withPlayingState(isCurrentlyPlayingUrn);
                    items.set(i, updatedItem);
                    notifyItemChanged(i);
                }
            }
        }
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

}
