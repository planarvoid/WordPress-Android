package com.soundcloud.android.playlists;


import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.ListItemAdapter;
import com.soundcloud.android.rx.observers.EmptyViewAware;
import com.soundcloud.android.tracks.PlaylistTrackItemRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.EmptyView;

import javax.inject.Inject;

class InlinePlaylistTracksAdapter extends ListItemAdapter<TrackItem> implements EmptyViewAware {

    private final EmptyViewAware emptyViewRenderer;
    private final PlaylistTrackItemRenderer playlistItemRenderer;

    @Inject
    InlinePlaylistTracksAdapter(PlaylistTrackItemRenderer playlistItemRenderer,
                                EmptyPlaylistTracksRenderer emptyViewRenderer) {
        super(new CellRendererBinding<>(DEFAULT_VIEW_TYPE, playlistItemRenderer),
                new CellRendererBinding<>(IGNORE_ITEM_VIEW_TYPE, emptyViewRenderer));
        this.emptyViewRenderer = emptyViewRenderer;
        this.playlistItemRenderer = playlistItemRenderer;
    }

    @Override
    public int getItemViewType(int position) {
        return items.isEmpty() ? IGNORE_ITEM_VIEW_TYPE : super.getItemViewType(position);
    }

    protected boolean hasContentItems() {
        return !items.isEmpty();
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemCount() {
        return Math.max(1, items.size()); // at least 1 for the empty view
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) != IGNORE_ITEM_VIEW_TYPE;
    }

    @Override
    public void setEmptyViewStatus(EmptyView.Status status) {
        emptyViewRenderer.setEmptyViewStatus(status);
    }

    PlaylistTrackItemRenderer getPlaylistItemRenderer() {
        return playlistItemRenderer;
    }
}
