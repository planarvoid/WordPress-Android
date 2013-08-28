package com.soundcloud.android.view.play;

import com.soundcloud.android.R;
import com.soundcloud.android.service.playback.PlayQueueItem;
import com.soundcloud.android.view.EmptyListView;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.FrameLayout;

public class PlayerQueueView extends FrameLayout {

    private EmptyListView mEmptyView;
    private PlayerTrackView mTrackView;

    public PlayerQueueView(Context context) {
        super(context);
    }

    public void setPlayQueueItem(PlayQueueItem playQueueItem, boolean inCommentingMode){
        if (playQueueItem == PlayQueueItem.EMPTY){
            hideTrackView();

            if (mEmptyView == null){
                mEmptyView = new EmptyListView(getContext());
                mEmptyView.setBackgroundColor(Color.WHITE);
            }
            mEmptyView.setStatus(EmptyListView.Status.WAITING);

        } else {
            hideEmotyView();

            if (mTrackView == null) {
                mTrackView = createPlayerTrackView(getContext());
            }
            mTrackView.setPlayQueueItem(playQueueItem);
            mTrackView.setCommentMode(inCommentingMode);

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

    private void hideEmotyView() {
        if (mEmptyView != null) {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    protected PlayerTrackView createPlayerTrackView(Context context) {
            return (PlayerTrackView) View.inflate(context, R.layout.player_track_view, null);
        }



}
