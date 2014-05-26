package com.soundcloud.android.search;

import com.soundcloud.android.Consts;
import com.soundcloud.android.collections.views.IconLayout;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.User;
import com.soundcloud.android.view.adapters.PagingItemAdapter;

import javax.inject.Inject;

class SearchResultsAdapter extends PagingItemAdapter<ScResource, IconLayout> {

    @Inject
    public SearchResultsAdapter(SearchResultPresenter presenter) {
        super(presenter, Consts.LIST_PAGE_SIZE);
    }

    @Override
    public int getItemViewType(int position) {
        int itemViewType = super.getItemViewType(position);
        if (itemViewType == IGNORE_ITEM_VIEW_TYPE){
            return itemViewType;
        } else {
            return getItem(position) instanceof User ? SearchResultPresenter.TYPE_USER : SearchResultPresenter.TYPE_PLAYABLE;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

}
