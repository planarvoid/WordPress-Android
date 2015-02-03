package com.soundcloud.android.likes;

import butterknife.ButterKnife;
import butterknife.InjectView;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.android.view.HeaderViewPresenter;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

public class TrackLikesHeaderPresenter extends HeaderViewPresenter {

    private View headerView;
    @InjectView(R.id.header_text) TextView headerText;
    @InjectView(R.id.shuffle_btn) Button shuffleButton;
    @InjectView(R.id.sync_state) ImageView syncState;

    private State state = State.DEFAULT;
    private enum State {
        DEFAULT, SYNCING, DOWNLOADED
    }

    @Inject
    public TrackLikesHeaderPresenter() {
        // For Dagger
    }

    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        headerView = view.inflate(view.getContext(), R.layout.track_likes_header, null);
        ButterKnife.inject(this, headerView);
    }

    public void onDestroyView() {
        headerView = null;
    }

    @Override
    public View getHeaderView() {
        return headerView;
    }

    public void setOnShuffleButtonClick(View.OnClickListener listener) {
        shuffleButton.setOnClickListener(listener);
    }

    public void showDefaultState(int trackCount) {
        state = State.DEFAULT;
        syncState.clearAnimation();

        syncState.setVisibility(View.GONE);
        syncState.setImageDrawable(null);

        updateTrackCount(trackCount);
    }

    public void showSyncingState() {
        state = State.SYNCING;
        syncState.clearAnimation();

        syncState.setVisibility(View.VISIBLE);
        syncState.setImageResource(R.drawable.header_syncing);
        headerText.setText(headerView.getContext().getString(R.string.offline_sync_in_progress));

        AnimUtils.runSpinClockwiseAnimationOn(syncState.getContext(), syncState);
    }

    public void showDownloadedState(int trackCount) {
        state = State.DOWNLOADED;
        syncState.clearAnimation();

        syncState.setVisibility(View.VISIBLE);
        syncState.setImageResource(R.drawable.header_downloaded);

        updateTrackCount(trackCount);
    }

    public void updateTrackCount(int trackCount) {
        if (state != State.SYNCING) {
            updateHeaderTextWithTrackCount(trackCount);
        }
        updateShuffleButton(trackCount);
    }

    private void updateHeaderTextWithTrackCount(int likedTracks) {
        if (likedTracks == 0) {
            headerText.setText(headerView.getContext().getString(R.string.number_of_liked_tracks_you_liked_zero));
        } else {
            headerText.setText(headerView.getContext().getResources()
                    .getQuantityString(R.plurals.number_of_liked_tracks_you_liked, likedTracks, likedTracks));
        }
    }

    private void updateShuffleButton(int likedTracks) {
        if (likedTracks <= 1) {
            shuffleButton.setVisibility(View.GONE);
            shuffleButton.setEnabled(false);
        } else {
            shuffleButton.setVisibility(View.VISIBLE);
            shuffleButton.setEnabled(true);
        }
    }

}
