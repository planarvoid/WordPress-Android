package com.soundcloud.android.downgrade;

import static com.soundcloud.android.utils.ErrorUtils.isNetworkError;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

class GoOffboardingPresenter extends DefaultActivityLightCycle<AppCompatActivity> {

    enum StrategyContext {
        USER_NO_ACTION, USER_CONTINUE, USER_RESUBSCRIBE
    }

    private final Navigator navigator;
    private final DowngradeProgressOperations operations;
    private final GoOffboardingView view;
    private final EventBus eventBus;

    private AppCompatActivity activity;
    private Subscription subscription = RxUtils.invalidSubscription();

    private Strategy strategy;
    private StrategyContext context;

    @Inject
    GoOffboardingPresenter(Navigator navigator,
                           DowngradeProgressOperations operations,
                           GoOffboardingView view,
                           EventBus eventBus) {
        this.navigator = navigator;
        this.operations = operations;
        this.view = view;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        view.bind(activity, this);
        this.activity = activity;
        context = StrategyContext.USER_NO_ACTION;
        strategy = new InitStrategy().proceed();
    }

    @Override
    public void onDestroy(AppCompatActivity anActivity) {
        subscription.unsubscribe();
        activity = null;
    }

    void onResubscribeClicked() {
        context = StrategyContext.USER_RESUBSCRIBE;
        strategy = strategy.proceed();
    }

    void onContinueClicked() {
        context = StrategyContext.USER_CONTINUE;
        strategy = strategy.proceed();
    }

    private class DowngradeCompleteSubscriber extends DefaultSubscriber<Object> {

        @Override
        public void onNext(Object args) {
            strategy = new SuccessStrategy().proceed();
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
            subscription = operations.awaitAccountDowngrade()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new DowngradeCompleteSubscriber());
            return strategy;
        }
    }

    private class PendingStrategy implements Strategy {
        @Override
        public Strategy proceed() {
            switch (context) {
                case USER_CONTINUE:
                    view.setContinueButtonWaiting();
                    return this;
                case USER_RESUBSCRIBE:
                    view.setResubscribeButtonWaiting();
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
                case USER_CONTINUE:
                    navigator.openHomeAsRootScreen(activity);
                    //TODO
//                    eventBus.publish(EventQueue.TRACKING,
//                            OfflineInteractionEvent.fromOnboardingDismiss());
                    view.reset();
                    return this;
                case USER_RESUBSCRIBE:
                    navigator.openUpgrade(activity);
                    //TODO
//                    eventBus.publish(EventQueue.TRACKING,
//                            OfflineInteractionEvent.fromOnboardingStart());
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
                case USER_CONTINUE:
                    view.setContinueButtonRetry();
                    return new InitStrategy();
                case USER_RESUBSCRIBE:
                    view.setResubscribeButtonRetry();
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
                case USER_CONTINUE:
                    view.setContinueButtonRetry();
                    view.showErrorDialog(activity.getSupportFragmentManager());
                    return new InitStrategy();
                case USER_RESUBSCRIBE:
                    view.setResubscribeButtonRetry();
                    view.showErrorDialog(activity.getSupportFragmentManager());
                    return new InitStrategy();
                default:
                    return this;
            }
        }
    }

}
