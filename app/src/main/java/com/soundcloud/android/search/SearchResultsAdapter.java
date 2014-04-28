package com.soundcloud.android.search;

import com.soundcloud.android.Consts;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.EndlessPagingAdapter;
import com.soundcloud.android.collections.ListRow;
import com.soundcloud.android.collections.views.PlayableRow;
import com.soundcloud.android.collections.views.UserlistRow;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.User;

import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class SearchResultsAdapter extends EndlessPagingAdapter<ScResource> {

    public static final int TYPE_PLAYABLE = 0;
    public static final int TYPE_USER = 1;

    private final ImageOperations imageOperations;

    @Inject
    public SearchResultsAdapter(ImageOperations imageOperations) {
        super(Consts.LIST_PAGE_SIZE);
        this.imageOperations = imageOperations;
    }

    @Override
    protected View createItemView(int position, ViewGroup parent) {
        int type = getItemViewType(position);
        switch (type) {
            case TYPE_PLAYABLE:
                return new PlayableRow(parent.getContext(), imageOperations);
            case TYPE_USER:
                return new UserlistRow(parent.getContext(), Screen.SEARCH_EVERYTHING, imageOperations);
            default:
                throw new IllegalArgumentException("no view for playlists yet");
        }
    }

    @Override
    protected void bindItemView(int position, View itemView) {
        ((ListRow) itemView).display(position, getItem(position));
    }

    @Override
    public int getItemViewType(int position) {
        int itemViewType = super.getItemViewType(position);
        if (itemViewType == IGNORE_ITEM_VIEW_TYPE){
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
