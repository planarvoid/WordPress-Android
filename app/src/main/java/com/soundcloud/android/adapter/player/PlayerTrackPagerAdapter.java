package com.soundcloud.android.adapter.player;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.BiMap;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashBiMap;
import com.soundcloud.android.adapter.BasePagerAdapter;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.playback.PlayQueueItem;
import com.soundcloud.android.service.playback.PlayQueueManager;
import com.soundcloud.android.view.EmptyListView;
import com.soundcloud.android.view.play.PlayerQueueView;
import com.soundcloud.android.view.play.PlayerTrackView;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collection;
import java.util.Set;

public class PlayerTrackPagerAdapter extends BasePagerAdapter<PlayQueueItem> {

    private PlayQueueManager mPlayQueueManager;
    private int mCommentingPosition = -1;

    private final BiMap<PlayerQueueView, Integer> mPlayerViewsById = HashBiMap.create(3);
    private Track mPlaceholderTrack;

    public PlayerTrackPagerAdapter(PlayQueueManager playQueueManager) {
        this.mPlayQueueManager = playQueueManager;
    }

    public Collection<PlayerTrackView> getPlayerTrackViews() {
        return Collections2.transform(Collections2.filter(mPlayerViewsById.keySet(), new Predicate<PlayerQueueView>() {
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
        for (PlayerTrackView playerTrackView : getPlayerTrackViews()){
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
        if (convertView == null) {
            convertView = createPlayerQueueView(parent.getContext());
        }

        ((PlayerQueueView) convertView).setPlayQueueItem(dataItem, mCommentingPosition == dataItem.getPlayQueuePosition());
        return convertView;
    }

    @VisibleForTesting
    protected PlayerQueueView createPlayerQueueView(Context context) {
        return new PlayerQueueView(context);
    }

    @Override
    public int getItemPosition(Object object) {
        return object == PlayQueueItem.EMPTY && !shouldDisplayExtraItem() ? POSITION_NONE : super.getItemPosition(object);
    }

    public PlayerTrackView getPlayerTrackViewById(long id){
        for (PlayerTrackView playerTrackView : getPlayerTrackViews()){
            if (playerTrackView.getTrackId() == id) return playerTrackView;
        }
        return null;
    }

    public PlayerTrackView getPlayerTrackViewByPosition(int position){
        return mPlayerViewsById.inverse().get(position).getTrackView();
    }
}
