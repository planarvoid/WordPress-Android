package com.soundcloud.android.adapter.player;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.BasePagerAdapter;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.playback.PlayQueueItem;
import com.soundcloud.android.service.playback.PlayQueueManager;
import com.soundcloud.android.view.play.PlayerTrackView;

import android.view.View;
import android.view.ViewGroup;

import java.util.Set;

public class PlayerTrackPagerAdapter extends BasePagerAdapter<PlayQueueItem> {

    private PlayQueueManager mPlayQueueManager;
    private int mCommentingPosition;

    private final BiMap<PlayerTrackView, Integer> mPlayerViewsById = HashBiMap.create(3);
    private Track mPlaceholderTrack;

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
        getPlayerTrackViewByPosition(commentingPosition).setCommentMode(true, animated);
    }

    public void setPlaceholderTrack(Track displayTrack) {
        mPlaceholderTrack = displayTrack;
    }

    private PlayQueueManager getPlayQueueManager(){
        if (mPlayQueueManager == null) mPlayQueueManager = CloudPlaybackService.getPlaylistManager();
        return mPlayQueueManager;
    }

    @Override
    public int getCount() {
        if (mPlaceholderTrack != null){
            return 1;
        } else {
            final PlayQueueManager playQueueManager = getPlayQueueManager();
            return playQueueManager == null ? 0 : playQueueManager.length();
        }
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
            final PlayQueueManager playQueueManager = getPlayQueueManager();
            return playQueueManager == null ? null : mPlayQueueManager.getPlayQueueItem(position);
        }
    }

    @Override
    protected View getView(PlayQueueItem dataItem, View convertView, ViewGroup parent) {

        final PlayerTrackView playerTrackView = convertView != null ? (PlayerTrackView) convertView :
                (PlayerTrackView) View.inflate(parent.getContext(), R.layout.player_track_view, null);

        mPlayerViewsById.forcePut(playerTrackView, dataItem.getPlayQueuePosition());

        //TODO consolidate these calls
        playerTrackView.setPlayQueueItem(dataItem);
        playerTrackView.setCommentMode(mCommentingPosition == dataItem.getPlayQueuePosition());
        playerTrackView.setOnScreen(true);
        return playerTrackView;
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
