package com.soundcloud.android.downgrade;

import static com.soundcloud.android.utils.ErrorUtils.isNetworkError;

import com.soundcloud.android.configuration.PendingPlanOperations;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.configuration.PlanChangeOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.payments.UpsellContext;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

class GoOffboardingPresenter extends DefaultSupportFragmentLightCycle<Fragment> {

    private enum StrategyContext {
        USER_NO_ACTION, USER_CONTINUE, USER_RESUBSCRIBE
    }

    private final NavigationExecutor navigationExecutor;
    private final PlanChangeOperations planChangeOperations;
    private final GoOffboardingView view;
    private final EventBus eventBus;
    private final Plan plan;

    private Fragment fragment;
    private Disposable disposable = RxUtils.invalidDisposable();

    private Strategy strategy;
    private StrategyContext context;

    @Inject
    GoOffboardingPresenter(NavigationExecutor navigationExecutor,
                           PendingPlanOperations pendingPlanOperations,
                           PlanChangeOperations planChangeOperations,
                           GoOffboardingView view,
                           EventBus eventBus) {
        this.navigationExecutor = navigationExecutor;
        this.planChangeOperations = planChangeOperations;
        this.view = view;
        this.eventBus = eventBus;
        this.plan = pendingPlanOperations.getPendingDowngrade();
    }

    private LoadingStrategy initialLoadingStrategy() {
        return new LoadingStrategy(false);
    }

    private LoadingStrategy retryLoadingStrategy() {
        return new LoadingStrategy(true);
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        if (plan == Plan.UNDEFINED || plan == Plan.HIGH_TIER) {
            throw new IllegalStateException("Cannot downgrade to plan: " + plan.planId);
        }
        this.fragment = fragment;
        this.view.bind(fragment.getActivity(), this, plan);
        context = StrategyContext.USER_NO_ACTION;
        strategy = initialLoadingStrategy().proceed();
    }

    void trackResubscribeButtonImpression() {
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forResubscribeImpression());
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        view.unbind();
    }

    @Override
    public void onDestroy(Fragment fragment) {
        disposable.dispose();
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

    private class DowngradeCompleteObserver extends DefaultObserver<Object> {

        @Override
        public void onComplete() {
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

    private class LoadingStrategy implements Strategy {

        private boolean isRetrying;

        private LoadingStrategy(boolean isRetrying) {
            this.isRetrying = isRetrying;
        }

        @Override
        public Strategy proceed() {
            strategy = isRetrying ? new PendingStrategy().proceed() : new PendingStrategy();
            disposable.dispose();
            disposable = planChangeOperations.awaitAccountDowngrade()
                                             .observeOn(AndroidSchedulers.mainThread())
                                             .subscribeWith(new DowngradeCompleteObserver());
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
                    navigationExecutor.openHomeAsRootScreen(fragment.getActivity());
                    view.reset();
                    return this;
                case USER_RESUBSCRIBE:
                    navigationExecutor.openUpgradeOnMain(fragment.getContext(), UpsellContext.DEFAULT);
                    eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forResubscribeClick());
                    view.reset();
                    fragment.getActivity().finish();
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
                    return retryLoadingStrategy();
                case USER_RESUBSCRIBE:
                    view.setResubscribeButtonRetry();
                    return retryLoadingStrategy();
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
                    return retryLoadingStrategy();
                case USER_RESUBSCRIBE:
                    view.setResubscribeButtonRetry();
                    view.showErrorDialog(fragment.getFragmentManager());
                    return retryLoadingStrategy();
                default:
                    return this;
            }
        }
    }

}
