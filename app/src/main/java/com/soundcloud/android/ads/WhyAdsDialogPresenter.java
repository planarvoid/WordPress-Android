package com.soundcloud.android.ads;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.dialog.CustomFontViewBuilder;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.Context;
import android.support.v7.app.AlertDialog;

import javax.inject.Inject;

public class WhyAdsDialogPresenter {

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
        final CustomFontViewBuilder view = new CustomFontViewBuilder(context).setTitle(R.string.ads_why_ads);
        final AlertDialog.Builder dialog = new AlertDialog.Builder(context);

        if (featureOperations.upsellRemoveAudioAds()) {
            eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forWhyAdsImpression());
            configureForUpsell(context, view, dialog);
        } else {
            configureDefaultDialog(view, dialog);
        }
        dialog.create().show();
    }

    private void configureDefaultDialog(CustomFontViewBuilder view, AlertDialog.Builder dialog) {
        dialog.setView(view.setMessage(R.string.ads_why_ads_dialog_message).get())
              .setPositiveButton(android.R.string.ok, null);
    }

    private void configureForUpsell(final Context context, CustomFontViewBuilder view, AlertDialog.Builder dialog) {
        dialog.setView(view.setMessage(R.string.ads_why_ads_upsell_dialog_message).get())
              .setPositiveButton(R.string.upsell_remove_ads, (dialog11, which) -> {
                  navigator.openUpgrade(context);
                  eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forWhyAdsClick());
              })
              .setNegativeButton(android.R.string.ok, null);
    }
}
