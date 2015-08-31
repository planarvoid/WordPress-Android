package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.ui.TrackPagePresenter.TrackPageHolder;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.Player;

import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewStub;
import android.widget.TextView;

import javax.inject.Inject;

class ErrorViewController {

    private final View trackView;
    private final TrackPageHolder holder;
    private final ViewStub errorStub;

    private View errorLayout;

    @Nullable private Player.Reason currentError;

    public ErrorViewController(View trackView) {
        this.trackView = trackView;
        this.holder = (TrackPageHolder) trackView.getTag();
        this.errorStub = (ViewStub) trackView.findViewById(R.id.track_page_error_stub);
    }

    public boolean isShowingError() {
        return currentError != null;
    }

    public void showError(Player.Reason reason) {
        this.currentError = reason;
        holder.waveformController.hide();
        setGone(holder.hideOnErrorViews);

        setupErrorLayout();
        setMessageFromReason(reason);
    }

    private void setupErrorLayout() {
        errorLayout = trackView.findViewById(R.id.track_page_error);
        if (errorLayout == null) {
            errorLayout = errorStub.inflate();
        } else {
            errorLayout.setVisibility(View.VISIBLE);
        }
    }

    private void setMessageFromReason(Player.Reason reason) {
        final TextView message = (TextView) errorLayout.findViewById(R.id.playback_error_reason);
        message.setText(reason == Player.Reason.ERROR_FAILED
                ? R.string.playback_error_connection
                : R.string.playback_error_unable_to_play);
    }

    public void hideError() {
        if (isShowingError()) {
            holder.waveformController.show();
            setVisible(holder.hideOnErrorViews);

            errorLayout.setVisibility(View.GONE);
            currentError = null;
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
