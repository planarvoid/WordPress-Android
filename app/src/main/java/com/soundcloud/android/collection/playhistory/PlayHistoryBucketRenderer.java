package com.soundcloud.android.collection.playhistory;

import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.presentation.DividerItemDecoration;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class PlayHistoryBucketRenderer implements CellRenderer<PlayHistoryBucketItem> {

    private final PlayHistoryAdapter adapter;
    private final NavigationExecutor navigationExecutor;
    private final PerformanceMetricsEngine performanceMetricsEngine;

    private WeakReference<RecyclerView> recyclerViewRef;

    @Inject
    PlayHistoryBucketRenderer(PlayHistoryAdapter adapter, NavigationExecutor navigationExecutor, PerformanceMetricsEngine performanceMetricsEngine) {
        this.adapter = adapter;
        this.navigationExecutor = navigationExecutor;
        this.performanceMetricsEngine = performanceMetricsEngine;
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        final View view = LayoutInflater.from(viewGroup.getContext())
                                        .inflate(R.layout.play_history_bucket, viewGroup, false);

        ButterKnife.bind(this, view);
        RecyclerView recyclerView = ButterKnife.findById(view, R.id.play_history);
        initList(recyclerView);
        recyclerViewRef = new WeakReference<>(recyclerView);
        return view;
    }

    public void setTrackClickListener(TrackItemRenderer.Listener listener) {
        adapter.setTrackClickListener(listener);
    }

    public void detach() {
        adapter.clear();
        if (recyclerViewRef != null) {
            RecyclerView recyclerView = recyclerViewRef.get();

            if (recyclerView != null) {
                recyclerView.setAdapter(null);
                recyclerView.setLayoutManager(null);
            }
            recyclerViewRef = null;
        }
    }

    private void initList(RecyclerView recyclerView) {
        final Context context = recyclerView.getContext();

        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
        addListDividers(recyclerView);
    }

    private void addListDividers(RecyclerView recyclerView) {
        final Resources resources = recyclerView.getResources();
        final Drawable divider = resources.getDrawable(com.soundcloud.androidkit.R.drawable.ak_list_divider_item);
        int dividerHeight = resources.getDimensionPixelSize(com.soundcloud.androidkit.R.dimen.ak_list_divider_horizontal_height);
        recyclerView.addItemDecoration(new DividerItemDecoration(divider, dividerHeight));
    }

    @Override
    public void bindItemView(int position, View bucketView, List<PlayHistoryBucketItem> list) {
        final List<TrackItem> listeningHistory = list.get(position).getListeningHistory();

        List<PlayHistoryItem> items = new ArrayList<>();

        if (listeningHistory.isEmpty()) {
            items.add(PlayHistoryItemEmpty.create());
        } else {
            for (TrackItem trackItem : listeningHistory) {
                items.add(PlayHistoryItemTrack.create(trackItem));
            }
        }

        adapter.replaceItems(items);
    }

    @OnClick(R.id.play_history_view_all)
    void onViewAllClicked(View v) {
        performanceMetricsEngine.startMeasuring(MetricType.LISTENING_HISTORY_LOAD);
        navigationExecutor.openPlayHistory(v.getContext());
    }

}
