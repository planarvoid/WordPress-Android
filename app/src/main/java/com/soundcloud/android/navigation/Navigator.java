package com.soundcloud.android.navigation;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.feedback.Feedback;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.snackbar.FeedbackController;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

import android.support.annotation.CallSuper;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Resolves the provided {@link NavigationTarget} and pushes the resulting {@link NavigationResult} to the observable from {@link #listenToNavigation()}.
 * This kind of navigation should be used instead of {@link NavigationExecutor}.
 */
@Singleton
public class Navigator {

    private final NavigationResolver navigationResolver;
    private final PublishSubject<NavigationTarget> subject = PublishSubject.create();

    @Inject
    Navigator(NavigationResolver navigationResolver) {
        this.navigationResolver = navigationResolver;
    }

    public void navigateTo(NavigationTarget navigationTarget) {
        subject.onNext(navigationTarget);
    }

    public Observable<NavigationResult> listenToNavigation() {
        return subject.flatMapSingle(navigationResolver::resolveNavigationResult);
    }

    @AutoFactory
    public static class Observer extends DefaultObserver<NavigationResult> {

        private final FeedbackController feedbackController;

        @Inject
        Observer(@Provided FeedbackController feedbackController) {
            this.feedbackController = feedbackController;
        }

        @Override
        @CallSuper
        public void onNext(NavigationResult result) {
            try {
                result.action().run();
            } catch (Exception e) {
                feedbackController.showFeedback(Feedback.create(R.string.error_unknown_navigation));
                ErrorUtils.handleSilentException("Navigation failed: " + result.target(), e);
            }
        }

        @Override
        public final void onError(Throwable t) {
            throw new IllegalStateException("Complete in Navigation Subscription. This should never happen since navigation won\'t work in the app anymore. Thus we\'ll force close the app.", t);
        }

        @Override
        public final void onComplete() {
            throw new IllegalStateException("Complete in Navigation Subscription. This should never happen since navigation won\'t work in the app anymore. Thus we\'ll force close the app.");
        }
    }
}
