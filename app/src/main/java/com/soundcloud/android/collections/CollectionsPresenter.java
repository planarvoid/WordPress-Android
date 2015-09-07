package com.soundcloud.android.collections;

import com.soundcloud.android.R;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import rx.functions.Func1;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class CollectionsPresenter extends RecyclerViewPresenter<CollectionsItem> {

    public static final Func1<MyCollections, Iterable<CollectionsItem>> TO_COLLECTIONS_ITEMS =
            new Func1<MyCollections, Iterable<CollectionsItem>>() {
                @Override
                public List<CollectionsItem> call(MyCollections myCollections) {
                    List<PlaylistItem> playlistItems = myCollections.getPlaylistItems();
                    List<CollectionsItem> collectionsItems = new ArrayList<>(playlistItems.size() + 2);

                    // prepend likes row + playlist header
                    collectionsItems.add(CollectionsItem.fromLikes(myCollections.getLikesCount()));
                    collectionsItems.add(CollectionsItem.fromPlaylistHeader());

                    for (PlaylistItem playlistItem : playlistItems) {
                        collectionsItems.add(CollectionsItem.fromPlaylistItem(playlistItem));
                    }
                    return collectionsItems;
                }
            };

    private final CollectionsOperations collectionsOperations;
    private final CollectionsAdapter adapter;
    private final Resources resources;

    @Inject
    CollectionsPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                         CollectionsOperations collectionsOperations,
                         CollectionsAdapter adapter,
                         Resources resources) {
        super(swipeRefreshAttacher, Options.cards());
        this.collectionsOperations = collectionsOperations;
        this.adapter = adapter;
        this.resources = resources;
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        RecyclerView recyclerView = getRecyclerView();
        final int spanCount = resources.getInteger(R.integer.stations_grid_span_count);
        final GridLayoutManager layoutManager = new GridLayoutManager(view.getContext(), spanCount);
        layoutManager.setSpanSizeLookup(createSpanSizeLookup(spanCount));
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setBackgroundColor(view.getResources().getColor(R.color.collections_home_background));
    }

    @NonNull
    private GridLayoutManager.SpanSizeLookup createSpanSizeLookup(final int spanCount) {
        return new GridLayoutManager.SpanSizeLookup(){
            @Override
            public int getSpanSize(int position) {
                return adapter.getItem(position).isPlaylistItem() ? 1 : spanCount;
            }
        };
    }

    @Override
    protected CollectionBinding<CollectionsItem> onBuildBinding(Bundle bundle) {
        return CollectionBinding.from(collectionsOperations.collections(), TO_COLLECTIONS_ITEMS)
                .withAdapter(adapter)
                .build();
    }

    @Override
    protected CollectionBinding<CollectionsItem> onRefreshBinding() {
        return CollectionBinding.from(collectionsOperations.updatedCollections(), TO_COLLECTIONS_ITEMS)
                .withAdapter(adapter)
                .build();
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }
}
