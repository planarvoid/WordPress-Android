package com.soundcloud.android.actionbar;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

import android.support.v4.app.FragmentActivity;

import javax.inject.Inject;

public class PullToRefreshController {

    private final EventBus eventBus;
    private final PullToRefreshAttacher ptrAttacher;

    private PullToRefreshLayout pullToRefreshLayout;

    private Subscription playerExpandedSubscription = Subscriptions.empty();

    @Inject
    public PullToRefreshController(EventBus eventBus, PullToRefreshAttacher ptrAttacher) {
        this.eventBus = eventBus;
        this.ptrAttacher = ptrAttacher;
    }

    @Deprecated
    public PullToRefreshController(EventBus eventBus) {
        this(eventBus, new PullToRefreshAttacher());
    }

    public void attach(FragmentActivity activity, PullToRefreshLayout pullToRefreshLayout, OnRefreshListener listener) {
        this.pullToRefreshLayout = pullToRefreshLayout;
        ptrAttacher.attach(activity, pullToRefreshLayout, listener);
        playerExpandedSubscription = eventBus.subscribe(EventQueue.PLAYER_UI, new PlayerExpandedSubscriber());
    }

    public void detach() {
        playerExpandedSubscription.unsubscribe();
        pullToRefreshLayout = null;
    }

    public boolean isAttached() {
        return pullToRefreshLayout != null;
    }

    public boolean isRefreshing() {
        return pullToRefreshLayout.isRefreshing();
    }

    public void startRefreshing() {
        pullToRefreshLayout.setRefreshing(true);
    }

    public void stopRefreshing() {
        pullToRefreshLayout.setRefreshComplete();
    }

    private final class PlayerExpandedSubscriber extends DefaultSubscriber<PlayerUIEvent> {
        @Override
        public void onNext(PlayerUIEvent event) {
            if (event.getKind() == PlayerUIEvent.PLAYER_EXPANDED) {
                stopRefreshing();
            }
        }
    }

}
