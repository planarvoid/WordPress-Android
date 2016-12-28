package com.soundcloud.android.suggestedcreators;

import static butterknife.ButterKnife.findById;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.stream.StreamItem.SuggestedCreators;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.lang.ref.WeakReference;
import java.util.List;

public class SuggestedCreatorsItemRenderer implements CellRenderer<SuggestedCreators> {

    private final SuggestedCreatorRenderer suggestedCreatorRenderer;
    private SuggestedCreatorsAdapter adapter;
    private WeakReference<RecyclerView> recyclerViewRef;

    @Inject
    SuggestedCreatorsItemRenderer(SuggestedCreatorRenderer suggestedCreatorRenderer) {
        this.suggestedCreatorRenderer = suggestedCreatorRenderer;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                                  .inflate(R.layout.suggested_creators_stream_notification_card, parent, false);
        RecyclerView recyclerView = ButterKnife.findById(view, R.id.suggested_creators_carousel);
        initCarousel(recyclerView);
        recyclerViewRef = new WeakReference<>(recyclerView);
        return view;
    }

    public void onFollowingEntityChange(EntityStateChangedEvent event) {
        if (adapter != null) {
            adapter.onFollowingEntityChange(event);
        }
    }

    private void initCarousel(final RecyclerView recyclerView) {
        final Context context = recyclerView.getContext();
        adapter = new SuggestedCreatorsAdapter(suggestedCreatorRenderer);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(context,
                                                                          LinearLayoutManager.HORIZONTAL,
                                                                          false);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void bindItemView(int position, View itemView, List<SuggestedCreators> items) {
        StaggeredGridLayoutManager.LayoutParams layoutParams = new StaggeredGridLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                                                           ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setFullSpan(true);
        itemView.setLayoutParams(layoutParams);
        itemView.setEnabled(false);

        final SuggestedCreators suggestedCreatorsNotificationItem = items.get(position);
        bindCarousel(itemView, suggestedCreatorsNotificationItem);
    }

    public void unsubscribe() {
        if (adapter != null) {
            adapter.unsubscribe();
        }

        if (recyclerViewRef != null) {
            RecyclerView recyclerView = recyclerViewRef.get();
            if (recyclerView != null) {
                recyclerView.setAdapter(null);
                recyclerView.setLayoutManager(null);
            }
            recyclerViewRef = null;
        }
    }

    private void bindCarousel(View itemView, SuggestedCreators suggestedCreatorsNotificationItem) {
        final RecyclerView recyclerView = findById(itemView, R.id.suggested_creators_carousel);
        final SuggestedCreatorsAdapter adapter = (SuggestedCreatorsAdapter) recyclerView.getAdapter();
        adapter.clear();
        adapter.onNext(suggestedCreatorsNotificationItem.suggestedCreators());
    }
}
