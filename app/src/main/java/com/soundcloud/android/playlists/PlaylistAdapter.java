package com.soundcloud.android.playlists;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.TypedListItem;
import com.soundcloud.android.tracks.PlaylistTrackItemRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.upsell.PlaylistUpsellItemRenderer;
import com.soundcloud.android.upsell.UpsellListItem;
import com.soundcloud.android.view.adapters.PlayingTrackAware;
import com.soundcloud.android.view.dragdrop.OnStartDragListener;
import com.soundcloud.android.view.dragdrop.RecyclerDragDropAdapter;

import android.view.View;

import java.util.ArrayList;
import java.util.List;

@AutoFactory(allowSubclasses = true)
public class PlaylistAdapter
        extends RecyclerDragDropAdapter<TypedListItem, RecyclerDragDropAdapter.ViewHolder>
        implements PlayingTrackAware {

    private static final int TRACK_ITEM_TYPE = 0;
    private static final int EDIT_TRACK_ITEM_TYPE = 1;
    private static final int UPSELL_ITEM_TYPE = 2;

    private boolean isEditMode;

    private final PlaylistUpsellItemRenderer upsellItemRenderer;

    public PlaylistAdapter(OnStartDragListener dragListener,
                           @Provided PlaylistTrackItemRenderer trackItemRenderer,
                           @Provided TrackEditItemRenderer editTrackItemRenderer,
                           @Provided PlaylistUpsellItemRenderer upsellItemRenderer) {
        super(dragListener,
                new CellRendererBinding<>(TRACK_ITEM_TYPE, trackItemRenderer),
                new CellRendererBinding<>(EDIT_TRACK_ITEM_TYPE, editTrackItemRenderer),
                new CellRendererBinding<>(UPSELL_ITEM_TYPE, upsellItemRenderer));
        this.upsellItemRenderer = upsellItemRenderer;
        isEditMode = false;
    }

    @Override
    public int getBasicItemViewType(int position) {
        TypedListItem item = getItem(position);

        if (item.getUrn().isTrack()) {
            return isEditMode ? EDIT_TRACK_ITEM_TYPE : TRACK_ITEM_TYPE;
        } else if (item.getUrn().equals(UpsellListItem.PLAYLIST_UPSELL_URN)) {
            return UPSELL_ITEM_TYPE;
        } else {
            throw new IllegalArgumentException("Unexpected item type: " + item.getUrn());
        }
    }

    @Override
    public void updateNowPlaying(Urn currentlyPlayingUrn) {
        for (TypedListItem item : getItems()) {
            if (item instanceof TrackItem) {
                final TrackItem track = (TrackItem) item;
                track.setIsPlaying(track.getUrn().equals(currentlyPlayingUrn));
            }
        }
        notifyDataSetChanged();
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    public List<TrackItem> getTracks() {
        List<TrackItem> tracks = new ArrayList<>(getItems().size());
        for (TypedListItem item : getItems()) {
            if (item instanceof TrackItem) {
                tracks.add((TrackItem) item);
            }
        }
        return tracks;
    }

    void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        notifyDataSetChanged();
    }

    void setOnUpsellClickListener(PlaylistUpsellItemRenderer.Listener listener) {
        this.upsellItemRenderer.setListener(listener);
    }

}
