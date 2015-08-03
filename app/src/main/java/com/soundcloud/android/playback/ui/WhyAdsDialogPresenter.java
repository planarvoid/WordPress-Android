package com.soundcloud.android.playback.ui;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpsellTrackingEvent;
import com.soundcloud.android.rx.eventbus.EventBus;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import javax.inject.Inject;

class WhyAdsDialogPresenter {

    private final Navigator navigator;
    private final FeatureOperations featureOperations;
    private final EventBus eventBus;

    @Inject
    public WhyAdsDialogPresenter(Navigator navigator, FeatureOperations featureOperations, EventBus eventBus) {
        this.navigator = navigator;
        this.featureOperations = featureOperations;
        this.eventBus = eventBus;
    }

    public void show(final Activity activity) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.why_ads)
                .setMessage(R.string.why_ads_dialog_message);
        configureButtons(activity, dialog);
        dialog.create().show();
    }

    private void configureButtons(final Activity activity, AlertDialog.Builder dialog) {
        if (featureOperations.upsellRemoveAudioAds()) {
            eventBus.publish(EventQueue.TRACKING, UpsellTrackingEvent.forWhyAdsImpression());
            dialog.setPositiveButton(R.string.upsell_remove_ads, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    navigator.openUpgrade(activity);
                    eventBus.publish(EventQueue.TRACKING, UpsellTrackingEvent.forWhyAdsClick());
                }
            })
            .setNegativeButton(android.R.string.ok, null);
        } else {
            dialog.setPositiveButton(android.R.string.ok, null);
        }
    }

}
