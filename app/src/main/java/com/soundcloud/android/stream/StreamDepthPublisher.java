package com.soundcloud.android.stream;

import com.soundcloud.android.Consts;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.ReferringEvent;
import com.soundcloud.android.events.ScrollDepthEvent;
import com.soundcloud.android.events.ScrollDepthEvent.Action;
import com.soundcloud.android.events.ScrollDepthEvent.ItemDetails;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.rx.observers.LambdaObserver;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.disposables.Disposable;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.support.v7.widget.StaggeredGridLayoutManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StreamDepthPublisher extends RecyclerView.OnScrollListener {

    private final EventBusV2 eventBus;

    private final int[] spansArray;
    private final StaggeredGridLayoutManager layoutManager;

    private Optional<ReferringEvent> previousEvent = Optional.absent();
    private final Disposable disposable;

    private boolean hasFocus;
    private boolean playerExpanded;

    StreamDepthPublisher(StaggeredGridLayoutManager layoutManager,
                         boolean hasFocus,
                         EventBusV2 eventBus) {
        this.layoutManager = layoutManager;
        this.eventBus = eventBus;
        this.hasFocus = hasFocus;

        spansArray = new int[layoutManager.getSpanCount()];
        disposable = eventBus.queue(EventQueue.PLAYER_UI).subscribeWith(LambdaObserver.onNext(event -> {
            playerExpanded = event.getKind() == PlayerUIEvent.PLAYER_EXPANDED;
            onStreamUIChange();
        }));

        attemptTrackingStart();
    }

    void onFocusChange(boolean hasFocus) {
        this.hasFocus = hasFocus;
        onStreamUIChange();
    }

    private void attemptTrackingStart() {
        if (streamVisible() && !previousEvent.isPresent()) {
            trackScrollDepthState(Action.START);
        }
    }

    private void attemptTrackingEnd() {
        if (previousEvent.isPresent()) {
            trackScrollDepthState(Action.END);
            previousEvent = Optional.absent();
        }
    }

    private void onStreamUIChange() {
        if (streamVisible()) {
            attemptTrackingStart();
        } else {
            attemptTrackingEnd();
        }
    }

    private boolean streamVisible() {
        return hasFocus && !playerExpanded;
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        if (streamVisible()) {
            if (previousEvent.isPresent()) {
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        trackScrollDepthState(Action.SCROLL_START);
                        break;
                    case RecyclerView.SCROLL_STATE_IDLE:
                        trackScrollDepthState(Action.SCROLL_STOP);
                        break;
                    case RecyclerView.SCROLL_STATE_SETTLING:
                    default:
                        // no-op
                        break;
                }
            }
        }
    }

    private void trackScrollDepthState(Action action) {
        final int numColumns = layoutManager.getSpanCount();

        if (action == Action.START) {
            publishAction(ScrollDepthEvent.create(Screen.STREAM, action, numColumns, Collections.emptyList(), Collections.emptyList(), previousEvent));
        } else {
            final List<ItemDetails> firstVisible = itemDetailsForPositions(layoutManager, layoutManager.findFirstVisibleItemPositions(spansArray));
            final List<ItemDetails> lastVisible = itemDetailsForPositions(layoutManager, layoutManager.findLastVisibleItemPositions(spansArray));
            if (!firstVisible.isEmpty() && !lastVisible.isEmpty()) {
                publishAction(ScrollDepthEvent.create(Screen.STREAM, action, numColumns, firstVisible, lastVisible, previousEvent));
            }
        }
    }

    private void publishAction(ScrollDepthEvent event) {
        previousEvent = Optional.of(ReferringEvent.create(event.id(), ScrollDepthEvent.KIND));
        eventBus.publish(EventQueue.TRACKING, event);
    }

    private List<ItemDetails> itemDetailsForPositions(LayoutManager layoutManager, int[] positions) {
        final List<ItemDetails> result = new ArrayList<>(positions.length);

        for (int column = 0; column < positions.length; column++) {
            int position = positions[column];
            if (position != Consts.NOT_SET) {
                result.add(itemDetailsForPosition(layoutManager, column, position));
            }
        }
        return result;
    }

    private ItemDetails itemDetailsForPosition(LayoutManager layoutManager, int column, int position) {
        final float viewablePercentage = ViewUtils.calculateViewablePercentage(layoutManager.findViewByPosition(position));
        return ItemDetails.create(column, position, viewablePercentage);
    }

    void unsubscribe() {
        attemptTrackingEnd();
        disposable.dispose();
    }

    public static class Factory {
        private final EventBusV2 eventBusV2;

        @Inject
        Factory(EventBusV2 eventBusV2) {
            this.eventBusV2 = eventBusV2;
        }

        StreamDepthPublisher create(StaggeredGridLayoutManager layoutManager, boolean hasFocus) {
            return new StreamDepthPublisher(layoutManager, hasFocus, eventBusV2);
        }
    }
}
