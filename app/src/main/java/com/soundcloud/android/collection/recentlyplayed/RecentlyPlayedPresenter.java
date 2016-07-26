package com.soundcloud.android.collection.recentlyplayed;

import com.soundcloud.android.R;
import com.soundcloud.android.collection.playhistory.PlayHistoryOperations;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import org.jetbrains.annotations.Nullable;

import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

class RecentlyPlayedPresenter extends RecyclerViewPresenter<List<RecentlyPlayedItem>, RecentlyPlayedItem> {

    private final RecentlyPlayedAdapter adapter;
    private final Resources resources;
    private final PlayHistoryOperations playHistoryOperations;

    @Inject
    public RecentlyPlayedPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                                   RecentlyPlayedAdapterFactory adapterFactory,
                                   Resources resources,
                                   PlayHistoryOperations playHistoryOperations) {
        super(swipeRefreshAttacher, new Options.Builder().useDividers(Options.DividerMode.NONE).build());
        this.adapter = adapterFactory.create(false);
        this.resources = resources;
        this.playHistoryOperations = playHistoryOperations;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    protected CollectionBinding<List<RecentlyPlayedItem>, RecentlyPlayedItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding
                .from(playHistoryOperations
                              .recentlyPlayed()
                              .toList())
                .withAdapter(adapter)
                .build();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        final int spanCount = resources.getInteger(R.integer.collection_grid_span_count);
        final int itemMargin = view.getResources().getDimensionPixelSize(R.dimen.collection_default_margin);
        final GridLayoutManager layoutManager = new GridLayoutManager(view.getContext(), spanCount);
        RecyclerView recyclerView = getRecyclerView();

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new RecentlyPlayedItemDecoration(itemMargin));
        recyclerView.setPadding(itemMargin, 0, 0, 0);
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    private static class RecentlyPlayedItemDecoration extends RecyclerView.ItemDecoration {

        private final int spacing;

        RecentlyPlayedItemDecoration(int spacing) {
            this.spacing = spacing;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.left = 0;
            outRect.right = spacing;
            outRect.bottom = spacing;
            outRect.top = spacing;
        }
    }
}
