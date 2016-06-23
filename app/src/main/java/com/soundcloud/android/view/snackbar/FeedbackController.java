package com.soundcloud.android.view.snackbar;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.Feedback;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.lightcycle.ActivityLightCycleDispatcher;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.view.View;

@AutoFactory(allowSubclasses = true)
public class FeedbackController extends ActivityLightCycleDispatcher<FragmentActivity> {

    private final SlidingPlayerController playerController;
    private final PlayerSnackBarWrapper playerSnackBarWrapper;
    private final TopSnackBarWrapper topSnackBarWrapper;
    private final EventBus eventBus;
    private final FeatureFlags featureFlags;

    private Subscription subscription = RxUtils.invalidSubscription();
    private View snackBarHolder;


    public FeedbackController(SlidingPlayerController playerController,
                              @Provided PlayerSnackBarWrapper playerSnackBarWrapper,
                              @Provided TopSnackBarWrapper topSnackBarWrapper,
                              @Provided EventBus eventBus,
                              @Provided FeatureFlags featureFlags) {
        this.playerController = playerController;
        this.playerSnackBarWrapper = playerSnackBarWrapper;
        this.topSnackBarWrapper = topSnackBarWrapper;
        this.eventBus = eventBus;
        this.featureFlags = featureFlags;
    }

    @Override
    public void onResume(FragmentActivity activity) {
        super.onResume(activity);
        snackBarHolder = getActivitySnackBarHolder(activity);
        subscription = eventBus.subscribe(EventQueue.SHOW_FEEDBACK, new FeedbackSubscriber());
    }

    @Override
    public void onPause(FragmentActivity activity) {
        subscription.unsubscribe();
        snackBarHolder = null;
        super.onPause(activity);
    }

    private class FeedbackSubscriber extends DefaultSubscriber<Feedback> {
        @Override
        public void onNext(Feedback args) {
            if (playerController.isExpanded()) {
                playerSnackBarWrapper.show(playerController.getSnackbarHolder(), args);
            } else {
                topSnackBarWrapper.show(snackBarHolder, args);
            }
        }
    }

    public View getActivitySnackBarHolder(Activity activity) {
        final View snackBarViewHolder = activity.findViewById(R.id.snackbar_holder);
        return (snackBarViewHolder != null) ? snackBarViewHolder : activity.findViewById(R.id.container);
    }

}
