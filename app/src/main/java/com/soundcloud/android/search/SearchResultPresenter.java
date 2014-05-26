package com.soundcloud.android.search;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.ListRow;
import com.soundcloud.android.collections.views.PlayableRow;
import com.soundcloud.android.collections.views.UserlistRow;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.view.adapters.CellPresenter;

import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

/**
 * This class is merely an adapter around our old ListRow types until we've ported search over to api-mobile
 */
class SearchResultPresenter implements CellPresenter<ScResource> {

    static final int TYPE_PLAYABLE = 0;
    static final int TYPE_USER = 1;
    private final ImageOperations imageOperations;

    @Inject
    SearchResultPresenter(ImageOperations imageOperations) {
        this.imageOperations = imageOperations;
    }

    @Override
    public View createItemView(int position, ViewGroup parent, int itemViewType) {
        switch (itemViewType) {
            case TYPE_PLAYABLE:
                return new PlayableRow(parent.getContext(), imageOperations);
            case TYPE_USER:
                return new UserlistRow(parent.getContext(), Screen.SEARCH_EVERYTHING, imageOperations);
            default:
                throw new IllegalArgumentException("no view for playlists yet");
        }
    }

    @Override
    public void bindItemView(int position, View itemView, int itemViewType, List<ScResource> items) {
        ((ListRow) itemView).display(position, items.get(position));
    }
}
