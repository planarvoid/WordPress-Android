package com.soundcloud.android.collection.recentlyplayed;

import com.soundcloud.android.R;
import com.soundcloud.android.collection.CollectionItem;
import com.soundcloud.android.collection.PlayHistoryOperations;
import com.soundcloud.android.collection.RecentlyPlayedCollectionItem;
import com.soundcloud.android.collection.RecentlyPlayedItem;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import org.jetbrains.annotations.Nullable;
import rx.functions.Func1;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

class RecentlyPlayedPresenter extends RecyclerViewPresenter<List<CollectionItem>, CollectionItem> {

    private static final Func1<RecentlyPlayedItem, CollectionItem> TO_COLLECTION_ITEM = new Func1<RecentlyPlayedItem, CollectionItem>() {
        @Override
        public CollectionItem call(RecentlyPlayedItem recentlyPlayedItem) {
            return RecentlyPlayedCollectionItem.create(recentlyPlayedItem);
        }
    };

    private final RecentlyPlayedAdapter adapter;
    private final Resources resources;
    private final PlayHistoryOperations playHistoryOperations;

    @Inject
    public RecentlyPlayedPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                                   RecentlyPlayedAdapter adapter,
                                   Resources resources,
                                   PlayHistoryOperations playHistoryOperations) {
        super(swipeRefreshAttacher, new Options.Builder().useDividers(Options.DividerMode.NONE).build());
        this.adapter = adapter;
        this.resources = resources;
        this.playHistoryOperations = playHistoryOperations;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    protected CollectionBinding<List<CollectionItem>, CollectionItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding
                .from(playHistoryOperations
                              .recentlyPlayed()
                              .map(TO_COLLECTION_ITEM)
                              .toList())
                .withAdapter(adapter)
                .build();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        final int spanCount = resources.getInteger(R.integer.collection_grid_span_count);
        final GridLayoutManager layoutManager = new GridLayoutManager(view.getContext(), spanCount);
        getRecyclerView().setLayoutManager(layoutManager);
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

}
