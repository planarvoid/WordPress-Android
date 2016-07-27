package com.soundcloud.android.playlists;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.ListItemAdapter;
import com.soundcloud.android.presentation.TypedListItem;
import com.soundcloud.android.tracks.PlaylistTrackItemRenderer;
import com.soundcloud.android.upsell.PlaylistUpsellItemRenderer;
import com.soundcloud.android.upsell.UpsellListItem;
import com.soundcloud.android.view.EmptyView;

import javax.inject.Inject;

class InlinePlaylistTracksAdapter extends ListItemAdapter<TypedListItem> implements EmptyViewAware {

    private static final int TRACK_ITEM_TYPE = 0;
    private static final int EMPTY_ITEM_TYPE = 1;
    private static final int UPSELL_ITEM_TYPE = 2;

    private final EmptyPlaylistTracksRenderer emptyViewRenderer;
    private final PlaylistTrackItemRenderer playlistItemRenderer;
    private final PlaylistUpsellItemRenderer upsellItemRenderer;

    @Inject
    InlinePlaylistTracksAdapter(PlaylistTrackItemRenderer playlistItemRenderer,
                                PlaylistUpsellItemRenderer playlistUpsellItemRenderer,
                                EmptyPlaylistTracksRenderer emptyViewRenderer) {
        super(new CellRendererBinding<>(DEFAULT_VIEW_TYPE, playlistItemRenderer),
              new CellRendererBinding<>(UPSELL_ITEM_TYPE, playlistUpsellItemRenderer),
              new CellRendererBinding<>(EMPTY_ITEM_TYPE, emptyViewRenderer));
        this.emptyViewRenderer = emptyViewRenderer;
        this.playlistItemRenderer = playlistItemRenderer;
        this.upsellItemRenderer = playlistUpsellItemRenderer;
    }

    @Override
    public int getItemViewType(int position) {
        if (items.isEmpty()) {
            return EMPTY_ITEM_TYPE;
        } else if (urnAt(position).isTrack()) {
            return TRACK_ITEM_TYPE;
        } else if (urnAt(position).equals(UpsellListItem.PLAYLIST_UPSELL_URN)) {
            return UPSELL_ITEM_TYPE;
        } else {
            throw new IllegalStateException("Unexpected item type in playlist adapter");
        }
    }

    private Urn urnAt(int position) {
        return getItem(position).getUrn();
    }

    public void setEmptyStateMessage(String title, String description) {
        emptyViewRenderer.setEmptyStateMessage(title, description);
    }

    protected boolean hasContentItems() {
        return !items.isEmpty();
    }

    @Override
    public int getViewTypeCount() {
        return 3;
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

    PlaylistUpsellItemRenderer getUpsellItemRenderer() {
        return upsellItemRenderer;
    }
}
