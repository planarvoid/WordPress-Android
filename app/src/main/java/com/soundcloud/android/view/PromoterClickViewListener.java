package com.soundcloud.android.view;

import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.rx.eventbus.EventBus;

import android.view.View;

public class PromoterClickViewListener implements View.OnClickListener {

    private final PlayableItem item;
    private final EventBus eventBus;
    private final ScreenProvider screenProvider;
    private final NavigationExecutor navigationExecutor;

    public PromoterClickViewListener(PlayableItem item,
                                     EventBus eventBus,
                                     ScreenProvider screenProvider,
                                     NavigationExecutor navigationExecutor) {
        this.item = item;
        this.eventBus = eventBus;
        this.screenProvider = screenProvider;
        this.navigationExecutor = navigationExecutor;
    }

    @Override
    public void onClick(View v) {
        navigationExecutor.legacyOpenProfile(v.getContext(), item.promoterUrn().get());
        PromotedTrackingEvent event = PromotedTrackingEvent.forPromoterClick(item, screenProvider.getLastScreenTag());
        eventBus.publish(EventQueue.TRACKING, event);
    }
}
