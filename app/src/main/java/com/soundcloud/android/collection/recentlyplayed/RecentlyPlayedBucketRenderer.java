package com.soundcloud.android.collection.recentlyplayed;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.collection.RecentlyPlayedCollectionItem;
import com.soundcloud.android.collection.RecentlyPlayedItem;
import com.soundcloud.android.presentation.CellRenderer;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

public class RecentlyPlayedBucketRenderer implements CellRenderer<RecentlyPlayedBucketCollectionItem> {

    private final RecentlyPlayedAdapter adapter;

    @Inject
    RecentlyPlayedBucketRenderer(RecentlyPlayedAdapterFactory recentlyPlayedAdapterFactory) {
        this.adapter = recentlyPlayedAdapterFactory.create(true);
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        final View view = LayoutInflater.from(viewGroup.getContext())
                                        .inflate(R.layout.recently_played_bucket, viewGroup, false);
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
    public void bindItemView(int position, View bucketView, List<RecentlyPlayedBucketCollectionItem> list) {
        bindCarousel(adapter, list.get(position));
    }

    private void bindCarousel(RecentlyPlayedAdapter adapter, RecentlyPlayedBucketCollectionItem recentlyPlayedBucket) {
        final List<RecentlyPlayedItem> recentlyPlayedItems = recentlyPlayedBucket.getRecentlyPlayedItems();

        adapter.clear();

        for (RecentlyPlayedItem recentlyPlayedItem : recentlyPlayedItems) {
            adapter.addItem(RecentlyPlayedCollectionItem.create(recentlyPlayedItem));
        }
        adapter.notifyDataSetChanged();
    }

}
