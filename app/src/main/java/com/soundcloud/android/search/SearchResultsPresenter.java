package com.soundcloud.android.search;

import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import org.jetbrains.annotations.Nullable;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import javax.inject.Inject;
import javax.inject.Provider;

public class SearchResultsPresenter extends RecyclerViewPresenter<ListItem> {

    static final String EXTRA_QUERY = "query";
    static final String EXTRA_TYPE = "type";

    private final SearchOperations searchOperations;
    private final Provider<ExpandPlayerSubscriber> subscriberProvider;
    private final SearchResultsAdapter adapter;

    private int searchType;
    private String searchQuery;

    @Inject
    SearchResultsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                           SearchOperations searchOperations,
                           Provider<ExpandPlayerSubscriber> subscriberProvider,
                           SearchResultsAdapter adapter) {
        super(swipeRefreshAttacher, Options.list());
        this.searchOperations = searchOperations;
        this.subscriberProvider = subscriberProvider;
        this.adapter = adapter;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    protected CollectionBinding<ListItem> onBuildBinding(Bundle bundle) {
        searchType = bundle.getInt(EXTRA_TYPE);
        searchQuery = bundle.getString(EXTRA_QUERY);
        return createCollectionBinding();
    }

    @Override
    protected CollectionBinding<ListItem> onRefreshBinding() {
        return createCollectionBinding();
    }

    private CollectionBinding<ListItem> createCollectionBinding() {
        return CollectionBinding
                .from(searchOperations.searchResultList(searchQuery, searchType))
                .withAdapter(adapter)
                .withPager(searchOperations.pagingFunction())
                .build();
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }
}
