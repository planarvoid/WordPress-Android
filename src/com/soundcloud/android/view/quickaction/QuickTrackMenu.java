package com.soundcloud.android.view.quickaction;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.adapter.ITracklistAdapter;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.ImageUtils;

public class QuickTrackMenu extends QuickAction {

    ActionItem mPlayActionItem;
    ActionItem mProfileActionItem;
    ActionItem mShareActionItem;
    ActionItem mCommentActionItem;
    ActionItem mFavoriteActionItem;

    private Drawable mLikeDrawable;
    private Drawable mlikedDrawable;

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
        mFavoriteActionItem = new ActionItem(mActivity, mActivity.getResources().getDrawable(R.drawable.bg_submenu_states), getLikeDrawable());
        //mFavoriteActionItem.setTitle("Favorite");
        mCommentActionItem = new ActionItem(mActivity, mActivity.getResources().getDrawable(R.drawable.bg_submenu_states), mActivity.getResources().getDrawable(R.drawable.ic_submenu_comment_states));
        //mCommentActionItem.setTitle("Comment");
        mShareActionItem = new ActionItem(mActivity, mActivity.getResources().getDrawable(R.drawable.bg_submenu_states), mActivity.getResources().getDrawable(R.drawable.ic_submenu_share_states));
        //mShareActionItem.setTitle(("Share"));
        mProfileActionItem = new ActionItem(mActivity, mActivity.getResources().getDrawable(R.drawable.bg_submenu_right_states), mActivity.getResources().getDrawable(R.drawable.avatar_badge));
        //mProfileActionItem.setTitle("Profile");

        addActionItem(mPlayActionItem);
        addActionItem(mFavoriteActionItem);
        addActionItem(mCommentActionItem);
        addActionItem(mShareActionItem);
        addActionItem(mProfileActionItem);
    }

    public void show(View anchor, final Track track, final int itemPosition) {

        if (track == null) return;

        mPlayActionItem.setVisibility(track.isStreamable() ? View.VISIBLE : View.GONE);
        mPlayActionItem.setIcon((track.id == ((ITracklistAdapter) mAdapter).getPlayingId() && ((ITracklistAdapter) mAdapter).isPlaying())
                ? getPauseDrawable() : getPlayDrawable());

        mShareActionItem.setVisibility(track.isPublic() ? View.VISIBLE : View.GONE);
        mFavoriteActionItem.setIcon(track.user_favorite ? getLikedDrawable() : getLikeDrawable());

        if (!track.hasAvatar() ||
                ImageUtils.loadImageSubstitute(mActivity, mProfileActionItem.getIconView(), track.user.avatar_url,
                        ImageUtils.getListItemGraphicSize(mActivity), null, null) != ImageLoader.BindResult.OK) {
            mProfileActionItem.getIconView().setImageDrawable(mActivity.getResources().getDrawable(R.drawable.avatar_badge));
        }

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
                        if (track.user_favorite) {
                            track.user_favorite = false;
                            mAdapter.removeFavorite(track);
                        } else {
                            track.user_favorite = true;
                            mAdapter.addFavorite(track);
                        }
                        break;

                    case 2:
                        mActivity.playTrack(track.id, mAdapter.getData(), itemPosition, true,true);
                        break;
                    case 3:
                        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, track.title + " by " + track.user.username + " on #SoundCloud");
                        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, track.permalink_url);
                        mActivity.startActivity(Intent.createChooser(shareIntent, "Share: " + track.title));
                        break;
                    case 4:
                        Intent intent = new Intent(mActivity, UserBrowser.class);
                        intent.putExtra("user", track.user);
                        mActivity.startActivity(intent);

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

    private Drawable getLikeDrawable() {
        if (mLikeDrawable == null)
            mLikeDrawable = mContext.getResources().getDrawable(R.drawable.ic_submenu_like_states);
        return mLikeDrawable;
    }

    private Drawable getLikedDrawable() {
        if (mlikedDrawable == null)
            mlikedDrawable = mContext.getResources().getDrawable(R.drawable.ic_submenu_liked_states);
        return mlikedDrawable;
    }


}
