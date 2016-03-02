package com.soundcloud.android.downgrade;

import static com.soundcloud.android.utils.ErrorUtils.isNetworkError;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.configuration.PlanChangeOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

class GoOffboardingPresenter extends DefaultSupportFragmentLightCycle<Fragment> {

    enum StrategyContext {
        USER_NO_ACTION, USER_CONTINUE, USER_RESUBSCRIBE
    }

    private final Navigator navigator;
    private final PlanChangeOperations operations;
    private final GoOffboardingView view;
    private final EventBus eventBus;

    private Fragment fragment;
    private Subscription subscription = RxUtils.invalidSubscription();

    private Strategy strategy;
    private StrategyContext context;

    @Inject
    GoOffboardingPresenter(Navigator navigator,
                           PlanChangeOperations operations,
                           GoOffboardingView view,
                           EventBus eventBus) {
        this.navigator = navigator;
        this.operations = operations;
        this.view = view;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        this.fragment = fragment;
        context = StrategyContext.USER_NO_ACTION;
        strategy = new InitStrategy().proceed();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        this.view.bind(fragment.getActivity(), this);
    }

    void trackResubscribeButtonImpression() {
        eventBus.publish(EventQueue.TRACKING, UpgradeTrackingEvent.forResubscribeImpression());
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        view.unbind();
    }

    @Override
    public void onDestroy(Fragment fragment) {
        subscription.unsubscribe();
        this.fragment = null;
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
                    navigator.openHomeAsRootScreen(fragment.getContext());
                    view.reset();
                    return this;
                case USER_RESUBSCRIBE:
                    navigator.openUpgrade(fragment.getContext());
                    eventBus.publish(EventQueue.TRACKING, UpgradeTrackingEvent.forResubscribeClick());
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
                    view.showErrorDialog(fragment.getFragmentManager());
                    return new InitStrategy();
                case USER_RESUBSCRIBE:
                    view.setResubscribeButtonRetry();
                    view.showErrorDialog(fragment.getFragmentManager());
                    return new InitStrategy();
                default:
                    return this;
            }
        }
    }

}
