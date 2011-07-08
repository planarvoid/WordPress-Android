
package com.soundcloud.android.view;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.IUserlistAdapter;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.CloudUtils;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class UserlistRow extends LazyRow {
    protected User mUser;

    protected TextView mUsername;
    protected TextView mFullname;
    protected TextView mTracks;
    protected TextView mFollowers;

    protected ImageView mTracksIcon;
    protected ImageView mFollowersIcon;

    protected ImageButton mFollowBtn;
    protected ImageButton mFollowingBtn;

    protected Boolean _isFollowing;

    public UserlistRow(ScActivity _activity,LazyBaseAdapter _adapter) {
        super(_activity, _adapter);

        mUsername = (TextView) findViewById(R.id.username);
        mFullname = (TextView) findViewById(R.id.fullname);
        mTracks = (TextView) findViewById(R.id.tracks);
        mFollowers = (TextView) findViewById(R.id.followers);
        mIcon = (ImageView) findViewById(R.id.icon);
        mTracksIcon = (ImageView) findViewById(R.id.tracks_icon);
        mFollowersIcon = (ImageView) findViewById(R.id.followers_icon);
        mFollowingBtn = (ImageButton) findViewById(R.id.toggleFollowing);
        mFollowBtn = (ImageButton) findViewById(R.id.toggleFollow);

        if (mFollowingBtn != null) {
            mFollowingBtn.setFocusable(false);
            mFollowingBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFollowing(mUser.id);
                }
            });
        }
        if (mFollowBtn != null) {
            mFollowBtn.setFocusable(false);
            mFollowBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFollowing(mUser.id);
                }
            });
        }
    }

    @Override
    protected int getRowResourceId() {
        return R.layout.user_list_row;
    }

    protected Drawable getIconBgResourceId() {
        return getResources().getDrawable(R.drawable.avatar_badge);
    }

    protected Drawable getLargeIconBgResourceId() {
        return getResources().getDrawable(R.drawable.avatar_badge_large);
    }


    /** update the views with the data corresponding to selection index */
    @Override
    public void display(int position) {
        mUser = ((IUserlistAdapter) mAdapter).getUserAt(position);
        super.display(position);
        mUsername.setText(mUser.username);
        setTrackCount();
        setFollowerCount();
        setFollowingStatus(true);
        _isFollowing = false;
    }

    public void setFollowingStatus(boolean enabled) {
        boolean following = FollowStatus.get().isFollowing(mUser);

        mFollowingBtn.setVisibility(following ? View.VISIBLE : View.GONE);
        mFollowBtn.setVisibility(following ? View.GONE : View.VISIBLE);
        mFollowingBtn.setEnabled(enabled);
        mFollowBtn.setEnabled(enabled);
    }

    @Override
    public ImageView getRowIcon() {
        return mIcon;
    }

    @Override
    public String getIconRemoteUri() {
        if (mUser.avatar_url == null)
            return "";

        if (CloudUtils.isScreenXL(mActivity)) {
            return CloudUtils.formatGraphicsUrl(mUser.avatar_url, Consts.GraphicsSizes.LARGE);
        } else {
            if (getContext().getResources().getDisplayMetrics().density > 1) {
                return CloudUtils.formatGraphicsUrl(mUser.avatar_url, Consts.GraphicsSizes.LARGE);
            } else {
                return CloudUtils.formatGraphicsUrl(mUser.avatar_url, Consts.GraphicsSizes.BADGE);
            }
        }
    }

    protected void setTrackCount() {
        mTracks.setText(Integer.toString(mUser.track_count));
    }

    protected void setFollowerCount() {
       mFollowers.setText(Integer.toString(mUser.followers_count));
    }

    public void toggleFollowing(final long userId) {
        FollowStatus.get().toggleFollowing(userId, mActivity.getApp(), new Handler() {
            @Override public void handleMessage(Message msg) {
                setFollowingStatus(true);
                if (msg.arg1 == 1) mAdapter.notifyDataSetChanged();
            }
        });
        setFollowingStatus(false);
    }
}
