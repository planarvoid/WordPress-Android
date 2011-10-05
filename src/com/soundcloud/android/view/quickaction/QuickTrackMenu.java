package com.soundcloud.android.view.quickaction;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.View;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.adapter.ITracklistAdapter;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.CloudUtils;

public class QuickTrackMenu extends QuickAction {

    View mPlayActionView;
    View mShareActionView;
    View mCommentActionView;

    ActionItem mFavoriteActionItem;

    private Drawable mFavoriteDrawable;
    private Drawable mFavoritedDrawable;
    private ScActivity mActivity;
    private ITracklistAdapter mAdapter;

    public QuickTrackMenu(ScActivity activity, ITracklistAdapter tracklistAdapter) {
        super(activity);

        mActivity = activity;
        mAdapter = tracklistAdapter;

        mPlayActionView = addActionItem(new ActionItem(mActivity.getResources().getDrawable(R.drawable.ic_submenu_play_states)));

        //dashboard action item
        addActionItem(new ActionItem(mActivity.getResources().getDrawable(R.drawable.ic_profile_states)));
        mFavoriteActionItem = new ActionItem(mActivity.getResources().getDrawable(R.drawable.ic_favorite_states));
        addActionItem(mFavoriteActionItem);
        mCommentActionView = addActionItem(new ActionItem(mActivity.getResources().getDrawable(R.drawable.ic_comment_states)));
        mShareActionView = addActionItem(new ActionItem(mActivity.getResources().getDrawable(R.drawable.ic_share_states)));
    }

    public void show(View anchor, final Track track, final int itemPosition) {

        mPlayActionView.setVisibility(track.isStreamable() ? View.VISIBLE : View.GONE);
        mShareActionView.setVisibility(track.isPublic() ? View.VISIBLE : View.GONE);
        mFavoriteActionItem.setIcon(track.user_favorite ? getFavoritedDrawable() : getFavoriteDrawable());
        setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
            @Override
            public void onItemClick(int pos) {
                switch (pos) {
                    case 0:
                        Intent intent = new Intent(mActivity, UserBrowser.class);
                        intent.putExtra("userId", track.user.id);
                        mActivity.startActivity(intent);
                        break;

                    case 1:
                        if (track.id == mAdapter.getPlayingId() && mAdapter.isPlaying()) {
                            mActivity.pause(false);
                        } else {
                            mActivity.playTrack(track.id, mAdapter.getData(), itemPosition, false);
                        }
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
