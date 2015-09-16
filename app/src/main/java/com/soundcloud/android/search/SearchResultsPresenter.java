package com.soundcloud.android.search;

import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.java.collections.PropertySet;
import org.jetbrains.annotations.Nullable;
import rx.functions.Func1;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

public class SearchResultsPresenter extends RecyclerViewPresenter<ListItem> {

    static final String EXTRA_QUERY = "query";
    static final String EXTRA_TYPE = "type";

    private static final Func1<SearchResult, List<ListItem>> TO_SEARCH_ITEM_LIST = new Func1<SearchResult, List<ListItem>>() {
        @Override
        public List<ListItem> call(SearchResult searchResult) {
            final List<PropertySet> sourceSets = searchResult.getItems();
            final List<ListItem> items = new ArrayList<>(sourceSets.size());
            for (PropertySet source : sourceSets) {
                final Urn urn = source.get(EntityProperty.URN);
                if (urn.isTrack()) {
                    items.add(TrackItem.from(source));
                } else if (urn.isPlaylist()) {
                    items.add(PlaylistItem.from(source));
                } else if (urn.isUser()) {
                    items.add(UserItem.from(source));
                }
            }
            return items;
        }
    };

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
                .from(searchOperations.searchResult(searchQuery, searchType), TO_SEARCH_ITEM_LIST)
                .withAdapter(adapter)
                .withPager(searchOperations.pagingFunction(searchType))
                .build();
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }
}
