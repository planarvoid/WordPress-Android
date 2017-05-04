package com.soundcloud.android.analytics.appboy;

import com.appboy.models.IInAppMessage;
import com.appboy.models.MessageButton;
import com.appboy.ui.inappmessage.InAppMessageCloser;
import com.appboy.ui.inappmessage.InAppMessageOperation;
import com.appboy.ui.inappmessage.listeners.IInAppMessageManagerListener;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import javax.inject.Inject;

public class AppboyInAppMessageListener implements IInAppMessageManagerListener {

    private final FeatureFlags featureFlags;

    @Inject
    AppboyInAppMessageListener(FeatureFlags featureFlags){
        this.featureFlags = featureFlags;
    }

    @Override
    public boolean onInAppMessageReceived(IInAppMessage iInAppMessage) {
        return false;
    }

    @Override
    public InAppMessageOperation beforeInAppMessageDisplayed(IInAppMessage iInAppMessage) {
        if (featureFlags.isEnabled(Flag.DISPLAY_PRESTITIAL)) {
            // TODO: More advanced logic coming soon to handle prestitial ad request state
            return InAppMessageOperation.DISCARD;
        } else {
            return InAppMessageOperation.DISPLAY_NOW;
        }
    }

    @Override
    public boolean onInAppMessageClicked(IInAppMessage iInAppMessage, InAppMessageCloser inAppMessageCloser) {
        return false;
    }

    @Override
    public boolean onInAppMessageButtonClicked(MessageButton messageButton, InAppMessageCloser inAppMessageCloser) {
        return false;
    }

    @Override
    public void onInAppMessageDismissed(IInAppMessage iInAppMessage) {
        // no-op
    }
}
