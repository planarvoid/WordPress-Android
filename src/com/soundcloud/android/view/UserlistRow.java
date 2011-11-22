
package com.soundcloud.android.view;

import android.database.Cursor;
import android.os.Parcelable;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.IScAdapter;
import com.soundcloud.android.adapter.IUserlistAdapter;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.User;
import com.soundcloud.android.utils.ImageUtils;

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

    protected ImageButton mFollowBtn;
    protected ImageButton mFollowingBtn;

    protected Boolean _isFollowing;



    public UserlistRow(ScActivity _activity, IScAdapter _adapter) {
        super(_activity, _adapter);

        mUsername = (TextView) findViewById(R.id.username);
        mFullname = (TextView) findViewById(R.id.fullname);
        mTracks = (TextView) findViewById(R.id.tracks);
        mFollowers = (TextView) findViewById(R.id.followers);
        mIcon = (ImageView) findViewById(R.id.icon);
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
    public void display(Cursor cursor) {
        display(cursor.getPosition(), new User(cursor));
    }
    @Override
    public void display(int position, Parcelable p) {
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

        if (mUser.id == mCurrentUserId) {
            mFollowingBtn.setVisibility(View.GONE);
            mFollowBtn.setVisibility(View.GONE);
        } else {
            mFollowingBtn.setVisibility(following ? View.VISIBLE : View.GONE);
            mFollowBtn.setVisibility(following ? View.GONE : View.VISIBLE);
            mFollowingBtn.setEnabled(enabled);
            mFollowBtn.setEnabled(enabled);
        }

    }

    @Override
    public String getIconRemoteUri() {
        if (mUser.avatar_url == null) return "";
        return ImageUtils.formatGraphicsUriForList(getContext(), mUser.avatar_url);
    }

    protected void setTrackCount() {
        mTracks.setText(Integer.toString(mUser.track_count));
    }

    protected void setFollowerCount() {
       mFollowers.setText(Integer.toString(mUser.followers_count));
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
}
