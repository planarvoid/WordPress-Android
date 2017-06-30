package com.soundcloud.android.analytics.appboy;

import com.appboy.models.IInAppMessage;
import com.appboy.models.MessageButton;
import com.appboy.ui.inappmessage.InAppMessageCloser;
import com.appboy.ui.inappmessage.InAppMessageOperation;
import com.appboy.ui.inappmessage.listeners.IInAppMessageManagerListener;

final class AppboyInAppMessageListenerFactory {

    static IInAppMessageManagerListener forImmediateDisplay() {
        return create(InAppMessageOperation.DISPLAY_NOW);
    }

    static IInAppMessageManagerListener forDisplayLater() {
        return create(InAppMessageOperation.DISPLAY_LATER);
    }

    private static IInAppMessageManagerListener create(InAppMessageOperation displayOption) {
        return new IInAppMessageManagerListener() {
            @Override
            public boolean onInAppMessageReceived(IInAppMessage iInAppMessage) {
                return false; // Not handled
            }

            @Override
            public InAppMessageOperation beforeInAppMessageDisplayed(IInAppMessage iInAppMessage) {
                return displayOption;
            }

            @Override
            public boolean onInAppMessageClicked(IInAppMessage iInAppMessage, InAppMessageCloser inAppMessageCloser) {
                return false; // Not handled
            }

            @Override
            public boolean onInAppMessageButtonClicked(MessageButton messageButton, InAppMessageCloser inAppMessageCloser) {
                return false; // Not handled
            }

            @Override
            public void onInAppMessageDismissed(IInAppMessage iInAppMessage) {}
        };
    }

    private AppboyInAppMessageListenerFactory() {}

}
