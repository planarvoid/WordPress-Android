package com.soundcloud.android.collection.playhistory;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
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
import java.util.List;

public class PlayHistoryBucketRenderer implements CellRenderer<PlayHistoryBucketItem> {

    private final PlayHistoryAdapter adapter;
    private final Navigator navigator;

    @BindView(R.id.play_history) RecyclerView recyclerView;

    @Inject
    PlayHistoryBucketRenderer(PlayHistoryAdapter adapter, Navigator navigator) {
        this.adapter = adapter;
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

    public void setTrackClickListener(TrackItemRenderer.Listener listener) {
        adapter.setTrackClickListener(listener);
    }

    private void initList() {
        final Context context = recyclerView.getContext();

        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
        addListDividers();
    }

    private void addListDividers() {
        final Resources resources = recyclerView.getResources();
        final Drawable divider = resources.getDrawable(com.soundcloud.androidkit.R.drawable.ak_list_divider_item);
        int dividerHeight = resources.getDimensionPixelSize(com.soundcloud.androidkit.R.dimen.ak_list_divider_horizontal_height);
        recyclerView.addItemDecoration(new DividerItemDecoration(divider, dividerHeight));
    }

    @Override
    public void bindItemView(int position, View bucketView, List<PlayHistoryBucketItem> list) {
        final List<TrackItem> listeningHistory = list.get(position).getListeningHistory();

        adapter.clear();

        if (listeningHistory.isEmpty()) {
            adapter.addItem(PlayHistoryItemEmpty.create());
        } else {
            for (TrackItem trackItem : listeningHistory) {
                adapter.addItem(PlayHistoryItemTrack.create(trackItem));
            }
        }

        adapter.notifyDataSetChanged();
    }

    @OnClick(R.id.play_history_view_all)
    public void onViewAllClicked(View v) {
        navigator.openPlayHistory(v.getContext());
    }

}
