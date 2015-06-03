package com.soundcloud.android.view.adapters;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingItemAdapter;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.tracks.TrackItemRenderer;

import javax.inject.Inject;

public class MixedPlayableAdapter extends PagingItemAdapter<PlayableItem> {

    @VisibleForTesting static final int TRACK_ITEM_TYPE = 0;
    @VisibleForTesting static final int PLAYLIST_ITEM_TYPE = 1;

    private final TrackItemRenderer trackRenderer;

    @Inject
    public MixedPlayableAdapter(TrackItemRenderer trackRenderer, PlaylistItemRenderer playlistRenderer) {
        super(new CellRendererBinding<>(TRACK_ITEM_TYPE, trackRenderer),
                new CellRendererBinding<>(PLAYLIST_ITEM_TYPE, playlistRenderer));
        this.trackRenderer = trackRenderer;
    }

    @Override
    public int getItemViewType(int position) {
        final int itemViewType = super.getItemViewType(position);
        if (itemViewType == IGNORE_ITEM_VIEW_TYPE) {
            return itemViewType;
        } else if (getItem(position).getEntityUrn().isTrack()) {
            return TRACK_ITEM_TYPE;
        } else {
            return PLAYLIST_ITEM_TYPE;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    public TrackItemRenderer getTrackRenderer() {
        return trackRenderer;
    }

}
