package com.soundcloud.android.playback;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.playback.views.LegacyPlayerTrackView;
import com.soundcloud.android.track.TrackOperations;
import com.soundcloud.android.view.EmptyView;

import android.content.Context;
import android.graphics.Color;
import android.view.View;

import javax.inject.Inject;

/**
 * Extends the basic player pager adapter to include storing the commenting position. Since views will be recycled,
 * this commenting state needs to be persisted outside of that layer
 * <p/>
 * NOTE: I don't expect this will be used in the new player. If we need similar functionality, this should be looked at harder
 */
public class CommentingPlayerPagerAdapter extends PlayerTrackPagerAdapter<LegacyPlayerTrackView> {

    private static final int NOT_SET = -1;
    private int commentingPosition = NOT_SET;

    @Inject
    public CommentingPlayerPagerAdapter(TrackOperations trackOperations, PlaybackStateProvider stateProvider) {
        super(trackOperations, stateProvider);
    }

    @Override
    protected LegacyPlayerTrackView createPlayerTrackView(Context context) {
        return (LegacyPlayerTrackView) View.inflate(context, R.layout.player_track_view, null);
    }

    @Override
    protected EmptyView createEmptyListView(Context context) {
        EmptyView emptyView = new EmptyView(context, R.layout.empty_player_track);
        emptyView.setBackgroundColor(Color.WHITE);
        emptyView.setMessageText(R.string.player_no_recommended_tracks);
        return emptyView;
    }

    public int getCommentingPosition() {
        return commentingPosition;
    }

    public void clearCommentingPosition(boolean animated) {
        commentingPosition = NOT_SET;
        for (LegacyPlayerTrackView playerTrackView : getPlayerTrackViews()) {
            playerTrackView.setCommentMode(false, animated);
        }
    }

    public void setCommentingPosition(int commentingPosition, boolean animated) {
        if (commentingPosition >= 0 && commentingPosition < playQueue.size()) {
            this.commentingPosition = commentingPosition;
            LegacyPlayerTrackView commentingView = getPlayerTrackViewByPosition(commentingPosition);
            for (LegacyPlayerTrackView ptv : getPlayerTrackViews()) {
                if (ptv.equals(commentingView)) {
                    ptv.setCommentMode(true, animated);
                } else {
                    ptv.setCommentMode(false);
                }
            }
        } else {
            throw new IllegalArgumentException("Invalid commenting position " + commentingPosition);
        }
    }

    @Override
    protected void setTrackOnPlayerTrackView(Track track, LegacyPlayerTrackView playerTrackView, Integer queuePosition) {
        super.setTrackOnPlayerTrackView(track, playerTrackView, queuePosition);
        playerTrackView.setCommentMode(commentingPosition == queuePosition, false);
    }
}
