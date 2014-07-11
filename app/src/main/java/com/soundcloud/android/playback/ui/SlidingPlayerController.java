package com.soundcloud.android.playback.ui;

import static com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;

import javax.inject.Inject;

public class SlidingPlayerController implements PlayerController, PanelSlideListener {

    private static final String EXTRA_PLAYER_EXPANDED = "player_expanded";
    private static final float EXPAND_THRESHOLD = 0.5f;
    private static final int EMPTY_SYSTEM_UI_FLAGS = 0;

    private final EventBus eventBus;
    private final PlayQueueManager playQueueManager;
    private ActionBarController actionBarController;
    private SlidingUpPanelLayout slidingPanel;
    private Activity activity;

    private Subscription subscription = Subscriptions.empty();

    private boolean isExpanding;

    @Inject
    public SlidingPlayerController(PlayQueueManager playQueueManager, EventBus eventBus) {
        this.eventBus = eventBus;
        this.playQueueManager = playQueueManager;
    }

    public void attach(Activity activity, ActionBarController actionBarController) {
        this.actionBarController = actionBarController;
        this.activity = activity;
        slidingPanel = (SlidingUpPanelLayout) activity.findViewById(R.id.sliding_layout);
        slidingPanel.setPanelSlideListener(this);
        slidingPanel.setEnableDragViewTouchEvents(true);
    }

    @Override
    public boolean isExpanded() {
        return slidingPanel.isPanelExpanded();
    }

    @Override
    public void expand() {
        slidingPanel.expandPanel();
    }

    @Override
    public void collapse() {
        slidingPanel.collapsePanel();
    }

    @Override
    public void onResume() {
        subscription = eventBus.subscribe(EventQueue.PLAYER_UI, new PlayerUISubscriber());
        refreshVisibility();
    }

    @Override
    public void onPause() {
        subscription.unsubscribe();
    }

    private void refreshVisibility() {
        if (playQueueManager.isQueueEmpty()) {
            slidingPanel.hidePanel();
        } else {
            slidingPanel.showPanel();
        }
        if (slidingPanel.isPanelExpanded()) {
            dimSystemBars(true);
        }
    }

    @Override
    public void storeState(Bundle bundle) {
        bundle.putBoolean(EXTRA_PLAYER_EXPANDED, slidingPanel.isPanelExpanded());
    }

    @Override
    public void restoreState(Bundle bundle) {
        if (bundle == null) {
            eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
        } else {
            boolean isExpanded = bundle.getBoolean(EXTRA_PLAYER_EXPANDED, false);
            actionBarController.setVisible(!isExpanded);
            dimSystemBars(isExpanded);
            eventBus.publish(EventQueue.PLAYER_UI, isExpanded
                    ? PlayerUIEvent.fromPlayerExpanded()
                    : PlayerUIEvent.fromPlayerCollapsed());
        }
    }

    @Override
    public void onPanelSlide(View panel, float slideOffset) {
        if (slideOffset > EXPAND_THRESHOLD && !isExpanding) {
            actionBarController.setVisible(false);
            dimSystemBars(true);
            eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());
            isExpanding = true;
        } else if (slideOffset < EXPAND_THRESHOLD && isExpanding) {
            actionBarController.setVisible(true);
            dimSystemBars(false);
            eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
            isExpanding = false;
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void dimSystemBars(boolean shouldDim) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            View decorView = activity.getWindow().getDecorView();
            decorView.setSystemUiVisibility(shouldDim ? View.SYSTEM_UI_FLAG_LOW_PROFILE : EMPTY_SYSTEM_UI_FLAGS);
        }
    }

    private class PlayerUISubscriber extends DefaultSubscriber<PlayerUIEvent> {
        @Override
        public void onNext(PlayerUIEvent event) {
            if (event.getKind() == PlayerUIEvent.EXPAND_PLAYER) {
                slidingPanel.expandPanel();
            } else if (event.getKind() == PlayerUIEvent.COLLAPSE_PLAYER) {
                slidingPanel.collapsePanel();
            }
        }
    }

    @Override
    public void onPanelCollapsed(View panel) {}

    @Override
    public void onPanelExpanded(View panel) {}

    @Override
    public void onPanelAnchored(View panel) {}

    @Override
    public void onPanelHidden(View view) {

    }

}
