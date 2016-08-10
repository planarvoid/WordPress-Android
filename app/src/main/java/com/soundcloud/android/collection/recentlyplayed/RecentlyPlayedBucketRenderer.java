package com.soundcloud.android.collection.recentlyplayed;

import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

public class RecentlyPlayedBucketRenderer implements CellRenderer<RecentlyPlayedBucketItem> {

    private final RecentlyPlayedAdapter adapter;
    private final Navigator navigator;

    @Inject
    RecentlyPlayedBucketRenderer(RecentlyPlayedAdapterFactory recentlyPlayedAdapterFactory,
                                 Navigator navigator) {
        this.adapter = recentlyPlayedAdapterFactory.create(true);
        this.navigator = navigator;
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        final View view = LayoutInflater.from(viewGroup.getContext())
                                        .inflate(R.layout.recently_played_bucket, viewGroup, false);
        ButterKnife.bind(this, view);
        initCarousel(ButterKnife.<RecyclerView>findById(view, R.id.recently_played_carousel));
        return view;
    }

    private void initCarousel(final RecyclerView recyclerView) {
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
        final List<RecentlyPlayedItem> recentlyPlayedItems = recentlyPlayedBucket.getRecentlyPlayedItems();

        adapter.clear();

        for (RecentlyPlayedItem recentlyPlayedItem : recentlyPlayedItems) {
            adapter.addItem(recentlyPlayedItem);
        }
        adapter.notifyDataSetChanged();
    }

    @OnClick(R.id.recently_played_view_all)
    public void onViewAllClicked(View v) {
        navigator.openRecentlyPlayed(v.getContext());
    }
}