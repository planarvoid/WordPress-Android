package com.soundcloud.android.playlists;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.tracks.PlaylistTrackItemRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.adapters.NowPlayingAdapter;
import com.soundcloud.android.view.dragdrop.OnStartDragListener;
import com.soundcloud.android.view.dragdrop.RecyclerDragDropAdapter;

import android.view.View;

import javax.inject.Inject;

@AutoFactory(allowSubclasses = true)
public class PlaylistAdapter
        extends RecyclerDragDropAdapter<TrackItem, RecyclerDragDropAdapter.ViewHolder>
        implements NowPlayingAdapter {

    private static final int TRACK_ITEM_TYPE = 0;
    private static final int EDIT_TRACK_ITEM_TYPE = 1;
    private boolean isEditMode;

    @Inject
    public PlaylistAdapter(OnStartDragListener dragListener,
                           @Provided PlaylistTrackItemRenderer trackItemRenderer,
                           @Provided TrackEditItemRenderer editTrackItemRenderer) {
        super(dragListener,
                new CellRendererBinding<>(TRACK_ITEM_TYPE, trackItemRenderer),
                new CellRendererBinding<>(EDIT_TRACK_ITEM_TYPE, editTrackItemRenderer));
        isEditMode = false;
    }

    @Override
    public int getBasicItemViewType(int position) {
        return isEditMode ? EDIT_TRACK_ITEM_TYPE : TRACK_ITEM_TYPE;
    }

    @Override
    public void updateNowPlaying(Urn currentlyPlayingUrn) {
        for (TrackItem item : getItems()) {
            item.setIsPlaying(item.getUrn().equals(currentlyPlayingUrn));
        }
        notifyDataSetChanged();
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        notifyDataSetChanged();
    }

}
