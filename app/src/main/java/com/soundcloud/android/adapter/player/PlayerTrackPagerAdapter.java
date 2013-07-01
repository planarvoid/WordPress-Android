package com.soundcloud.android.adapter.player;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.adapter.BasePagerAdapter;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.playback.PlayQueueItem;
import com.soundcloud.android.service.playback.PlayQueueManager;
import com.soundcloud.android.view.play.PlayerTrackView;

import android.view.View;
import android.view.ViewGroup;

import java.util.Set;

public class PlayerTrackPagerAdapter extends BasePagerAdapter<PlayQueueItem> {

    private final ScPlayer mPlayer;
    private PlayQueueManager mPlayQueueManager;
    private int mCommentingPosition;

    private final BiMap<PlayerTrackView, Integer> mPlayerViewsById = HashBiMap.create(3);

    public PlayerTrackPagerAdapter(ScPlayer player) {
        mPlayer = player;
    }

    public Set<PlayerTrackView> getPlayerTrackViews() {
        return mPlayerViewsById.keySet();
    }

    public int getCommentingPosition() {
        return mCommentingPosition;
    }

    public void onConnected() {
        for (PlayerTrackView ptv : getPlayerTrackViews()) {
            ptv.onDestroy();
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

    public void setCommentingPosition(int commentingPosition) {
        mCommentingPosition = commentingPosition;
        getPlayerTrackViewByPosition(commentingPosition).setCommentMode(true);
    }

    private PlayQueueManager getPlayQueueManager(){
        if (mPlayQueueManager == null) mPlayQueueManager = CloudPlaybackService.getPlaylistManager();
        return mPlayQueueManager;
    }

    @Override
    public int getCount() {
        final PlayQueueManager playQueueManager = getPlayQueueManager();
        return playQueueManager == null ? 0 : playQueueManager.length();
    }

    public void clearCommentingPosition() {
        mCommentingPosition = -1;
        for (PlayerTrackView playerTrackView : mPlayerViewsById.keySet()){
            playerTrackView.setCommentMode(false);
        }
    }

    @Override
    protected PlayQueueItem getItem(int position) {
        final PlayQueueManager playQueueManager = getPlayQueueManager();
        final PlayQueueItem playQueueItem = playQueueManager == null ? null : mPlayQueueManager.getPlayQueueItem(position);
        playQueueItem.setIsInCommentingMode(mCommentingPosition == position);
        return playQueueItem;
    }

    @Override
    protected View getView(PlayQueueItem dataItem, View convertView, ViewGroup parent) {
        if (convertView == null){
            convertView = new PlayerTrackView(mPlayer);
        }

        final PlayerTrackView playerTrackView = (PlayerTrackView) convertView;
        mPlayerViewsById.forcePut(playerTrackView, dataItem.getPlayQueuePosition());

        //TODO consolidate these calls
        playerTrackView.setPlayQueueItem(dataItem);
        playerTrackView.setCommentMode(mCommentingPosition == dataItem.getPlayQueuePosition());
        playerTrackView.setOnScreen(true);
        return convertView;
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
