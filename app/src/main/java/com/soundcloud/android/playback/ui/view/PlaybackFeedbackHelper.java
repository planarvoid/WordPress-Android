package com.soundcloud.android.playback.ui.view;

import com.soundcloud.android.R;
import com.soundcloud.android.feedback.Feedback;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.view.snackbar.FeedbackController;

import javax.inject.Inject;


public class PlaybackFeedbackHelper {

    private final PlaySessionStateProvider playSessionStateProvider;
    private final FeedbackController feedbackController;

    @Inject
    public PlaybackFeedbackHelper(PlaySessionStateProvider playSessionStateProvider,
                                  FeedbackController feedbackController) {
        this.playSessionStateProvider = playSessionStateProvider;
        this.feedbackController = feedbackController;
    }

    public void showFeedbackOnPlaybackError(PlaybackResult.ErrorReason errorReason) {
        switch (errorReason) {
            case UNSKIPPABLE:
                showUnskippableAdFeedback();
                break;
            case TRACK_UNAVAILABLE_OFFLINE:
                showTrackUnavailableOfflineFeedback();
                break;
            case MISSING_PLAYABLE_TRACKS:
                showMissingPlayableTracksFeedback();
                break;
            case TRACK_UNAVAILABLE_CAST:
                showUnableToCastFeedback();
                break;
            default:
                throw new IllegalStateException("Unknown error reason: " + errorReason);
        }
    }

    public void showUnskippableAdFeedback() {
        final int stringId = playSessionStateProvider.isPlaying()
                ? R.string.ads_ad_in_progress
                : R.string.ads_resume_playing_ad_to_continue;
        feedbackController.showFeedback(Feedback.create(stringId));
    }

    public void showConcurrentStreamingStoppedFeedback() {
        feedbackController.showFeedback(Feedback.create(R.string.concurrent_streaming_stopped));
    }

    private void showTrackUnavailableOfflineFeedback() {
        feedbackController.showFeedback(Feedback.create(R.string.offline_track_not_available));
    }

    private void showMissingPlayableTracksFeedback() {
        feedbackController.showFeedback(Feedback.create(R.string.playback_missing_playable_tracks));
    }

    private void showUnableToCastFeedback() {
        feedbackController.showFeedback(Feedback.create(R.string.cast_unable_play_track));
    }
}
