package com.soundcloud.android.main;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.deeplinks.IntentResolver;
import com.soundcloud.android.feedback.Feedback;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.snackbar.FeedbackController;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

import android.support.annotation.CallSuper;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NavigationDelegate {

    private final IntentResolver intentResolver;
    private final PublishSubject<NavigationTarget> subject = PublishSubject.create();

    @Inject
    NavigationDelegate(IntentResolver intentResolver) {
        this.intentResolver = intentResolver;
    }

    public void navigateTo(NavigationTarget navigationTarget) {
        subject.onNext(navigationTarget);
    }

    public Observable<NavigationResult> listenToNavigation() {
        return subject.flatMapSingle(intentResolver::resolveNavigationResult);
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
