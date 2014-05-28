package com.soundcloud.android.stream;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PropertySet;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import com.soundcloud.android.view.adapters.PlaylistItemPresenter;
import com.soundcloud.android.view.adapters.TrackItemPresenter;

import javax.inject.Inject;

class SoundStreamAdapter extends PagingItemAdapter<PropertySet> {

    static final int TRACK_ITEM_TYPE = 0;
    static final int PLAYLIST_ITEM_TYPE = 1;

    @Inject
    SoundStreamAdapter(TrackItemPresenter trackPresenter, PlaylistItemPresenter playlistPresenter) {
        super(new CellPresenterEntity<PropertySet>(TRACK_ITEM_TYPE, trackPresenter),
                new CellPresenterEntity<PropertySet>(PLAYLIST_ITEM_TYPE, playlistPresenter));
    }

    @Override
    public int getItemViewType(int position) {
        final int itemViewType = super.getItemViewType(position);
        if (itemViewType == IGNORE_ITEM_VIEW_TYPE) {
            return itemViewType;
        } else if (getItem(position).get(PlayableProperty.URN) instanceof TrackUrn) {
            return TRACK_ITEM_TYPE;
        } else {
            return PLAYLIST_ITEM_TYPE;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }
}
