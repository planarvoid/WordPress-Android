package com.soundcloud.android.upgrade;

import static com.soundcloud.android.utils.ErrorUtils.isNetworkError;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

class GoOnboardingPresenter extends DefaultActivityLightCycle<AppCompatActivity> {
    enum StrategyContext {
        USER_NO_ACTION, USER_SETUP_LATER, USER_SETUP_OFFLINE
    }

    private final Navigator navigator;
    private final UpgradeProgressOperations upgradeProgressOperations;
    private final GoOnboardingView view;
    private final EventBus eventBus;

    private AppCompatActivity activity;
    private Subscription subscription = RxUtils.invalidSubscription();

    private Strategy strategy;
    private StrategyContext context;

    @Inject
    GoOnboardingPresenter(Navigator navigator, UpgradeProgressOperations upgradeProgressOperations, GoOnboardingView view, EventBus eventBus) {
        this.navigator = navigator;
        this.upgradeProgressOperations = upgradeProgressOperations;
        this.view = view;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(AppCompatActivity anActivity, Bundle bundle) {
        view.bind(anActivity, this);
        this.activity = anActivity;
        context = StrategyContext.USER_NO_ACTION;
        strategy = new InitStrategy().proceed();
    }

    @Override
    public void onDestroy(AppCompatActivity anActivity) {
        subscription.unsubscribe();
        activity = null;
    }

    void onSetupOfflineClicked() {
        context = StrategyContext.USER_SETUP_OFFLINE;
        strategy = strategy.proceed();
    }

    void onSetupLaterClicked() {
        context = StrategyContext.USER_SETUP_LATER;
        strategy = strategy.proceed();
    }

    private class UpgradeCompleteSubscriber extends DefaultSubscriber<Object> {

        private boolean hasPlan = false;

        @Override
        public void onCompleted() {
            if (hasPlan) {
                strategy = new SuccessStrategy().proceed();
            } else {
                strategy = new UnrecoverableErrorStrategy().proceed();
            }
        }

        @Override
        public void onNext(Object args) {
            hasPlan = true;
        }

        @Override
        public void onError(Throwable e) {
            if (isNetworkError(e)) {
                strategy = new NetworkErrorStrategy().proceed();
            } else {
                strategy = new UnrecoverableErrorStrategy().proceed();
            }
            // reporting
            super.onError(e);
        }

    }

    interface Strategy {
        Strategy proceed();
    }

    private class InitStrategy implements Strategy {
        @Override
        public Strategy proceed() {
            strategy = new PendingStrategy();
            subscription = upgradeProgressOperations.awaitAccountUpgrade()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new UpgradeCompleteSubscriber());
            return strategy;
        }
    }

    private class PendingStrategy implements Strategy {
        @Override
        public Strategy proceed() {
            switch (context) {
                case USER_SETUP_LATER:
                    view.setSetUpLaterButtonWaiting();
                    return this;
                case USER_SETUP_OFFLINE:
                    view.setSetUpOfflineButtonWaiting();
                    return this;
                default:
                    return this;
            }
        }
    }

    private class SuccessStrategy implements Strategy {
        @Override
        public Strategy proceed() {
            switch (context) {
                case USER_SETUP_LATER:
                    navigator.openHomeAsRootScreen(activity);
                    eventBus.publish(EventQueue.TRACKING,
                            OfflineInteractionEvent.fromOnboardingDismiss());
                    view.reset();
                    return this;
                case USER_SETUP_OFFLINE:
                    navigator.openOfflineContentOnboarding(activity);
                    eventBus.publish(EventQueue.TRACKING,
                            OfflineInteractionEvent.fromOnboardingStart());
                    view.reset();
                    return this;
                default:
                    return this;
            }
        }
    }

    private class NetworkErrorStrategy implements Strategy {
        @Override
        public Strategy proceed() {
            switch (context) {
                case USER_SETUP_LATER:
                    view.setSetUpLaterButtonRetry();
                    return new InitStrategy();
                case USER_SETUP_OFFLINE:
                    view.setSetUpOfflineButtonRetry();
                    return new InitStrategy();
                default:
                    return this;
            }
        }
    }

    private class UnrecoverableErrorStrategy implements Strategy {
        @Override
        public Strategy proceed() {
            switch (context) {
                case USER_SETUP_LATER:
                    view.setSetUpLaterButtonRetry();
                    view.showErrorDialog(activity.getSupportFragmentManager());
                    return new InitStrategy();
                case USER_SETUP_OFFLINE:
                    view.setSetUpOfflineButtonRetry();
                    view.showErrorDialog(activity.getSupportFragmentManager());
                    return new InitStrategy();
                default:
                    return this;
            }
        }
    }
}
