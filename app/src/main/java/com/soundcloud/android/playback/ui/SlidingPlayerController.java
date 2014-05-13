package com.soundcloud.android.playback.ui;

import static com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import javax.inject.Inject;

public class SlidingPlayerController implements PlayerController, PanelSlideListener {

    private static final String EXTRA_ACTIONBAR_VISIBLE = "actionbar_visible";
    private static final float EXPAND_THRESHOLD = 0.5f;

    private final EventBus eventBus;
    private ActionBarController actionBarController;
    private SlidingUpPanelLayout slidingPanel;

    private Subscription subscription = Subscriptions.empty();

    private boolean isExpanding;

    @Inject
    public SlidingPlayerController(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void attach(Activity activity, ActionBarController actionBarController) {
        this.actionBarController = actionBarController;
        slidingPanel = (SlidingUpPanelLayout) activity.findViewById(R.id.sliding_layout);
        slidingPanel.setPanelSlideListener(this);
        slidingPanel.setDragView(activity.findViewById(R.id.footer_drag_view));
    }

    @Override
    public void startListening() {
        subscription = eventBus.subscribe(EventQueue.PLAYER_UI, new PlayerUISubscriber());
    }

    @Override
    public void stopListening() {
        subscription.unsubscribe();
    }

    public boolean isExpanded() {
        return slidingPanel.isExpanded();
    }

    public void collapse() {
        slidingPanel.collapsePane();
    }

    @Override
    public void storeState(Bundle bundle) {
        bundle.putBoolean(EXTRA_ACTIONBAR_VISIBLE, actionBarController.isVisible());
    }

    @Override
    public void restoreState(Bundle bundle) {
        if (bundle != null) {
            actionBarController.setVisible(bundle.getBoolean(EXTRA_ACTIONBAR_VISIBLE, true));
        }
    }

    @Override
    public void onPanelSlide(View panel, float slideOffset) {
        if (slideOffset < EXPAND_THRESHOLD && !isExpanding) {
            actionBarController.setVisible(false);
            eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());
            isExpanding = true;

        } else if (slideOffset > EXPAND_THRESHOLD && isExpanding) {
            actionBarController.setVisible(true);
            eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
            isExpanding = false;
        }
    }

    @Override
    public void onPanelCollapsed(View panel) {}

    @Override
    public void onPanelExpanded(View panel) {}

    @Override
    public void onPanelAnchored(View panel) {}

    private class PlayerUISubscriber extends DefaultSubscriber<PlayerUIEvent> {
        @Override
        public void onNext(PlayerUIEvent event) {
            if (event.getKind() == PlayerUIEvent.PLAY_TRIGGERED) {
                slidingPanel.expandPane();
            }
        }
    }

}
