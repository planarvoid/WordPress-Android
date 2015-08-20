package com.soundcloud.android.view;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.rx.eventbus.EventBus;

import android.view.View;

public class PromoterClickViewListener implements View.OnClickListener {

    private final PromotedListItem item;
    private final EventBus eventBus;
    private final ScreenProvider screenProvider;
    private final Navigator navigator;

    public PromoterClickViewListener(PromotedListItem item,
                                     EventBus eventBus,
                                     ScreenProvider screenProvider,
                                     Navigator navigator) {
        this.item = item;
        this.eventBus = eventBus;
        this.screenProvider = screenProvider;
        this.navigator = navigator;
    }

    @Override
    public void onClick(View v) {
        navigator.openProfile(v.getContext(), item.getPromoterUrn().get());
        PromotedTrackingEvent event = PromotedTrackingEvent.forPromoterClick(item, screenProvider.getLastScreenTag());
        eventBus.publish(EventQueue.TRACKING, event);
    }
}
