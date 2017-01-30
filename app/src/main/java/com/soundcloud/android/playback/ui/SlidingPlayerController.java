package com.soundcloud.android.playback.ui;

import static com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState;
import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.playback.MiniplayerStorage;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.status.StatusBarColorController;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;

import javax.inject.Inject;

public class SlidingPlayerController extends DefaultActivityLightCycle<AppCompatActivity>
        implements PanelSlideListener {

    private Func1 notExpandedAndItemAdded = new Func1<PlayQueueEvent, Boolean>() {
        @Override
        public Boolean call(PlayQueueEvent playQueueEvent) {
            return !isExpanded() && playQueueEvent.itemAdded();
        }
    };

    public static final String EXTRA_EXPAND_PLAYER = "expand_player";
    private static final String EXTRA_PLAYQUEUE_LOCK = "playqueue_lock";

    private final PlayQueueManager playQueueManager;
    private final EventBus eventBus;
    private final StatusBarColorController statusBarColorController;
    private final MiniplayerStorage miniplayerStorage;

    private SlidingUpPanelLayout slidingPanel;
    private PlayerFragment playerFragment;

    private CompositeSubscription subscription = new CompositeSubscription();

    private boolean isLocked;
    private boolean isPlayQueueLocked;
    private boolean expandOnResume;
    private boolean wasDragged;

    @Inject
    public SlidingPlayerController(PlayQueueManager playQueueManager,
                                   EventBus eventBus,
                                   StatusBarColorController statusBarColorController,
                                   MiniplayerStorage miniplayerStorage) {
        this.playQueueManager = playQueueManager;
        this.eventBus = eventBus;
        this.statusBarColorController = statusBarColorController;
        this.miniplayerStorage = miniplayerStorage;
    }

    @Nullable
    public View getSnackbarHolder() {
        final View view = playerFragment.getView();
        return view != null ? view.findViewById(R.id.player_root) : null;
    }

    @Override
    public void onCreate(AppCompatActivity activity, @Nullable Bundle bundle) {
        slidingPanel = (SlidingUpPanelLayout) activity.findViewById(R.id.sliding_layout);
        slidingPanel.addPanelSlideListener(this);
        slidingPanel.setOnTouchListener(new TrackingDragListener());
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

    private void setupTrackInsertedSubscriber(final View view) {
        subscription.add(eventBus.queue(EventQueue.PLAY_QUEUE)
                                 .filter(notExpandedAndItemAdded)
                                 .subscribe(new ScaleAnimationSubscriber(view)));
    }

    private PlayerFragment getPlayerFragmentFromActivity(AppCompatActivity activity) {
        return (PlayerFragment) activity.getSupportFragmentManager().findFragmentById(R.id.player_root);
    }

    public boolean isExpanded() {
        return slidingPanel.getPanelState() == PanelState.EXPANDED;
    }

    private boolean isHidden() {
        return slidingPanel.getPanelState() == PanelState.HIDDEN;
    }

    private void expand() {
        slidingPanel.setPanelState(PanelState.EXPANDED);
    }

    private void collapse() {
        slidingPanel.setPanelState(PanelState.COLLAPSED);
    }

    private void manualCollapse() {
        miniplayerStorage.setMinimizedPlayerManually();
        collapse();
    }

    private void hide() {
        slidingPanel.setPanelState(PanelState.HIDDEN);
    }

    private void lockExpanded() {
        slidingPanel.setTouchEnabled(false);
        if (!isExpanded()) {
            expand();
        }
        isLocked = true;
    }

    private void unlock() {
        if (!isPlayQueueLocked) {
            slidingPanel.setTouchEnabled(true);
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
        showPanelAsCollapsedIfNeeded();
        if (expandOnResume || isRestoringVideoAd || isPlayQueueLocked) {
            restoreExpanded(isRestoringVideoAd || isPlayQueueLocked);
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

    @Override
    public void onPanelSlide(View panel, float slideOffset) {
        playerFragment.onPlayerSlide(slideOffset);
        statusBarColorController.onPlayerSlide(slideOffset);
    }

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

    @Override
    public void onPanelStateChanged(View panel, PanelState previousState, PanelState newState) {
        switch (newState) {
            case EXPANDED:
                onPanelExpanded();
                break;
            case COLLAPSED:
                onPanelCollapsed();
                break;
            default:
                break;
        }

    }

    public void onPanelCollapsed() {
        statusBarColorController.onPlayerCollapsed();
        notifyCollapsedState();

        if (wasDragged) {
            miniplayerStorage.setMinimizedPlayerManually();
        }

        trackPlayerSlide(UIEvent.fromPlayerClose(wasDragged));
    }

    public void onPanelExpanded() {
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
    }

    private void notifyCollapsedState() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
    }
}
