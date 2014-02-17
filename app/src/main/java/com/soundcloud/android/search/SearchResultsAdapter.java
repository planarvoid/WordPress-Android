package com.soundcloud.android.search;

import com.soundcloud.android.Consts;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.ItemAdapter;
import com.soundcloud.android.collections.ListRow;
import com.soundcloud.android.collections.views.PlayableRow;
import com.soundcloud.android.collections.views.UserlistRow;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SearchResultsCollection;
import com.soundcloud.android.model.User;
import rx.Observer;

import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class SearchResultsAdapter extends ItemAdapter<ScResource> implements Observer<SearchResultsCollection> {

    public static final int TYPE_PLAYABLE = 0;
    public static final int TYPE_USER = 1;

    private final ImageOperations mImageOperations;

    @Inject
    public SearchResultsAdapter(ImageOperations imageOperations) {
        super(Consts.COLLECTION_PAGE_SIZE);
        mImageOperations = imageOperations;
    }

    @Override
    public void onCompleted() {}

    @Override
    public void onError(Throwable e) {}

    @Override
    public void onNext(SearchResultsCollection results) {
        for (ScResource item : results) {
            addItem(item);
        }
        notifyDataSetChanged();
    }

    @Override
    protected View createItemView(int position, ViewGroup parent) {
        int type = getItemViewType(position);
        switch (type) {
            case TYPE_PLAYABLE:
                return new PlayableRow(parent.getContext(), mImageOperations);
            case TYPE_USER:
                return new UserlistRow(parent.getContext(), Screen.SEARCH_EVERYTHING, mImageOperations);
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
        return getItem(position) instanceof User ? TYPE_USER : TYPE_PLAYABLE;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

}
