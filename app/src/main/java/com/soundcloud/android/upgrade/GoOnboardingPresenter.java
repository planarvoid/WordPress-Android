package com.soundcloud.android.upgrade;

import static com.soundcloud.android.utils.ErrorUtils.isNetworkError;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.configuration.PendingPlanOperations;
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

class GoOnboardingPresenter extends DefaultActivityLightCycle<AppCompatActivity> implements GoOnboardingView.Listener {

    private static final String BUNDLE_PENDING_PLAN = "pending_plan";

    private enum StrategyContext {
        USER_NO_ACTION, USER_CLICKED_START
    }

    private final Navigator navigator;
    private final PendingPlanOperations pendingPlanOperations;
    private final PlanChangeOperations planChangeOperations;
    private final GoOnboardingView view;
    private final EventBus eventBus;

    private AppCompatActivity activity;
    private Subscription subscription = RxUtils.invalidSubscription();

    private Plan plan;
    private Strategy strategy;
    private StrategyContext context;

    @Inject
    GoOnboardingPresenter(Navigator navigator,
                          PendingPlanOperations pendingPlanOperations,
                          PlanChangeOperations planChangeOperations,
                          GoOnboardingView view,
                          EventBus eventBus) {
        this.navigator = navigator;
        this.pendingPlanOperations = pendingPlanOperations;
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
        resolvePendingPlan(bundle);
        view.bind(activity, this, plan);
        this.activity = activity;
        context = StrategyContext.USER_NO_ACTION;
        strategy = initialLoadingStrategy().proceed();
    }

    /*
     * We save pending plan to the bundle, so that we can continue to render the correct onboarding flow
     * on configuration change (rotation), even after the new plan has been applied.
     */
    private void resolvePendingPlan(Bundle bundle) {
        if (bundle == null || !bundle.containsKey(BUNDLE_PENDING_PLAN)) {
            plan = pendingPlanOperations.getPendingUpgrade();
            if (plan == Plan.UNDEFINED || plan == Plan.FREE_TIER) {
                throw new IllegalStateException("Cannot upgrade to plan: " + plan.planId);
            }
        } else {
            plan = (Plan) bundle.getSerializable(BUNDLE_PENDING_PLAN);
        }
    }

    @Override
    public void onSaveInstanceState(AppCompatActivity activity, Bundle bundle) {
        bundle.putSerializable(BUNDLE_PENDING_PLAN, plan);
        super.onSaveInstanceState(activity, bundle);
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        view.unbind();
        subscription.unsubscribe();
        this.activity = null;
    }

    @Override
    public void onStartClicked() {
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
            subscription = planChangeOperations.awaitAccountUpgrade()
                                               .observeOn(AndroidSchedulers.mainThread())
                                               .subscribe(new UpgradeCompleteSubscriber());
            return strategy;
        }
    }

    private class PendingStrategy implements Strategy {
        @Override
        public Strategy proceed() {
            if (context == StrategyContext.USER_CLICKED_START) {
                view.setStartButtonWaiting();
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
                view.setStartButtonRetry();
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
                view.setStartButtonRetry();
                view.showErrorDialog(activity.getSupportFragmentManager());
                return retryLoadingStrategy();
            } else {
                return this;
            }
        }
    }
}
