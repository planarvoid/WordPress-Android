package com.soundcloud.android.playlists;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.view.adapters.CellPresenter;
import com.soundcloud.android.view.adapters.ItemAdapter;

import javax.inject.Inject;

class InlinePlaylistTracksAdapter extends ItemAdapter<Track> {

    @Inject
    InlinePlaylistTracksAdapter(CellPresenter<Track> trackPresenter, EmptyPlaylistTracksPresenter emptyViewPresenter) {
        super(new CellPresenterEntity<Track>(DEFAULT_VIEW_TYPE, trackPresenter),
                new CellPresenterEntity<Track>(IGNORE_ITEM_VIEW_TYPE, emptyViewPresenter));
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
    public int getCount() {
        return Math.max(1, items.size()); // at least 1 for the empty view
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) != IGNORE_ITEM_VIEW_TYPE;
    }

}
