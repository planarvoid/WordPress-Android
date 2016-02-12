package com.soundcloud.android.playback.ui;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.rx.eventbus.EventBus;

import android.app.Activity;
import android.content.Context;
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

    public void show(final Context context) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.ads_why_ads);

        if (featureOperations.upsellRemoveAudioAds()) {
            eventBus.publish(EventQueue.TRACKING, UpgradeTrackingEvent.forWhyAdsImpression());
            configureDialogForUpsell(context, dialog);
        } else {
            configureDefaultDialog(dialog);
        }
        dialog.create().show();
    }

    private void configureDefaultDialog(AlertDialog.Builder dialog) {
        dialog.setMessage(R.string.ads_why_ads_dialog_message)
                .setPositiveButton(android.R.string.ok, null);
    }

    private void configureDialogForUpsell(final Context context, AlertDialog.Builder dialog) {
        dialog.setMessage(R.string.ads_why_ads_upsell_dialog_message)
                .setPositiveButton(R.string.upsell_remove_ads, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog11, int which) {
                navigator.openUpgrade((Activity) context);
                eventBus.publish(EventQueue.TRACKING, UpgradeTrackingEvent.forWhyAdsClick());
            }
        })
        .setNegativeButton(android.R.string.ok, null);
    }

}
