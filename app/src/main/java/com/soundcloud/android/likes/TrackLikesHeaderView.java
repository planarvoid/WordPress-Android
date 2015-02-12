package com.soundcloud.android.likes;

import butterknife.ButterKnife;
import butterknife.InjectView;
import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.AnimUtils;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import javax.inject.Inject;

class TrackLikesHeaderView {

    private View headerView;
    @InjectView(R.id.header_text) TextView headerText;
    @InjectView(R.id.shuffle_btn) Button shuffleButton;
    @InjectView(R.id.sync_state) ImageView syncState;

    private State state = State.DEFAULT;
    private int trackCount;

    private enum State {
        DEFAULT, SYNCING, DOWNLOADED
    }

    @Inject
    TrackLikesHeaderView() {
        // For Dagger
    }

    @VisibleForTesting
    View getHeaderView() {
        return headerView;
    }

    void onViewCreated(View view) {
        headerView = View.inflate(view.getContext(), R.layout.track_likes_header, null);
        ButterKnife.inject(this, headerView);
    }

    void onDestroyView() {
        headerView = null;
    }

    public void attachToList(ListView listView) {
        listView.addHeaderView(headerView);
    }

    void setOnShuffleButtonClick(View.OnClickListener listener) {
        shuffleButton.setOnClickListener(listener);
    }

    void showDefaultState() {
        state = State.DEFAULT;
        syncState.clearAnimation();

        syncState.setVisibility(View.GONE);
        syncState.setImageDrawable(null);

        updateTrackCount(trackCount);
    }

    void showSyncingState() {
        state = State.SYNCING;
        syncState.clearAnimation();

        syncState.setVisibility(View.VISIBLE);
        syncState.setImageResource(R.drawable.header_syncing);
        headerText.setText(headerView.getContext().getString(R.string.offline_update_in_progress));

        AnimUtils.runSpinClockwiseAnimationOn(syncState.getContext(), syncState);
    }

    void showDownloadedState() {
        state = State.DOWNLOADED;
        syncState.clearAnimation();

        syncState.setVisibility(View.VISIBLE);
        syncState.setImageResource(R.drawable.header_downloaded);

        updateTrackCount(trackCount);
    }

    void updateTrackCount(int trackCount) {
        this.trackCount = trackCount;
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
