package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.LockableBottomSheetBehavior;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.status.StatusBarColorController;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;

import javax.inject.Inject;

@SuppressWarnings("PMD.GodClass")
public class SlidingPlayerController extends DefaultActivityLightCycle<AppCompatActivity> {

    public static final String EXTRA_EXPAND_PLAYER = "expand_player";
    private static final String EXTRA_PLAYQUEUE_LOCK = "playqueue_lock";

    private final PlayQueueManager playQueueManager;
    private final EventBus eventBus;
    private final StatusBarColorController statusBarColorController;
    private final PerformanceMetricsEngine performanceMetricsEngine;
    private final LockableBottomSheetBehavior.Factory lockableBottomSheetBehaviorFactory;
    private final CompositeSubscription subscription = new CompositeSubscription();

    private PlayerFragment playerFragment;
    private LockableBottomSheetBehavior<View> bottomSheetBehavior;

    private boolean isLocked;
    private boolean isPlayQueueLocked;
    private boolean expandOnResume;
    private boolean wasDragged;

    @Inject
    public SlidingPlayerController(PlayQueueManager playQueueManager,
                                   EventBus eventBus,
                                   StatusBarColorController statusBarColorController,
                                   PerformanceMetricsEngine performanceMetricsEngine,
                                   LockableBottomSheetBehavior.Factory lockableBottomSheetBehaviorFactory) {
        this.playQueueManager = playQueueManager;
        this.eventBus = eventBus;
        this.statusBarColorController = statusBarColorController;
        this.performanceMetricsEngine = performanceMetricsEngine;
        this.lockableBottomSheetBehaviorFactory = lockableBottomSheetBehaviorFactory;
    }

    @Nullable
    public View getSnackbarHolder() {
        final View view = playerFragment.getView();
        return view != null ? view.findViewById(R.id.player_root) : null;
    }

