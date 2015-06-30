package com.soundcloud.android.paywall;

import com.google.common.base.Preconditions;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.MidTierTrackEvent;
import com.soundcloud.android.presentation.ItemAdapter;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.tracks.TrackItem;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

public class PaywallImpressionController implements RecyclerView.OnChildAttachStateChangeListener{

    @VisibleForTesting
    static final int HANDLER_MESSAGE = 0;

    private final EventBus eventBus;
    private final Handler deduplicateHandler;

    private LinearLayoutManager linearLayoutManager;
    private ItemAdapter<ListItem> listItemAdapter;

    @Inject
    public PaywallImpressionController(EventBus eventBus) {
        this(eventBus, new Handler());
    }

    public PaywallImpressionController(EventBus eventBus, Handler deduplicateHandler) {
        this.eventBus = eventBus;
        this.deduplicateHandler = deduplicateHandler;
    }

    public void attachRecyclerView(RecyclerView recyclerView){
        safeAssignLayoutManager(recyclerView);
        safeAssignListAdapter(recyclerView);
        recyclerView.addOnChildAttachStateChangeListener(this);
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    private void safeAssignLayoutManager(RecyclerView recyclerView) {
        final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        Preconditions.checkArgument(layoutManager != null && layoutManager instanceof LinearLayoutManager,
                "PaywallImpressionCreator expects a LinearLayoutManager");

        linearLayoutManager = (LinearLayoutManager) layoutManager;
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    private void safeAssignListAdapter(RecyclerView recyclerView) {
        final RecyclerView.Adapter adapter = recyclerView.getAdapter();
        Preconditions.checkArgument(adapter != null && adapter instanceof ItemAdapter,
                "PaywallImpressionCreator expects an ItemAdapter");

        listItemAdapter = (ItemAdapter) adapter;
    }

    public void detachRecyclerView(RecyclerView recyclerView){
        recyclerView.removeOnChildAttachStateChangeListener(this);
    }

    @Override
    public void onChildViewAttachedToWindow(View view) {
        final ListItem item = listItemAdapter.getItem(linearLayoutManager.getPosition(view));
        if (isMidTierTrack(item) && isNotDuplicate(item)) {
            eventBus.publish(EventQueue.TRACKING, MidTierTrackEvent.forImpression(item.getEntityUrn()));
        }
    }

    @Override
    public void onChildViewDetachedFromWindow(View view) {
        final ListItem item = listItemAdapter.getItem(linearLayoutManager.getPosition(view));
        if (isMidTierTrack(item)) {
            addToDeduplicateHandler(item);
        }
    }

    private boolean isNotDuplicate(ListItem item) {
        return !deduplicateHandler.hasMessages(HANDLER_MESSAGE, item.getEntityUrn());
    }

    private boolean isMidTierTrack(ListItem item) {
        return item instanceof TrackItem && ((TrackItem) item).isMidTier();
    }

    private void addToDeduplicateHandler(ListItem item) {
        deduplicateHandler.sendMessage(Message.obtain(deduplicateHandler, 0, item.getEntityUrn()));
    }
}
