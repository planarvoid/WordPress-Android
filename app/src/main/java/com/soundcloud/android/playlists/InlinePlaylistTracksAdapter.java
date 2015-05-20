package com.soundcloud.android.playlists;


import com.soundcloud.android.rx.observers.EmptyViewAware;
import com.soundcloud.android.tracks.PlaylistTrackItemPresenter;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.adapters.CellPresenterBinding;
import com.soundcloud.android.view.adapters.ItemAdapter;

import javax.inject.Inject;

class InlinePlaylistTracksAdapter extends ItemAdapter<TrackItem> implements EmptyViewAware {

    private final EmptyViewAware emptyViewPresenter;
    private final PlaylistTrackItemPresenter playlistItemPresenter;

    @Inject
    InlinePlaylistTracksAdapter(PlaylistTrackItemPresenter playlistItemPresenter,
                                EmptyPlaylistTracksPresenter emptyViewPresenter) {
        super(new CellPresenterBinding<>(DEFAULT_VIEW_TYPE, playlistItemPresenter),
                new CellPresenterBinding<>(IGNORE_ITEM_VIEW_TYPE, emptyViewPresenter));
        this.emptyViewPresenter = emptyViewPresenter;
        this.playlistItemPresenter = playlistItemPresenter;
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
    public void setEmptyViewStatus(int status) {
        emptyViewPresenter.setEmptyViewStatus(status);
    }

    PlaylistTrackItemPresenter getPlaylistItemPresenter() {
        return playlistItemPresenter;
    }
}
