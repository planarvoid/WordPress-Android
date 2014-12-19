package com.soundcloud.android.playback.ui;

import static com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.DefaultActivityLifeCycle;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.MotionEvent;
import android.view.View;

import javax.inject.Inject;

@SuppressWarnings({"PMD.CallSuperFirst", "PMD.CallSuperLast"})
public class SlidingPlayerController extends DefaultActivityLifeCycle<ScActivity> implements PanelSlideListener {

    public static final String EXTRA_EXPAND_PLAYER = "expand_player";
    private static final float EXPAND_THRESHOLD = 0.5f;

    private final EventBus eventBus;
    private final PlayQueueManager playQueueManager;

    private ActionBarController actionBarController;
    private SlidingUpPanelLayout slidingPanel;
    private Activity activity;
    private PlayerFragment playerFragment;

    private Subscription subscription = Subscriptions.empty();

    private boolean isExpanding;
    private boolean expandOnResume;
    private boolean wasDragged;

    @Inject
    public SlidingPlayerController(PlayQueueManager playQueueManager, EventBus eventBus) {
        this.eventBus = eventBus;
        this.playQueueManager = playQueueManager;
    }

    @Override
    public void onBind(ScActivity activity) {
        this.activity = activity;
        this.actionBarController = activity.getActionBarController();
        slidingPanel = (SlidingUpPanelLayout) activity.findViewById(R.id.sliding_layout);
        slidingPanel.setPanelSlideListener(this);
        slidingPanel.setEnableDragViewTouchEvents(true);
        slidingPanel.setOnTouchListener(new TrackingDragListener());
        expandOnResume = false;

        playerFragment = getPlayerFragmentFromActivity(activity);
        if (playerFragment == null) {
            throw new IllegalArgumentException("Player fragment not found. Make sure it is present with the expected id.");
        }
    }

    private PlayerFragment getPlayerFragmentFromActivity(FragmentActivity activity) {
        return (PlayerFragment) activity.getSupportFragmentManager().findFragmentById(R.id.player_root);
    }

    private void expand() {
        slidingPanel.expandPanel();
        toggleActionBarAndSysBarVisibility();
    }

    private void collapse() {
        slidingPanel.collapsePanel();
        toggleActionBarAndSysBarVisibility();
    }

    private void show() {
        slidingPanel.showPanel();
        toggleActionBarAndSysBarVisibility();
    }

    private void hide() {
        slidingPanel.hidePanel();
        toggleActionBarAndSysBarVisibility();
    }

    public boolean handleBackPressed() {
        if (slidingPanel.isPanelExpanded()) {
            collapse();
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayerClose(UIEvent.METHOD_BACK_BUTTON));
            return true;
        }
        return false;
    }

    @Override
    public void onCreate(Bundle bundle) {
        expandOnResume = shouldExpand(getCurrentBundle(bundle));
    }

    private Bundle getCurrentBundle(Bundle bundle) {
        if (bundle != null) {
            return bundle;
        }

        final Intent intent = activity.getIntent();
        if (intent != null) {
            return intent.getExtras();
        }
        return null;
    }

    @Override
    public void onNewIntent(Intent intent) {
        expandOnResume = shouldExpand(intent.getExtras());
    }

    private boolean shouldExpand(Bundle bundle) {
        return bundle != null && bundle.getBoolean(EXTRA_EXPAND_PLAYER, false);
    }

    @Override
    public void onResume() {
        if (playQueueManager.isQueueEmpty()) {
            hide();
        } else {
            restorePlayerState();
        }
        expandOnResume = false;
        subscription = eventBus.subscribe(EventQueue.PLAYER_COMMAND, new PlayerCommandSubscriber());
    }

    private void restorePlayerState() {
        showPanelIfNeeded();
        if (expandOnResume) {
            restoreExpanded();
        } else {
            notifyCurrentState();
            toggleActionBarAndSysBarVisibility();
        }
    }

    private void notifyCurrentState() {
        if (slidingPanel.isPanelExpanded()) {
            notifyExpandedState();
        } else {
            notifyCollapsedState();
        }
    }

    private void restoreExpanded() {
        expand();
        notifyExpandedState();
    }

    private void showPanelIfNeeded() {
        if (slidingPanel.isPanelHidden()) {
            show();
        }
    }

    private void toggleActionBarAndSysBarVisibility() {
        boolean panelExpanded = !slidingPanel.isPanelHidden() && slidingPanel.isPanelExpanded();
        actionBarController.setVisible(!panelExpanded);
    }

    @Override
    public void onPause() {
        subscription.unsubscribe();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putBoolean(EXTRA_EXPAND_PLAYER, slidingPanel.isPanelExpanded());
    }

    @Override
    public void onDestroy() {
        this.activity = null;
    }

    @Override
    public void onPanelSlide(View panel, float slideOffset) {
        playerFragment.onPlayerSlide(slideOffset);

        if (slideOffset > EXPAND_THRESHOLD && !isExpanding) {
            actionBarController.setVisible(false);
            isExpanding = true;
        } else if (slideOffset < EXPAND_THRESHOLD && isExpanding) {
            actionBarController.setVisible(true);
            isExpanding = false;
        }
    }

    private class PlayerCommandSubscriber extends DefaultSubscriber<PlayerUICommand> {
        @Override
        public void onNext(PlayerUICommand event) {
            if (event.isExpand()) {
                expand();
            } else if (event.isCollapse()) {
                collapse();
            } else if (event.isShow()) {
                show();
            }
        }
    }

    private class TrackingDragListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                wasDragged = false;
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                wasDragged = true;
            }
            return false;
        }
    }

    @Override
    public void onPanelCollapsed(View panel) {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
        trackPlayerSlide(UIEvent.fromPlayerClose(UIEvent.METHOD_SLIDE));
    }

    @Override
    public void onPanelExpanded(View panel) {
        notifyExpandedState();
        trackPlayerSlide(UIEvent.fromPlayerOpen(UIEvent.METHOD_SLIDE_FOOTER));
    }

    private void trackPlayerSlide(UIEvent event) {
        if (wasDragged) {
            wasDragged = false;
            eventBus.publish(EventQueue.TRACKING, event);
        }
    }

    private void notifyExpandedState() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());
    }

    private void notifyCollapsedState() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
    }

    @Override
    public void onPanelAnchored(View panel) {/* no-op */}

    @Override
    public void onPanelHidden(View view) {/* no-op */}
}
