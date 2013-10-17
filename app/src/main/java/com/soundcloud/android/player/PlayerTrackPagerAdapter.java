package com.soundcloud.android.player;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.BiMap;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashBiMap;
import com.soundcloud.android.adapter.BasePagerAdapter;
import com.soundcloud.android.dao.TrackStorage;
import com.soundcloud.android.service.playback.PlayQueue;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collection;

public class PlayerTrackPagerAdapter extends BasePagerAdapter<Long> {

    private int mCommentingPosition = -1;

    private final BiMap<PlayerQueueView, Integer> mQueueViewsByPosition = HashBiMap.create(3);

    private TrackStorage mTrackStorage;
    private PlayQueue mPlayQueue = PlayQueue.EMPTY;

    public PlayerTrackPagerAdapter() {
        this(new TrackStorage());
    }

    public PlayerTrackPagerAdapter(TrackStorage trackStorage) {
        mTrackStorage = trackStorage;
    }

    public Collection<PlayerTrackView> getPlayerTrackViews() {
        return Collections2.transform(Collections2.filter(mQueueViewsByPosition.keySet(), new Predicate<PlayerQueueView>() {
            @Override
            public boolean apply(PlayerQueueView input) {
                return input.isShowingPlayerTrackView();
            }
        }), new Function<PlayerQueueView, PlayerTrackView>() {
            @Override
            public PlayerTrackView apply(PlayerQueueView input) {
                return input.getTrackView();
            }
        });
    }

    public void setPlayQueue(PlayQueue playQueue) {
        mPlayQueue = playQueue;
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

    @Override
    public int getCount() {
        return shouldDisplayExtraItem() ? mPlayQueue.getCurrentTrackIds().size() + 1 : mPlayQueue.getCurrentTrackIds().size();
    }


    private boolean shouldDisplayExtraItem() {
        return mPlayQueue.isLoading() || mPlayQueue.lastLoadFailed() || mPlayQueue.lastLoadWasEmpty();
    }

    public void clearCommentingPosition(boolean animated) {
        mCommentingPosition = -1;
        for (PlayerTrackView playerTrackView : getPlayerTrackViews()){
            playerTrackView.setCommentMode(false, animated);
        }
    }

    @Override
    protected Long getItem(int position) {
        if (position >= mPlayQueue.getCurrentTrackIds().size()) {
            return -1L;
        } else {
            return mPlayQueue.getTrackIdAt(position);
        }
    }

    @Override
    protected View getView(Long id, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = createPlayerQueueView(parent.getContext());
        }

        final PlayerQueueView queueView = (PlayerQueueView) convertView;
        final int playQueuePosition = id == -1 ? mPlayQueue.getCurrentTrackIds().size() : mPlayQueue.getCurrentTrackIds().indexOf(id);

        queueView.setPlayQueueItem(id == -1 ? null : mTrackStorage.getTrack(id), playQueuePosition, mCommentingPosition == playQueuePosition);
        mQueueViewsByPosition.forcePut(queueView, playQueuePosition);
        return convertView;
    }

    @VisibleForTesting
    protected PlayerQueueView createPlayerQueueView(Context context) {
        return new PlayerQueueView(context);
    }

    @Override
    public int getItemPosition(Object object) {
        return ((Long) object) == -1 && !shouldDisplayExtraItem() ? POSITION_NONE : super.getItemPosition(object);
    }

    public PlayerTrackView getPlayerTrackViewById(long id){
        for (PlayerTrackView playerTrackView : getPlayerTrackViews()){
            if (playerTrackView.getTrackId() == id) return playerTrackView;
        }
        return null;
    }

    public PlayerTrackView getPlayerTrackViewByPosition(int position){
        final PlayerQueueView playerQueueView = mQueueViewsByPosition.inverse().get(position);
        if (playerQueueView != null){
            return playerQueueView.getTrackView();
        } else {
            // this is expected to happen, for instance if we try to refresh a play position that is not on screen
            return null;
        }
    }

    public void reloadEmptyView(){
        for (PlayerQueueView playerQueueView : mQueueViewsByPosition.keySet()){
            if (!playerQueueView.isShowingPlayerTrackView()){
                final Integer position = mQueueViewsByPosition.get(playerQueueView);
                final Long id = getItem(position);
                playerQueueView.setPlayQueueItem(id == -1 ? null : mTrackStorage.getTrack(id), position, mCommentingPosition == position);
            }
        }
    }

}
