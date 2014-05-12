package com.soundcloud.android.playback;

import static com.sothree.slidinguppanel.SlidingUpPanelLayout.SimplePanelSlideListener;

import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;

import android.view.View;

public class PlayerPanelListener extends SimplePanelSlideListener {

    private static final float EXPAND_THRESHOLD = 0.5f;

    private final EventBus eventBus;
    private final ActionBarController actionBarController;

    private boolean isExpanding;

    public PlayerPanelListener(EventBus eventBus, ActionBarController actionBarController) {
        this.eventBus = eventBus;
        this.actionBarController = actionBarController;
    }

    @Override
    public void onPanelSlide(View panel, float slideOffset) {
        if (slideOffset < EXPAND_THRESHOLD && !isExpanding) {
            actionBarController.setVisible(false);
            eventBus.publish(EventQueue.UI, UIEvent.fromPlayerExpanded());
            isExpanding = true;
        } else if (slideOffset > EXPAND_THRESHOLD && isExpanding) {
            actionBarController.setVisible(true);
            eventBus.publish(EventQueue.UI, UIEvent.fromPlayerCollapsed());
            isExpanding = false;
        }
    }

}
