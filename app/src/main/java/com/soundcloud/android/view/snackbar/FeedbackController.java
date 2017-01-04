package com.soundcloud.android.view.snackbar;

import com.soundcloud.android.R;
import com.soundcloud.android.feedback.Feedback;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.java.optional.Optional;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FeedbackController {

    private final PlayerSnackBarWrapper playerSnackBarWrapper;
    private final TopSnackBarWrapper topSnackBarWrapper;

    private Optional<SlidingPlayerController> playerControllerOpt = Optional.absent();
    private Optional<View> snackBarHolderOpt = Optional.absent();


    @Inject
    public FeedbackController(PlayerSnackBarWrapper playerSnackBarWrapper,
                              TopSnackBarWrapper topSnackBarWrapper) {
        this.playerSnackBarWrapper = playerSnackBarWrapper;
        this.topSnackBarWrapper = topSnackBarWrapper;
    }

    public void register(FragmentActivity activity, SlidingPlayerController playerController) {
        this.snackBarHolderOpt = Optional.fromNullable(getActivitySnackBarHolder(activity));
        this.playerControllerOpt = Optional.of(playerController);
    }

    public void clear() {
        snackBarHolderOpt = Optional.absent();
        playerControllerOpt = Optional.absent();
    }

    public void showFeedback(Feedback feedback) {
        if (playerControllerOpt.isPresent()) {
            SlidingPlayerController playerController = playerControllerOpt.get();
            if (playerController.isExpanded()) {
                playerSnackBarWrapper.show(playerController.getSnackbarHolder(), feedback);
            } else if (snackBarHolderOpt.isPresent()) {
                topSnackBarWrapper.show(snackBarHolderOpt.get(), feedback);
            }
        }
    }

    private View getActivitySnackBarHolder(Activity activity) {
        final View snackBarViewHolder = activity.findViewById(R.id.snackbar_holder);
        return (snackBarViewHolder != null) ? snackBarViewHolder : activity.findViewById(R.id.container);
    }

}
