package com.soundcloud.android.playlists;


import com.soundcloud.android.view.adapters.CellPresenter;
import com.soundcloud.android.view.adapters.ItemAdapter;
import com.soundcloud.propeller.PropertySet;

import javax.inject.Inject;

class InlinePlaylistTracksAdapter extends ItemAdapter<PropertySet> {

    @Inject
    InlinePlaylistTracksAdapter(CellPresenter<PropertySet> trackPresenter, EmptyPlaylistTracksPresenter emptyViewPresenter) {
        super(new CellPresenterEntity<PropertySet>(DEFAULT_VIEW_TYPE, trackPresenter),
                new CellPresenterEntity<PropertySet>(IGNORE_ITEM_VIEW_TYPE, emptyViewPresenter));
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
