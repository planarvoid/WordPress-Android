package com.soundcloud.android.likes;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperiment;
import com.soundcloud.android.feedback.Feedback;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.snackbar.FeedbackController;

import android.content.Context;
import android.widget.Toast;

public class LikeToggleSubscriber extends DefaultSubscriber<Object> {
    private final Context context;
    private final boolean likeStatus;
    private final ChangeLikeToSaveExperiment changeLikeToSaveExperiment;
    private final FeedbackController feedbackController;

    public LikeToggleSubscriber(Context context,
                                boolean likeStatus,
                                ChangeLikeToSaveExperiment changeLikeToSaveExperiment,
                                FeedbackController feedbackController) {
        this.context = context;
        this.likeStatus = likeStatus;
        this.changeLikeToSaveExperiment = changeLikeToSaveExperiment;
        this.feedbackController = feedbackController;
    }

    @Override
    public void onNext(Object ignored) {
        if (changeLikeToSaveExperiment.isEnabled()) {
            showAddSnackbar();
        } else {
            showLikeToast();
        }
    }

    private void showAddSnackbar() {
        feedbackController.showFeedback(Feedback.create(likeStatus
                                                        ? R.string.add_snackbar_overflow_action
                                                        : R.string.remove_snackbar_overflow_action));
    }

    private void showLikeToast() {
        Toast.makeText(context,
                       likeStatus
                       ? R.string.like_toast_overflow_action
                       : R.string.unlike_toast_overflow_action,
                       Toast.LENGTH_SHORT)
             .show();
    }

    @Override
    public void onError(Throwable e) {
        if (changeLikeToSaveExperiment.isEnabled()) {
            feedbackController.showFeedback(Feedback.create(R.string.add_error_snackbar_overflow_action));
        } else {
            Toast.makeText(context, R.string.like_error_toast_overflow_action, Toast.LENGTH_LONG).show();
        }
    }
}
