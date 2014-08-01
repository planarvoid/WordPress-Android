package com.soundcloud.android.playback.ui;

import static com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.main.DefaultLifeCycleComponent;
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

import javax.inject.Inject;

@SuppressWarnings({"PMD.CallSuperFirst", "PMD.CallSuperLast"})
public class SlidingPlayerController extends DefaultLifeCycleComponent implements PanelSlideListener {

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

    @Override
    public void attach(Activity activity, ActionBarController actionBarController) {
        this.actionBarController = actionBarController;
        this.activity = activity;
        slidingPanel = (SlidingUpPanelLayout) activity.findViewById(R.id.sliding_layout);
        slidingPanel.setPanelSlideListener(this);
        slidingPanel.setEnableDragViewTouchEvents(true);
    }

    public boolean handleBackPressed() {
        if (isExpanded()) {
            collapse();
            return true;
        }
        return false;
    }

    public boolean isExpanded() {
        return slidingPanel.isPanelExpanded();
    }

    public void expand() {
        slidingPanel.expandPanel();
    }

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
        } else if (slidingPanel.isPanelHidden()) {
            slidingPanel.showPanel();
        } else if (slidingPanel.isPanelExpanded()) {
            dimSystemBars(true);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putBoolean(EXTRA_PLAYER_EXPANDED, slidingPanel.isPanelExpanded());
    }

    @Override
    public void onCreate(Bundle bundle) {
        if (bundle == null) {
            eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsing());
        } else {
            boolean isExpanded = bundle.getBoolean(EXTRA_PLAYER_EXPANDED, false);
            actionBarController.setVisible(!isExpanded);
            dimSystemBars(isExpanded);
            if (isExpanded){
                eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanding());
            } else {
                // FIXME : We should not have to fire both of these, however the player only responds to Collapsing currently
                // this can be fixed in the Smooth Transition story
                eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsing());
                eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
            }

        }
    }

    @Override
    public void onPanelSlide(View panel, float slideOffset) {
        if (slideOffset > EXPAND_THRESHOLD && !isExpanding) {
            actionBarController.setVisible(false);
            dimSystemBars(true);
            eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanding());
            isExpanding = true;
        } else if (slideOffset < EXPAND_THRESHOLD && isExpanding) {
            actionBarController.setVisible(true);
            dimSystemBars(false);
            eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsing());
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
            } else if (event.getKind() == PlayerUIEvent.SHOW_PLAYER) {
                slidingPanel.showPanel();
            }
        }
    }

    @Override
    public void onPanelCollapsed(View panel) {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
    }

    @Override
    public void onPanelExpanded(View panel) {/* no-op */}

    @Override
    public void onPanelAnchored(View panel) {/* no-op */}

    @Override
    public void onPanelHidden(View view) {/* no-op */}
}
