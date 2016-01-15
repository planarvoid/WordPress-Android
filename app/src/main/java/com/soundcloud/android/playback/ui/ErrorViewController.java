package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.ui.TrackPagePresenter.TrackPageHolder;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stations.StartStationPresenter;

import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

@AutoFactory(allowSubclasses = true)
class ErrorViewController {

    enum ErrorState {
        FAILED, BLOCKED, UNPLAYABLE;
    }

    private final View trackView;

    private final TrackPageHolder holder;
    private final ViewStub errorStub;
    private final StartStationPresenter startStationPresenter;

    private Urn urn;
    private View errorLayout;

    @Nullable private ErrorState currentError;

    public ErrorViewController(@Provided StartStationPresenter startStationPresenter, View trackView) {
        this.startStationPresenter = startStationPresenter;
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

        setupStartStationButton(error);
    }

    public void setUrn(Urn urn) {
        this.urn = urn;
    }

    private void setupStartStationButton(ErrorState error) {
        final Button stationButton = (Button) errorLayout.findViewById(R.id.playback_error_station_button);
        if (error == ErrorState.BLOCKED){
            setupStartStationButton(stationButton);
        } else {
            stationButton.setVisibility(View.GONE);
        }
    }

    private void setupStartStationButton(Button stationButton) {
        stationButton.setVisibility(View.VISIBLE);
        stationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startStationPresenter.startStation(errorLayout.getContext(), Urn.forTrackStation(urn.getNumericId()));
            }
        });
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
}
