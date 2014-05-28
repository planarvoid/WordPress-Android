package com.soundcloud.android.search;

import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.User;
import com.soundcloud.android.view.adapters.LegacyPlayableRowPresenter;
import com.soundcloud.android.view.adapters.LegacyUserRowPresenter;
import com.soundcloud.android.view.adapters.PagingItemAdapter;

class SearchResultsAdapter extends PagingItemAdapter<ScResource> {

    static final int TYPE_USER = 0;
    static final int TYPE_PLAYABLE = 1;

    SearchResultsAdapter(LegacyUserRowPresenter userRowPresenter, LegacyPlayableRowPresenter<ScResource> playableRowPresenter) {
        super(new CellPresenterEntity<ScResource>(TYPE_USER, userRowPresenter),
                new CellPresenterEntity<ScResource>(TYPE_PLAYABLE, playableRowPresenter));
    }

    @Override
    public int getItemViewType(int position) {
        int itemViewType = super.getItemViewType(position);
        if (itemViewType == IGNORE_ITEM_VIEW_TYPE) {
            return itemViewType;
        } else {
            return getItem(position) instanceof User ? TYPE_USER : TYPE_PLAYABLE;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

}
