
package com.soundcloud.android.view;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.IScAdapter;
import com.soundcloud.android.adapter.IUserlistAdapter;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.ImageUtils;

public class UserlistRow extends LazyRow {
    protected User mUser;

    protected TextView mUsername;
    protected TextView mFullname;
    protected TextView mTracks;
    protected TextView mFollowers;
    protected View mVrStats;

    protected Button mFollowBtn;
    protected Button mFollowingBtn;

    public UserlistRow(Context _activity, IScAdapter _adapter, boolean useFollowBack) {
        super(_activity, _adapter);

        mUsername = (TextView) findViewById(R.id.username);
        mFullname = (TextView) findViewById(R.id.fullname);
        mTracks = (TextView) findViewById(R.id.tracks);
        mFollowers = (TextView) findViewById(R.id.followers);
        mIcon = (ImageView) findViewById(R.id.icon);
        mFollowingBtn = (Button) findViewById(R.id.toggleFollowing);
        mFollowBtn = (Button) findViewById(R.id.toggleFollow);
        mVrStats = findViewById(R.id.vr_stats);

        // set proper follow back text and algnment to wider button
        if (useFollowBack) {
            mFollowBtn.setText(R.string.btn_follow_back);
            if (mFollowingBtn.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
                final RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mFollowingBtn.getLayoutParams();
                layoutParams.addRule(RelativeLayout.ALIGN_LEFT, R.id.toggleFollow);
                layoutParams.addRule(RelativeLayout.ALIGN_RIGHT, R.id.toggleFollow);
            }

        } else if (mFollowBtn.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
            final RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mFollowBtn.getLayoutParams();
            layoutParams.addRule(RelativeLayout.ALIGN_LEFT, R.id.toggleFollowing);
            layoutParams.addRule(RelativeLayout.ALIGN_RIGHT, R.id.toggleFollowing);
        }

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
    public void display(Cursor cursor) {
        display(cursor.getPosition(), new User(cursor));
    }

    @Override
    public void display(int position, Parcelable p) {
        mUser = ((IUserlistAdapter) mAdapter).getUserAt(position);
        super.display(position);
        mUsername.setText(mUser.username);
        setFollowingStatus(true);
        setTrackCount();
        setFollowerCount();
        mVrStats.setVisibility((mUser.track_count == 0 || mUser.followers_count == 0) ? View.GONE : View.VISIBLE);
    }

    public void setFollowingStatus(boolean enabled) {
        boolean following = FollowStatus.get().isFollowing(mUser);

        if (mUser.id == mCurrentUserId) {
            mFollowingBtn.setVisibility(View.INVISIBLE);
            mFollowBtn.setVisibility(View.INVISIBLE);
        } else {
            mFollowingBtn.setVisibility(following ? View.VISIBLE : View.INVISIBLE);
            mFollowBtn.setVisibility(following ? View.INVISIBLE : View.VISIBLE);
            mFollowingBtn.setEnabled(enabled);
            mFollowBtn.setEnabled(enabled);
        }
    }

    protected void setTrackCount() {
        if (mUser.track_count == 0){
            mTracks.setVisibility(View.GONE);
        } else {
            mTracks.setText(Integer.toString(mUser.track_count));
            mTracks.setVisibility(View.VISIBLE);
        }
    }

    protected void setFollowerCount() {
        if (mUser.followers_count == 0){
            mFollowers.setVisibility(View.GONE);
        } else {
            mFollowers.setText(Integer.toString(mUser.followers_count));
            mFollowers.setVisibility(View.VISIBLE);
        }
    }

    public void toggleFollowing(final long userId) {
        SoundCloudApplication app = SoundCloudApplication.fromContext(getContext());
        if (app != null) {
            FollowStatus.get().toggleFollowing(userId, app, new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    setFollowingStatus(true);
                    //if (msg.arg1 == 1) mAdapter.notifyDataSetChanged();
                }
            });

            setFollowingStatus(false);
        }
    }

    @Override
    public String getIconRemoteUri() {
        return mUser == null ? null : mUser.getListAvatarUri(getContext());
    }
}
