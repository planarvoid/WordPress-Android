package com.soundcloud.android.view.play;

import com.soundcloud.android.R;
import com.soundcloud.android.service.playback.PlayQueueItem;
import com.soundcloud.android.service.playback.PlayQueueManager;
import com.soundcloud.android.view.EmptyListView;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.FrameLayout;

public class PlayerQueueView extends FrameLayout {

    private final PlayQueueManager mPlayQueueManager;
    private EmptyListView mEmptyView;
    private PlayerTrackView mTrackView;

    public PlayerQueueView(Context context, PlayQueueManager playQueueManager) {
        super(context);
        mPlayQueueManager = playQueueManager;
    }

    public void setPlayQueueItem(PlayQueueItem playQueueItem, boolean inCommentingMode){
        // TODO, replace these with viewStubs
        if (playQueueItem == PlayQueueItem.EMPTY){
            showEmptyView();
            mEmptyView.setStatus(mPlayQueueManager.isFetchingRelated() ?
                    EmptyListView.Status.WAITING : EmptyListView.Status.ERROR);

        } else {
            showTrackView();
            mTrackView.setPlayQueueItem(playQueueItem);
            mTrackView.setCommentMode(inCommentingMode);
            mTrackView.setOnScreen(true);
        }
    }

    private void showEmptyView() {
        hideTrackView();
        if (mEmptyView == null){
            mEmptyView = createEmptyListView(getContext());
        }
        if (mEmptyView.getParent() != this){
            addView(mEmptyView);
        }
    }

    private void showTrackView() {
        hideEmptyView();
        if (mTrackView == null) {
            mTrackView = createPlayerTrackView(getContext());
        }
        if (mTrackView.getParent() != this){
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
        if (mTrackView != null){
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
        EmptyListView emptyListView = new EmptyListView(context);
        emptyListView.setBackgroundColor(Color.WHITE);
        return emptyListView;
    }



}
