package com.soundcloud.android.collection.playhistory;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItem;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

public class PlayHistoryBucketRenderer implements CellRenderer<PlayHistoryBucketItem> {

    private final PlayHistoryAdapter adapter;
    private final Navigator navigator;

    @Bind(R.id.play_history) RecyclerView recyclerView;

    @Inject
    PlayHistoryBucketRenderer(PlayHistoryAdapterFactory adapterFactory, Navigator navigator) {
        this.adapter = adapterFactory.create(null);
        this.navigator = navigator;
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        final View view = LayoutInflater.from(viewGroup.getContext())
                                        .inflate(R.layout.play_history_bucket, viewGroup, false);

        ButterKnife.bind(this, view);
        initList();
        return view;
    }

    private void initList() {
        final Context context = recyclerView.getContext();

        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void bindItemView(int position, View bucketView, List<PlayHistoryBucketItem> list) {
        final List<TrackItem> listeningHistory = list.get(position).getListeningHistory();

        adapter.clear();

        for (TrackItem trackItem : listeningHistory) {
            adapter.addItem(PlayHistoryItemTrack.create(trackItem));
        }

        adapter.notifyDataSetChanged();
    }

    @OnClick(R.id.play_history_view_all)
    public void onViewAllClicked(View v) {
        navigator.openPlayHistory(v.getContext());
    }

}
