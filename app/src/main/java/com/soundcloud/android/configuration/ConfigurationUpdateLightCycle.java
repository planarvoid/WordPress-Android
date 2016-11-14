package com.soundcloud.android.configuration;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;

import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

public class ConfigurationUpdateLightCycle extends DefaultActivityLightCycle<AppCompatActivity> {

    private final ConfigurationManager configurationManager;
    private final Navigator navigator;
    private final EventBus eventBus;
    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    ConfigurationUpdateLightCycle(ConfigurationManager configurationManager,
                                  Navigator navigator,
                                  EventBus eventBus) {
        this.configurationManager = configurationManager;
        this.navigator = navigator;
        this.eventBus = eventBus;
    }

    @Override
    public void onStart(AppCompatActivity activity) {
        subscription = eventBus.subscribe(EventQueue.USER_PLAN_CHANGE, new PlanChangeSubscriber(activity));
        if (configurationManager.isPendingHighTierUpgrade()) {
            navigator.resetForAccountUpgrade(activity);
        } else if (configurationManager.isPendingDowngrade()) {
            navigator.resetForAccountDowngrade(activity);
        } else {
            configurationManager.requestConfigurationUpdate();
        }
    }

    @Override
    public void onStop(AppCompatActivity activity) {
        subscription.unsubscribe();
        super.onStop(activity);
    }

    private final class PlanChangeSubscriber extends DefaultSubscriber<UserPlanChangedEvent> {
        private final AppCompatActivity activity;

        PlanChangeSubscriber(AppCompatActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onNext(UserPlanChangedEvent event) {
            if (isHighTierUpgradeEvent(event)) {
                navigator.resetForAccountUpgrade(activity);
            } else if (isDowngradeEvent(event)) {
                navigator.resetForAccountDowngrade(activity);
            }
        }
    }

    private static boolean isDowngradeEvent(UserPlanChangedEvent event) {
        return event.newPlan.isDowngradeFrom(event.oldPlan);
    }

    private static boolean isHighTierUpgradeEvent(UserPlanChangedEvent event) {
        return event.newPlan == Plan.HIGH_TIER && event.newPlan.isUpgradeFrom(event.oldPlan);
    }
}