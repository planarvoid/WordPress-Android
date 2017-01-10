package com.soundcloud.android.upgrade;

import static com.soundcloud.android.utils.ErrorUtils.isNetworkError;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.configuration.PlanChangeOperations;
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
        USER_NO_ACTION, USER_CLICKED_START
    }

    private final Navigator navigator;
    private final PlanChangeOperations planChangeOperations;
    private final GoOnboardingView view;
    private final EventBus eventBus;

    private AppCompatActivity activity;
    private Subscription subscription = RxUtils.invalidSubscription();

    private Strategy strategy;
    private StrategyContext context;

    @Inject
    GoOnboardingPresenter(Navigator navigator,
                          PlanChangeOperations planChangeOperations,
                          GoOnboardingView view,
                          EventBus eventBus) {
        this.navigator = navigator;
        this.planChangeOperations = planChangeOperations;
        this.view = view;
        this.eventBus = eventBus;
    }

    private LoadingStrategy initialLoadingStrategy() {
        return new LoadingStrategy(false);
    }

    private LoadingStrategy retryLoadingStrategy() {
        return new LoadingStrategy(true);
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        view.bind(activity, this);
        this.activity = activity;
        context = StrategyContext.USER_NO_ACTION;
        strategy = initialLoadingStrategy().proceed();
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        subscription.unsubscribe();
        this.activity = null;
    }

    void onSetupOfflineClicked() {
        context = StrategyContext.USER_CLICKED_START;
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

    private class LoadingStrategy implements Strategy {

        private final boolean isRetrying;

        private LoadingStrategy(boolean isRetrying) {
            this.isRetrying = isRetrying;
        }

        @Override
        public Strategy proceed() {
            strategy = isRetrying ? new PendingStrategy().proceed() : new PendingStrategy();
            // TODO: Pass mid-tier plan if we're onboarding to mid rather than high tier
            subscription = planChangeOperations.awaitAccountUpgrade(Plan.HIGH_TIER)
                                               .observeOn(AndroidSchedulers.mainThread())
                                               .subscribe(new UpgradeCompleteSubscriber());
            return strategy;
        }
    }

    private class PendingStrategy implements Strategy {
        @Override
        public Strategy proceed() {
            if (context == StrategyContext.USER_CLICKED_START) {
                view.setSetUpOfflineButtonWaiting();
            }
            return this;
        }
    }

    private class SuccessStrategy implements Strategy {
        @Override
        public Strategy proceed() {
            if (context == StrategyContext.USER_CLICKED_START) {
                navigator.openCollectionAsRootScreen(activity);
                eventBus.publish(EventQueue.TRACKING,
                                 OfflineInteractionEvent.fromOnboardingStart());
                view.reset();
            }
            return this;
        }
    }

    private class NetworkErrorStrategy implements Strategy {
        @Override
        public Strategy proceed() {
            if (context == StrategyContext.USER_CLICKED_START) {
                view.setSetUpOfflineButtonRetry();
                return retryLoadingStrategy();
            } else {
                return this;
            }
        }
    }

    private class UnrecoverableErrorStrategy implements Strategy {
        @Override
        public Strategy proceed() {
            if (context == StrategyContext.USER_CLICKED_START) {
                view.setSetUpOfflineButtonRetry();
                view.showErrorDialog(activity.getSupportFragmentManager());
                return retryLoadingStrategy();
            } else {
                return this;
            }
        }
    }
}
