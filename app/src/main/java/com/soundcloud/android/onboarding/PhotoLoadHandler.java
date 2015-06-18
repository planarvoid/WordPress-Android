package com.soundcloud.android.onboarding;

import static com.soundcloud.android.util.AnimUtils.hideView;

import android.os.Handler;
import android.os.Message;
import android.view.View;

import java.lang.ref.WeakReference;

class PhotoLoadHandler extends Handler {
    private final WeakReference<OnboardActivity> onboardActivityRef;
    private final WeakReference<View> splashRef;

    public PhotoLoadHandler(OnboardActivity onboardActivity, View splash) {
        this.onboardActivityRef = new WeakReference<>(onboardActivity);
        this.splashRef = new WeakReference<>(splash);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case TourLayout.IMAGE_LOADED:
            case TourLayout.IMAGE_ERROR:
                final OnboardActivity onboardActivity = onboardActivityRef.get();
                final View splash = splashRef.get();
                if (onboardActivity != null && splash != null) {
                    hideView(splash, true);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown msg.what: " + msg.what);
        }
    }
}
