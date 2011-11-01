package com.soundcloud.android.view.quickaction;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.adapter.ITracklistAdapter;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.CloudUtils;

public class QuickTrackMenu extends QuickAction {

    ActionItem mPlayActionItem;
    ActionItem mProfileActionItem;
    ActionItem mShareActionItem;
    ActionItem mCommentActionItem;
    ActionItem mFavoriteActionItem;

    private Drawable mFavoriteDrawable;
    private Drawable mFavoritedDrawable;

    private Drawable mPlayDrawable;
    private Drawable mPauseDrawable;


    private ScActivity mActivity;
    private ITracklistAdapter mAdapter;

    public QuickTrackMenu(ScActivity activity, ITracklistAdapter tracklistAdapter) {
        super(activity);

        mActivity = activity;
        mAdapter = tracklistAdapter;

        mPlayActionItem = new ActionItem(mActivity, mActivity.getResources().getDrawable(R.drawable.bg_submenu_left_states), getPlayDrawable());
        //mPlayActionItem.setTitle("Play");
        mFavoriteActionItem = new ActionItem(mActivity, mActivity.getResources().getDrawable(R.drawable.bg_submenu_states), getFavoriteDrawable());
        //mFavoriteActionItem.setTitle("Favorite");
        mCommentActionItem = new ActionItem(mActivity, mActivity.getResources().getDrawable(R.drawable.bg_submenu_states), mActivity.getResources().getDrawable(R.drawable.ic_submenu_comment_states));
        //mCommentActionItem.setTitle("Comment");
        mShareActionItem = new ActionItem(mActivity, mActivity.getResources().getDrawable(R.drawable.bg_submenu_states), mActivity.getResources().getDrawable(R.drawable.ic_submenu_share_states));
        //mShareActionItem.setTitle(("Share"));
        mProfileActionItem = new ActionItem(mActivity, mActivity.getResources().getDrawable(R.drawable.bg_submenu_right_states), mActivity.getResources().getDrawable(R.drawable.ic_profile_states));
        //mProfileActionItem.setTitle("Profile");

        addActionItem(mPlayActionItem);
        addActionItem(mFavoriteActionItem);
        addActionItem(mCommentActionItem);
        addActionItem(mShareActionItem);
        addActionItem(mProfileActionItem);
    }

    public void show(View anchor, final Track track, final int itemPosition) {

        mPlayActionItem.setVisibility(track.isStreamable() ? View.VISIBLE : View.GONE);
        mPlayActionItem.setIcon((track.id == ((ITracklistAdapter) mAdapter).getPlayingId() && ((ITracklistAdapter) mAdapter).isPlaying())
                ? getPauseDrawable() : getPlayDrawable());

        mShareActionItem.setVisibility(track.isPublic() ? View.VISIBLE : View.GONE);
        mFavoriteActionItem.setIcon(track.user_favorite ? getFavoritedDrawable() : getFavoriteDrawable());

        setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
            @Override
            public void onItemClick(int pos) {
                switch (pos) {
                    case 0:
                        if (track.id == mAdapter.getPlayingId() && mAdapter.isPlaying()) {
                            mActivity.pause(false);
                        } else {
                            mActivity.playTrack(track.id, mAdapter.getData(), itemPosition, false);
                        }
                        break;

                    case 1:
                        Intent intent = new Intent(mActivity, UserBrowser.class);
                        intent.putExtra("userId", track.user.id);
                        mActivity.startActivity(intent);
                        break;

                    case 2:
                        if (track.user_favorite) {
                            track.user_favorite = false;
                            mAdapter.removeFavorite(track);
                        } else {
                            track.user_favorite = true;
                            mAdapter.addFavorite(track);
                        }
                        break;
                    case 3:
                        mActivity.addNewComment(CloudUtils.buildComment(
                                mActivity,
                                mActivity.getCurrentUserId(),
                                track.id, -1, "",
                                0), null);
                        break;
                    case 4:
                        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, track.title + " by " + track.user.username + " on #SoundCloud");
                        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, track.permalink_url);
                        mActivity.startActivity(Intent.createChooser(shareIntent, "Share: " + track.title));
                        break;
                }

            }
        });

        show(anchor);
    }

     private Drawable getPauseDrawable() {
        if (mPauseDrawable == null)
            mPauseDrawable = mContext.getResources().getDrawable(R.drawable.ic_submenu_pause_states);
        return mPauseDrawable;
    }

    private Drawable getPlayDrawable() {
        if (mPlayDrawable == null)
            mPlayDrawable = mContext.getResources().getDrawable(R.drawable.ic_submenu_play_states);
        return mPlayDrawable;
    }

    private Drawable getFavoriteDrawable() {
        if (mFavoriteDrawable == null)
            mFavoriteDrawable = mContext.getResources().getDrawable(R.drawable.ic_favorite_states);
        return mFavoriteDrawable;
    }

    private Drawable getFavoritedDrawable() {
        if (mFavoritedDrawable == null)
            mFavoritedDrawable = mContext.getResources().getDrawable(R.drawable.ic_favorited_states);
        return mFavoritedDrawable;
    }


}
