package com.soundcloud.android.configuration;

import com.soundcloud.android.navigation.NavigationExecutor;
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
    private final PendingPlanOperations pendingPlanOperations;
    private final NavigationExecutor navigationExecutor;
    private final EventBus eventBus;
    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    ConfigurationUpdateLightCycle(ConfigurationManager configurationManager,
                                  PendingPlanOperations pendingPlanOperations,
                                  NavigationExecutor navigationExecutor,
                                  EventBus eventBus) {
        this.configurationManager = configurationManager;
        this.pendingPlanOperations = pendingPlanOperations;
        this.navigationExecutor = navigationExecutor;
        this.eventBus = eventBus;
    }

    @Override
    public void onStart(AppCompatActivity activity) {
        subscription = eventBus.subscribe(EventQueue.USER_PLAN_CHANGE, new PlanChangeSubscriber(activity));
        if (pendingPlanOperations.isPendingUpgrade()) {
            navigationExecutor.resetForAccountUpgrade(activity);
        } else if (pendingPlanOperations.isPendingDowngrade()) {
            navigationExecutor.resetForAccountDowngrade(activity);
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
            if (isUpgradeEvent(event)) {
                navigationExecutor.resetForAccountUpgrade(activity);
            } else if (isDowngradeEvent(event)) {
                navigationExecutor.resetForAccountDowngrade(activity);
            }
        }
    }

    private static boolean isDowngradeEvent(UserPlanChangedEvent event) {
        return event.newPlan.isDowngradeFrom(event.oldPlan);
    }

    private static boolean isUpgradeEvent(UserPlanChangedEvent event) {
        return event.newPlan.isUpgradeFrom(event.oldPlan);
    }
}
