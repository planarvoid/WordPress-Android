package com.soundcloud.android.navigation;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.feedback.Feedback;
import com.soundcloud.android.playback.ExpandPlayerSingleObserver;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.snackbar.FeedbackController;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.CallSuper;
import android.support.v4.app.TaskStackBuilder;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Resolves the provided {@link NavigationTarget} and pushes the resulting {@link NavigationResult} to the observable from {@link #listenToNavigation()}.
 * This kind of navigation should be used instead of {@link NavigationExecutor}.
 */
@Singleton
public class Navigator {

    private final NavigationResolver navigationResolver;
    private final BehaviorSubject<NavigationTarget> subject = BehaviorSubject.create();
    private NavigationTarget lastNavigationTarget;

    @Inject
    Navigator(NavigationResolver navigationResolver) {
        this.navigationResolver = navigationResolver;
    }

    public void navigateTo(NavigationTarget navigationTarget) {
        subject.onNext(navigationTarget);
    }

    public Observable<NavigationResult> listenToNavigation() {
        return subject.filter(currentTarget -> currentTarget != lastNavigationTarget).flatMapSingle(this::performNavigation);
    }

    private Single<NavigationResult> performNavigation(NavigationTarget navigationTarget) {
        lastNavigationTarget = navigationTarget;
        return navigationResolver.resolveNavigationResult(navigationTarget);
    }

    @AutoFactory
    public static class Observer extends DefaultObserver<NavigationResult> {

        private final FeedbackController feedbackController;
        private final ExpandPlayerSingleObserver expandPlayerObserver;
        private final Activity activity;

        @Inject
        Observer(Activity activity,
                 @Provided FeedbackController feedbackController,
                 @Provided ExpandPlayerSingleObserver expandPlayerObserver) {
            this.activity = activity;
            this.feedbackController = feedbackController;
            this.expandPlayerObserver = expandPlayerObserver;
        }

        @Override
        @CallSuper
        public void onNext(NavigationResult result) {
            try {
                if (!result.isSuccess()) {
                    feedbackController.showFeedback(Feedback.create(R.string.error_unknown_navigation));
                    ErrorUtils.handleSilentException("Navigation failed: " + result.target(), new IllegalArgumentException("Navigation failed for target: " + result.target()));
                    return;
                }

                result.toastMessage().ifPresent(message -> AndroidUtils.showToast(activity, message));

                result.playbackResult()
                      .filter(PlaybackResult::isSuccess)
                      .ifPresent(expandPlayerObserver::onSuccess);

                if (!result.intent().isPresent()) {
                    return;
                }

                if (!result.taskStack().isEmpty()) {
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(activity);
                    for (Intent intent : result.taskStack()) {
                        stackBuilder.addNextIntent(intent);
                    }
                    stackBuilder.addNextIntent(result.intent().get());
                    stackBuilder.startActivities();
                    return;
                }

                activity.startActivity(result.intent().get());
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
