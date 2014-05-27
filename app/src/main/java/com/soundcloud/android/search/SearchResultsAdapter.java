package com.soundcloud.android.search;

import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.User;
import com.soundcloud.android.view.adapters.CellPresenter;
import com.soundcloud.android.view.adapters.LegacyPlayableRowPresenter;
import com.soundcloud.android.view.adapters.LegacyUserRowPresenter;
import com.soundcloud.android.view.adapters.PagingItemAdapter;

class SearchResultsAdapter extends PagingItemAdapter<ScResource> {

    SearchResultsAdapter(CellPresenter<ScResource>... cellPresenters) {
        super(cellPresenters);
    }

    @Override
    public int getItemViewType(int position) {
        int itemViewType = super.getItemViewType(position);
        if (itemViewType == IGNORE_ITEM_VIEW_TYPE) {
            return itemViewType;
        } else {
            return getItem(position) instanceof User ?
                    LegacyUserRowPresenter.TYPE_USER :
                    LegacyPlayableRowPresenter.TYPE_PLAYABLE;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

}
