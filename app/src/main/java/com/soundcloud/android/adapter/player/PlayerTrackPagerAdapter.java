package com.soundcloud.android.adapter.player;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.BasePagerAdapter;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.playback.PlayQueueItem;
import com.soundcloud.android.service.playback.PlayQueueManager;
import com.soundcloud.android.view.EmptyListView;
import com.soundcloud.android.view.play.PlayerTrackView;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;

import java.util.Set;

public class PlayerTrackPagerAdapter extends BasePagerAdapter<PlayQueueItem> {

    private PlayQueueManager mPlayQueueManager;
    private int mCommentingPosition = -1;

    private final BiMap<PlayerTrackView, Integer> mPlayerViewsById = HashBiMap.create(3);
    private Track mPlaceholderTrack;

    public PlayerTrackPagerAdapter(PlayQueueManager playQueueManager) {
        this.mPlayQueueManager = playQueueManager;
    }

    public Set<PlayerTrackView> getPlayerTrackViews() {
        return mPlayerViewsById.keySet();
    }

    public int getCommentingPosition() {
        return mCommentingPosition;
    }

    public void onConnected() {
        for (PlayerTrackView ptv : getPlayerTrackViews()) {
            ptv.onDataConnected();
        }
    }

    public void onStop() {
        for (PlayerTrackView ptv : getPlayerTrackViews()) {
            ptv.onStop(true);
        }
    }

    public void onDestroy() {
        for (PlayerTrackView ptv : getPlayerTrackViews()) {
            ptv.onDestroy();
        }
    }

    public void setCommentingPosition(int commentingPosition, boolean animated) {
        mCommentingPosition = commentingPosition;
        PlayerTrackView commentingView = getPlayerTrackViewByPosition(commentingPosition);
        for (PlayerTrackView ptv : getPlayerTrackViews()) {
            if (ptv != commentingView){
                ptv.setCommentMode(false);
            } else {
                ptv.setCommentMode(true, animated);
            }
        }
    }

    public void setPlaceholderTrack(Track displayTrack) {
        mPlaceholderTrack = displayTrack;
    }

    @Override
    public int getCount() {
        if (mPlaceholderTrack != null){
            return 1;
        } else {
            return shouldDisplayExtraItem() ? mPlayQueueManager.length() + 1 : mPlayQueueManager.length();
        }
    }

    private boolean shouldDisplayExtraItem() {
        return mPlayQueueManager.isFetchingRelated() || mPlayQueueManager.lastRelatedFetchFailed();
    }

    public void clearCommentingPosition(boolean animated) {
        mCommentingPosition = -1;
        for (PlayerTrackView playerTrackView : mPlayerViewsById.keySet()){
            playerTrackView.setCommentMode(false, animated);
        }
    }

    @Override
    protected PlayQueueItem getItem(int position) {
        if (position == 0 && mPlaceholderTrack != null){
            return new PlayQueueItem(mPlaceholderTrack, 0);
        } else {
            if (position >= mPlayQueueManager.length()){
                return PlayQueueItem.EMPTY;
            } else {
                return mPlayQueueManager.getPlayQueueItem(position);
            }
        }
    }

    @Override
    protected View getView(PlayQueueItem dataItem, View convertView, ViewGroup parent) {
        if (dataItem == PlayQueueItem.EMPTY){
            if (convertView == null){
                convertView = createEmptyListView(parent.getContext());
            }
            setEmptyViewStatus((EmptyListView) convertView);
            return convertView;

        } else {
            PlayerTrackView playerTrackView;
            if (convertView == null) {
                playerTrackView = createPlayerTrackView(parent.getContext());
            } else {
                playerTrackView = (PlayerTrackView) convertView;
            }

            mPlayerViewsById.forcePut(playerTrackView, dataItem.getPlayQueuePosition());

            //TODO consolidate these calls
            playerTrackView.setPlayQueueItem(dataItem);
            playerTrackView.setCommentMode(mCommentingPosition == dataItem.getPlayQueuePosition());
            playerTrackView.setOnScreen(true);
            return playerTrackView;
        }
    }

    private void setEmptyViewStatus(EmptyListView emptyListView) {
        if (mPlayQueueManager.isFetchingRelated()){
            emptyListView.setStatus(EmptyListView.Status.WAITING);
        } else {
            emptyListView.setStatus(EmptyListView.Status.ERROR);
        }
    }

    @VisibleForTesting
    protected PlayerTrackView createPlayerTrackView(Context context) {
        return (PlayerTrackView) View.inflate(context, R.layout.player_track_view, null);
    }

    @VisibleForTesting
    protected EmptyListView createEmptyListView(Context context) {
        final EmptyListView emptyListView = new EmptyListView(context);
        // TODO, change the activity background to white and remove this
        emptyListView.setBackgroundColor(Color.WHITE);
        return emptyListView;
    }

    @Override
    public int getItemPosition(Object object) {
        return object == PlayQueueItem.EMPTY && !shouldDisplayExtraItem() ? POSITION_NONE : super.getItemPosition(object);
    }

    public PlayerTrackView getPlayerTrackViewById(long id){
        for (PlayerTrackView playerTrackView : mPlayerViewsById.keySet()){
            if (playerTrackView.getTrackId() == id) return playerTrackView;
        }
        return null;
    }

    public PlayerTrackView getPlayerTrackViewByPosition(int position){
        return mPlayerViewsById.inverse().get(position);
    }
}
