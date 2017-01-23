package com.soundcloud.android.collection.recentlyplayed;

import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineProperties;
import com.soundcloud.android.presentation.CellRenderer;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.lang.ref.WeakReference;
import java.util.List;

public class RecentlyPlayedBucketRenderer implements CellRenderer<RecentlyPlayedBucketItem> {

    private final RecentlyPlayedAdapter adapter;
    private final Navigator navigator;

    private WeakReference<RecyclerView> recyclerViewRef;

    @Inject
    RecentlyPlayedBucketRenderer(RecentlyPlayedAdapterFactory recentlyPlayedAdapterFactory,
                                 Navigator navigator) {
        this.adapter = recentlyPlayedAdapterFactory.create(true, null);
        this.navigator = navigator;
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        final View view = LayoutInflater.from(viewGroup.getContext())
                                        .inflate(R.layout.recently_played_bucket, viewGroup, false);
        ButterKnife.bind(this, view);
        RecyclerView recyclerView = ButterKnife.findById(view, R.id.recently_played_carousel);
        initCarousel(recyclerView);
        recyclerViewRef = new WeakReference<>(recyclerView);
        return view;
    }

    public void update(OfflineContentChangedEvent event) {
        adapter.updateOfflineState(event);
    }

    public void update(OfflineProperties states) {
        adapter.updateOfflineState(states);
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

    private void initCarousel(RecyclerView recyclerView) {
        final Context context = recyclerView.getContext();

        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void bindItemView(int position, View bucketView, List<RecentlyPlayedBucketItem> list) {
        bindCarousel(list.get(position));
    }

    private void bindCarousel(RecentlyPlayedBucketItem recentlyPlayedBucket) {
        final List<RecentlyPlayedPlayableItem> recentlyPlayedPlayableItems = recentlyPlayedBucket.getRecentlyPlayedPlayableItems();

        adapter.clear();

        if (recentlyPlayedPlayableItems.isEmpty()) {
            adapter.addItem(RecentlyPlayedEmpty.create());
        } else {
            for (RecentlyPlayedPlayableItem recentlyPlayedPlayableItem : recentlyPlayedPlayableItems) {
                adapter.addItem(recentlyPlayedPlayableItem);
            }
        }
        adapter.notifyDataSetChanged();
    }

    @OnClick(R.id.recently_played_view_all)
    void onViewAllClicked(View v) {
        navigator.openRecentlyPlayed(v.getContext());
    }
}
