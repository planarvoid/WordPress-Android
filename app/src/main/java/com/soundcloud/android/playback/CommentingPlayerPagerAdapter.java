package com.soundcloud.android.playback;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.playback.views.PlayerTrackView;
import com.soundcloud.android.track.TrackOperations;

import javax.inject.Inject;

/**
 * Extends the basic player pager adapter to include storing the commenting position. Since views will be recycled,
 * this commenting state needs to be persisted outside of that layer
 *
 * NOTE: I don't expect this will be used in the new player. If we need similar functionality, this should be looked at harder
 */
public class CommentingPlayerPagerAdapter extends PlayerTrackPagerAdapter{

    private static final int NOT_SET = -1;
    private int commentingPosition = NOT_SET;

    @Inject
    public CommentingPlayerPagerAdapter(TrackOperations trackOperations, PlaybackStateProvider stateProvider,
                                        ViewFactory playerViewFactory) {
        super(trackOperations, stateProvider, playerViewFactory);
    }

    public int getCommentingPosition() {
        return commentingPosition;
    }

    public void clearCommentingPosition(boolean animated) {
        commentingPosition = NOT_SET;
        for (PlayerTrackView playerTrackView : getPlayerTrackViews()) {
            playerTrackView.setCommentMode(false, animated);
        }
    }

    public void setCommentingPosition(int commentingPosition, boolean animated) {
        if (commentingPosition >= 0 && commentingPosition < playQueue.size()){
            this.commentingPosition = commentingPosition;
            PlayerTrackView commentingView = getPlayerTrackViewByPosition(commentingPosition);
            for (PlayerTrackView ptv : getPlayerTrackViews()) {
                if (ptv != commentingView) {
                    ptv.setCommentMode(false);
                } else {
                    ptv.setCommentMode(true, animated);
                }
            }
        } else throw new IllegalArgumentException("Invalid commenting position " + commentingPosition);
    }

    @Override
    protected void setTrackOnPlayerTrackView(Track track, PlayerTrackView playerQueueView, Integer queuePosition) {
        super.setTrackOnPlayerTrackView(track, playerQueueView, queuePosition);
        playerQueueView.setCommentMode(commentingPosition == queuePosition, false);
    }
}
