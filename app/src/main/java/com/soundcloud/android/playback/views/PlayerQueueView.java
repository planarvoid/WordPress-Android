package com.soundcloud.android.playback.views;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.view.EmptyListView;
import rx.Observable;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.FrameLayout;

public class PlayerQueueView extends FrameLayout {

    private EmptyListView mEmptyView;
    private PlayerTrackView mTrackView;

    public PlayerQueueView(Context context) {
        super(context);
    }

    public void showTrack(Observable<Track> trackObservable, int queuePosition, boolean inCommentingMode) {
        // TODO, replace these with viewStubs
        showTrackView();
        mTrackView.setPlayQueueItem(trackObservable, queuePosition);
        mTrackView.setCommentMode(inCommentingMode);
        mTrackView.setOnScreen(true);
    }

    public void showEmptyViewWithState(PlaybackOperations.AppendState appendState) {
        showEmptyView();
        if (appendState == PlaybackOperations.AppendState.LOADING) {
            mEmptyView.setStatus(EmptyListView.Status.WAITING);
        } else if (appendState == PlaybackOperations.AppendState.ERROR) {
            mEmptyView.setStatus(EmptyListView.Status.ERROR);
        } else {
            mEmptyView.setStatus(EmptyListView.Status.OK);
        }
    }

    private void showEmptyView() {
        hideTrackView();
        if (mEmptyView == null) {
            mEmptyView = createEmptyListView(getContext());
        }
        mEmptyView.setOnRetryListener(new EmptyListView.RetryListener() {
            @Override
            public void onEmptyViewRetry() {
                getContext().startService(new Intent(PlaybackService.Actions.RETRY_RELATED_TRACKS));
            }
        });
        if (mEmptyView.getParent() != this) {
            addView(mEmptyView);
        }
    }

    private void showTrackView() {
        hideEmptyView();
        if (mTrackView == null) {
            mTrackView = createPlayerTrackView(getContext());
        }
        if (mTrackView.getParent() != this) {
            addView(mTrackView);
        }
    }

    public boolean isShowingPlayerTrackView() {
        return mTrackView != null && mTrackView.getVisibility() == VISIBLE;
    }

    public PlayerTrackView getTrackView() {
        return mTrackView;
    }

    private void hideTrackView() {
        if (mTrackView != null) {
            mTrackView.setVisibility(View.GONE);
        }
    }

    private void hideEmptyView() {
        if (mEmptyView != null) {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    protected PlayerTrackView createPlayerTrackView(Context context) {
        return (PlayerTrackView) View.inflate(context, R.layout.player_track_view, null);
    }

    protected EmptyListView createEmptyListView(Context context) {
        EmptyListView emptyListView = new EmptyListView(context, R.layout.empty_player_track);
        emptyListView.setBackgroundColor(Color.WHITE);
        emptyListView.setMessageText(R.string.player_no_recommended_tracks);
        return emptyListView;
    }


}
