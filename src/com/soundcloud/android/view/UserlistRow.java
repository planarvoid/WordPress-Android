
package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.IUserlistAdapter;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.ImageUtils;

import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class UserlistRow extends LazyRow {
    protected User mUser;

    protected TextView mUsername;
    protected TextView mFullname;
    protected TextView mTracks;
    protected TextView mFollowers;

    protected Button mFollowBtn;
    protected Button mFollowingBtn;

    protected Boolean _isFollowing;

    public UserlistRow(ScActivity _activity,LazyBaseAdapter _adapter) {
        super(_activity, _adapter);

        mUsername = (TextView) findViewById(R.id.username);
        mFullname = (TextView) findViewById(R.id.fullname);
        mTracks = (TextView) findViewById(R.id.tracks);
        mFollowers = (TextView) findViewById(R.id.followers);
        mIcon = (ImageView) findViewById(R.id.icon);
        mFollowingBtn = (Button) findViewById(R.id.toggleFollowing);
        mFollowBtn = (Button) findViewById(R.id.toggleFollow);

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

        if (mUser.id == mActivity.getCurrentUserId()) {
            mFollowingBtn.setVisibility(View.INVISIBLE);
            mFollowBtn.setVisibility(View.INVISIBLE);
        } else {
            mFollowingBtn.setVisibility(following ? View.VISIBLE : View.INVISIBLE);
            mFollowBtn.setVisibility(following ? View.INVISIBLE : View.VISIBLE);
            mFollowingBtn.setEnabled(enabled);
            mFollowBtn.setEnabled(enabled);
        }

    }

    @Override
    public String getIconRemoteUri() {
        if (mUser == null || mUser.avatar_url == null) return "";
        return ImageUtils.formatGraphicsUriForList(mActivity, mUser.avatar_url);
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
