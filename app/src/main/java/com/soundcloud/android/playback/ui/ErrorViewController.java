package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.ui.TrackPagePresenter.TrackPageHolder;

import com.soundcloud.android.R;

import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

class ErrorViewController {

    enum ErrorState {
        FAILED, BLOCKED, UNPLAYABLE
    }

    private final View trackView;
    private final TrackPageHolder holder;
    private final ViewStub errorStub;

    private View errorLayout;

    @Nullable private ErrorState currentError;

    public ErrorViewController(View trackView) {
        this.trackView = trackView;
        this.holder = (TrackPageHolder) trackView.getTag();
        this.errorStub = (ViewStub) trackView.findViewById(R.id.track_page_error_stub);
    }

    public boolean isShowingError() {
        return currentError != null;
    }

    public void showError(ErrorState error) {
        this.currentError = error;
        holder.waveformController.hide();
        setGone(holder.hideOnErrorViews);

        setupErrorLayout();

        final TextView message = (TextView) errorLayout.findViewById(R.id.playback_error_reason);
        message.setText(getMessageFromError(error));

        final ImageView errorImage = (ImageView) errorLayout.findViewById(R.id.playback_error_image);
        errorImage.setImageResource(getImageFromError(error));

    }

    private void setupErrorLayout() {
        errorLayout = trackView.findViewById(R.id.track_page_error);
        if (errorLayout == null) {
            errorLayout = errorStub.inflate();

        } else {
            errorLayout.setVisibility(View.VISIBLE);
        }
    }

    private int getMessageFromError(ErrorState error) {
        switch (error) {
            case FAILED:
                return R.string.playback_error_connection;
            case BLOCKED:
                return R.string.playback_error_blocked;
            default:
                return R.string.playback_error_unable_to_play;
        }
    }

    private int getImageFromError(ErrorState error) {
        switch (error) {
            case BLOCKED:
                return R.drawable.player_error_geoblock;
            default:
                return R.drawable.player_error;
        }
    }

    public void hideError() {
        if (isShowingError()) {
            holder.waveformController.show();
            setVisible(holder.hideOnErrorViews);

            errorLayout.setVisibility(View.GONE);
            currentError = null;
        }
    }

    public void hideNonBlockedErrors() {
        if (currentError != ErrorState.BLOCKED) {
            hideError();
        }
    }

    private void setGone(Iterable<View> views) {
        for (View v : views) {
            v.clearAnimation();
            v.setVisibility(View.GONE);
        }
    }

    private void setVisible(Iterable<View> views) {
        for (View v : views) {
            v.setVisibility(View.VISIBLE);
            v.setAlpha(1f);
        }
    }

    public static class Factory {

        @Inject
        Factory() {}

        public ErrorViewController create(View trackView) {
            return new ErrorViewController(trackView);
        }
    }

}
