package com.soundcloud.android.playback;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.BiMap;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashBiMap;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.BasePagerAdapter;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.PlayQueueView;
import com.soundcloud.android.playback.views.PlayerQueueView;
import com.soundcloud.android.playback.views.PlayerTrackView;
import com.soundcloud.android.track.TrackOperations;
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.Collection;

public class PlayerTrackPagerAdapter extends BasePagerAdapter<Long> {

    static final long EMPTY_VIEW_ID = -1;

    private int mCommentingPosition = -1;

    private final BiMap<PlayerQueueView, Integer> mQueueViewsByPosition = HashBiMap.create(3);
    private final TrackOperations mTrackOperations;

    private PlayQueueView mPlayQueue = PlayQueueView.EMPTY;

    private String mOriginScreen;

    @Inject
    public PlayerTrackPagerAdapter(TrackOperations trackOperations) {
        mTrackOperations = trackOperations;
        mOriginScreen = Screen.UNKNOWN.get();
    }

    public void setOriginScreen(String screen) {
        mOriginScreen = screen;
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

    public boolean setPlayQueueIfChanged(PlayQueueView playQueue) {
        final boolean changed = !mPlayQueue.equals(playQueue);
        if (changed) mPlayQueue = playQueue;
        return changed;
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
            if (ptv != commentingView) {
                ptv.setCommentMode(false);
            } else {
                ptv.setCommentMode(true, animated);
            }
        }
    }

    @Override
    public int getCount() {
        return shouldDisplayExtraItem() ? mPlayQueue.size() + 1 : mPlayQueue.size();
    }


    private boolean shouldDisplayExtraItem() {
        return mPlayQueue.isLoading() || mPlayQueue.lastLoadFailed() || mPlayQueue.lastLoadWasEmpty();
    }

    public void clearCommentingPosition(boolean animated) {
        mCommentingPosition = -1;
        for (PlayerTrackView playerTrackView : getPlayerTrackViews()) {
            playerTrackView.setCommentMode(false, animated);
        }
    }

    @Override
    protected Long getItem(int position) {
        if (position >= mPlayQueue.size()) {
            return EMPTY_VIEW_ID;
        } else {
            return mPlayQueue.getTrackIdAt(position);
        }
    }

    @Override
    protected View getView(Long id, View convertView, ViewGroup parent) {
        final Activity playerActivity = (Activity) parent.getContext();

        if (convertView == null) {
            convertView = createPlayerQueueView(playerActivity);
        }

        final PlayerQueueView queueView = (PlayerQueueView) convertView;
        final int playQueuePosition;

        if (id == EMPTY_VIEW_ID) {
            playQueuePosition = mPlayQueue.size();
            queueView.showEmptyViewWithState(mPlayQueue.getAppendState());
        } else {
            playQueuePosition = mPlayQueue.getPositionOfTrackId(id);
            queueView.showTrack(mTrackOperations.loadCompleteTrack(playerActivity, id),
                    playQueuePosition, mCommentingPosition == playQueuePosition, mOriginScreen);
        }
        mQueueViewsByPosition.forcePut(queueView, playQueuePosition);
        return convertView;
    }

    @VisibleForTesting
    protected PlayerQueueView createPlayerQueueView(Context context) {
        return new PlayerQueueView(context);
    }

    @Override
    public int getItemPosition(Object object) {
        long trackId = (Long) object;
        return trackId == Track.NOT_SET && !shouldDisplayExtraItem() ? POSITION_NONE : POSITION_UNCHANGED;
    }

    @Nullable
    public PlayerTrackView getPlayerTrackViewById(long id) {
        for (PlayerTrackView playerTrackView : getPlayerTrackViews()) {
            if (playerTrackView.getTrackId() == id) return playerTrackView;
        }
        return null;
    }

    @Nullable
    public PlayerTrackView getPlayerTrackViewByPosition(int position) {
        final PlayerQueueView playerQueueView = mQueueViewsByPosition.inverse().get(position);
        if (playerQueueView != null) {
            return playerQueueView.getTrackView();
        } else {
            // this is expected to happen, for instance if we try to refresh a play position that is not on screen
            return null;
        }
    }

    // since the pager layouts can show 2 completely different things (track views and the empty/loading view),
    // and since notifyDataSetChanged on ViewPager does not re-layout the page views, we have to do it manually
    public void reloadEmptyView(Activity playerActivity) {
        for (PlayerQueueView playerQueueView : mQueueViewsByPosition.keySet()) {
            if (playerQueueView.isShowingPlayerTrackView()) continue;

            // here we have an EmptyView, check if it still should be one, or convert it to a track view,
            // based on the latest adapter data
            final Integer position = mQueueViewsByPosition.get(playerQueueView);
            final Long id = getItem(position);
            if (id == EMPTY_VIEW_ID) {
                playerQueueView.showEmptyViewWithState(mPlayQueue.getAppendState());
            } else {
                playerQueueView.showTrack(mTrackOperations.loadCompleteTrack(playerActivity, id),
                        position, mCommentingPosition == position, mOriginScreen);
            }
        }
    }

}
