package com.soundcloud.android.player;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.view.EmptyListView;
import rx.Observable;

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

    public void setPlayQueueItem(Observable<Track> trackObservable, int queuePosition, boolean inCommentingMode){
        // TODO, replace these with viewStubs
        if (trackObservable == null){
            showEmptyView();
//            if (mPlayQueueManager.isLoading()){
//                mEmptyView.setStatus(EmptyListView.Status.WAITING);
//            } else if (mPlayQueueManager.lastLoadFailed()){
//                mEmptyView.setStatus(EmptyListView.Status.ERROR);
//            } else {
//                mEmptyView.setStatus(EmptyListView.Status.OK);
//            }


        } else {
            showTrackView();
            mTrackView.setPlayQueueItem(trackObservable, queuePosition);
            mTrackView.setCommentMode(inCommentingMode);
            mTrackView.setOnScreen(true);
        }
    }

    private void showEmptyView() {
        hideTrackView();
        if (mEmptyView == null){
            mEmptyView = createEmptyListView(getContext());
        }
        mEmptyView.setOnRetryListener(new EmptyListView.RetryListener() {
            @Override
            public void onEmptyViewRetry() {
                //mPlayQueueManager.retryRelatedTracksFetch();
            }
        });
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
        EmptyListView emptyListView = new EmptyListView(context, R.layout.empty_player_track);
        emptyListView.setBackgroundColor(Color.WHITE);
        emptyListView.setMessageText(R.string.player_no_recommended_tracks);
        return emptyListView;
    }



}