    @Override
    public void onCreate(AppCompatActivity activity, @Nullable Bundle bundle) {

        View mainContainer = activity.findViewById(R.id.player_root);
        mainContainer.setOnTouchListener(new TrackingDragListener());

        bottomSheetBehavior = lockableBottomSheetBehaviorFactory.from(mainContainer);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_EXPANDED:
                        onPanelExpanded();
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        onPanelCollapsed();
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                onPanelSlide(bottomSheet, slideOffset);
            }
        });

        if (bundle != null) {
            isPlayQueueLocked = bundle.getBoolean(EXTRA_PLAYQUEUE_LOCK, false);
        }
        expandOnResume = shouldExpand(getCurrentBundle(activity, bundle));

        playerFragment = getPlayerFragmentFromActivity(activity);
        if (playerFragment == null) {
            throw new IllegalArgumentException(
                    "Player fragment not found. Make sure it is present with the expected id.");
        }
    }

    public void onPanelSlide(View bottomSheet, float slideOffset) {
        playerFragment.onPlayerSlide(slideOffset);
        statusBarColorController.onPlayerSlide(slideOffset);
    }

    private void setupTrackInsertedSubscriber(final View view) {
        subscription.add(eventBus.queue(EventQueue.PLAY_QUEUE)
                                 .filter(playQueueEvent -> !isExpanded() && playQueueEvent.itemAdded())
                                 .subscribe(new ScaleAnimationSubscriber(view)));
    }

    private PlayerFragment getPlayerFragmentFromActivity(AppCompatActivity activity) {
        return (PlayerFragment) activity.getSupportFragmentManager().findFragmentById(R.id.player_root);
    }

    public boolean isExpanded() {
        return bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED;
    }

    private boolean isHidden() {
        return bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN;
    }

    private void expand() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void collapse() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void manualCollapse() {
        collapse();
    }

    private void hide() {
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private void lockExpanded() {
        bottomSheetBehavior.setLocked(true);
        if (!isExpanded()) {
            expand();
        }
        isLocked = true;
    }

    private void unlock() {
        if (!isPlayQueueLocked) {
            bottomSheetBehavior.setLocked(false);
            isLocked = false;
        }
    }

    public boolean handleBackPressed() {
        if (playerFragment.handleBackPressed()) {
            return true;
        } else {
            if (!isLocked && isExpanded()) {
                manualCollapse();
                return true;
            } else if (isLocked && isPlayQueueLocked) {
                unlockForPlayQueue();
                eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayQueueClose());
                return true;
            }
        }
        return false;
    }

    private Bundle getCurrentBundle(Activity activity, Bundle bundle) {
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
    public void onNewIntent(AppCompatActivity activity, Intent intent) {
        expandOnResume = shouldExpand(intent.getExtras());
    }

    private boolean shouldExpand(Bundle bundle) {
        if (bundle == null) {
            return false;
        } else {
            return bundle.getBoolean(EXTRA_EXPAND_PLAYER, false) || isPlayQueueLocked;
        }

    }

    @Override
    public void onResume(AppCompatActivity activity) {
        if (playQueueManager.isQueueEmpty()) {
            hide();
        } else {
            restorePlayerState();
        }
        expandOnResume = false;
        subscription.add(eventBus.subscribe(EventQueue.PLAYER_COMMAND, new PlayerCommandSubscriber()));
        setupTrackInsertedSubscriber(activity.findViewById(R.id.player_root));
    }

    private void restorePlayerState() {
        final boolean isRestoringVideoAd = playQueueManager.getCurrentPlayQueueItem().isVideoAd();
        final boolean shouldLockPlayer = isRestoringVideoAd || isPlayQueueLocked;

        if (expandOnResume || shouldLockPlayer) {
            restoreExpanded(shouldLockPlayer);
        } else {
            eventBus.queue(EventQueue.PLAYER_UI)
                    .firstOrDefault(PlayerUIEvent.fromPlayerCollapsed())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(uiEvent -> {
                        if (uiEvent.getKind() == PlayerUIEvent.PLAYER_EXPANDED) {
                            restoreExpanded(false);
                        } else {
                            restoreCollapsed();
                        }
                    });
        }
    }

    private void restoreExpanded(boolean shouldLockPlayer) {
        statusBarColorController.onPlayerExpanded();

        expand();
        notifyExpandedState();
        if (shouldLockPlayer) {
            lockExpanded();
        }
    }

    private void restoreCollapsed() {
        statusBarColorController.onPlayerCollapsed();

        collapse();
        notifyCollapsedState();
    }

    private void showPanelAsCollapsedIfNeeded() {
        if (isHidden()) {
            collapse();
        }
    }

    @Override
    public void onPause(AppCompatActivity activity) {
        subscription.clear();
    }

    @Override
    public void onSaveInstanceState(AppCompatActivity activity, Bundle bundle) {
        bundle.putBoolean(EXTRA_EXPAND_PLAYER, isExpanded());
        bundle.putBoolean(EXTRA_PLAYQUEUE_LOCK, isPlayQueueLocked);
    }

    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    private class PlayerCommandSubscriber extends DefaultSubscriber<PlayerUICommand> {
        @Override
        public void onNext(PlayerUICommand event) {
            if (event.isShow()) {
                showPanelAsCollapsedIfNeeded();
            } else if (event.isHide()) {
                hide();
            } else if (event.isExpand()) {
                expand();
            } else if (event.isManualCollapse()) {
                manualCollapse();
            } else if (event.isAutomaticCollapse()) {
                collapse();
            } else if (event.isLockExpanded()) {
                lockExpanded();
            } else if (event.isUnlock()) {
                unlock();
            } else if (event.isLockPlayQueue()) {
                lockForPlayQueue();
            } else if (event.isUnlockPlayQueue()) {
                unlockForPlayQueue();
            }
        }
    }


    private void lockForPlayQueue() {
        lockExpanded();
        isPlayQueueLocked = true;
    }

    private void unlockForPlayQueue() {
        isPlayQueueLocked = false;
        unlock();
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

    void onPanelCollapsed() {
        bottomSheetBehavior.setHideable(false);
        statusBarColorController.onPlayerCollapsed();
        notifyCollapsedState();
        trackPlayerSlide(UIEvent.fromPlayerClose(wasDragged));
    }

    void onPanelExpanded() {
        bottomSheetBehavior.setHideable(false);
        statusBarColorController.onPlayerExpanded();
        notifyExpandedState();
        trackPlayerSlide(UIEvent.fromPlayerOpen(wasDragged));
    }

    private void trackPlayerSlide(UIEvent event) {
        wasDragged = false;
        eventBus.publish(EventQueue.TRACKING, event);
    }

    private void notifyExpandedState() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());
        performanceMetricsEngine.endMeasuring(MetricType.TIME_TO_EXPAND_PLAYER);
    }

    private void notifyCollapsedState() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
    }
}
