
package com.soundcloud.android.view;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.IUserlistAdapter;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.CloudUtils.GraphicsSizes;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;

public class UserlistRow extends LazyRow {

    private static final String TAG = "UserlistRow";

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

    protected final android.os.Handler mHandler = new android.os.Handler();

    public UserlistRow(ScActivity _activity,LazyBaseAdapter _adapter) {
        super(_activity, _adapter);

        mUsername = (TextView) findViewById(R.id.username);
        mFullname = (TextView) findViewById(R.id.fullname);
        mTracks = (TextView) findViewById(R.id.tracks);
        mFollowers = (TextView) findViewById(R.id.followers);
        mIcon = (ImageView) findViewById(R.id.icon);
        mTracksIcon = (ImageView) findViewById(R.id.tracks_icon);
        mFollowersIcon = (ImageView) findViewById(R.id.followers_icon);
        mFollowingBtn = (ImageButton)findViewById(R.id.toggleFollowing);
        mFollowBtn = (ImageButton)findViewById(R.id.toggleFollow);

        if (mFollowingBtn != null) {
            mFollowingBtn.setFocusable(false);
            mFollowingBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    changeFollowing(mUser.id, false);
                    setFollowingStatus(false);
                }
            });
        }
        if (mFollowBtn != null) {
            mFollowBtn.setFocusable(false);
            mFollowBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    changeFollowing(mUser.id, true);
                    setFollowingStatus(false);
                }
            });
        }
    }

    @Override
    protected int getRowResourceId() {
        return R.layout.user_list_item;
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

    public void setFollowingStatus(boolean enable) {
        FollowStatus followStatus = FollowStatus.get();
        mFollowingBtn.setVisibility(followStatus.following(mUser) ? View.VISIBLE : View.GONE);
        mFollowBtn.setVisibility(followStatus.following(mUser) ? View.GONE : View.VISIBLE);

        mFollowingBtn.setEnabled(enable);
        mFollowBtn.setEnabled(enable);
    }

    @Override
    public ImageView getRowIcon() {
        return mIcon;
    }

    @Override
    public String getIconRemoteUri() {
        if (mUser.avatar_url == null)
            return "";
        if (getContext().getResources().getDisplayMetrics().density > 1) {
            return CloudUtils.formatGraphicsUrl(mUser.avatar_url, GraphicsSizes.LARGE);
        } else {
            return CloudUtils.formatGraphicsUrl(mUser.avatar_url, GraphicsSizes.BADGE);
        }
    }


    protected void setTrackCount() {
        mTracks.setText(Integer.toString(mUser.track_count));
    }

    protected void setFollowerCount() {
       mFollowers.setText(Integer.toString(mUser.followers_count));
    }

    public void changeFollowing(final long userId, final boolean follow){
        FollowStatus.get().updateFollowing(userId, follow);
        final AndroidCloudAPI api = mActivity.getApp();
        new Thread() {
            @Override
            public void run() {
                final Request request = Request.to(Endpoints.MY_FOLLOWING, userId);
                boolean success = false;
                try {
                    int status;
                    if (follow) {
                        status = api.put(request).getStatusLine().getStatusCode();
                    } else {
                        status = api.delete(request).getStatusLine().getStatusCode();
                    }
                    success = (status == 200 || status == 201 || status == 404);
                } catch (IOException e) {
                    Log.e(TAG, "error", e);
                }
                if (!success) {
                    FollowStatus.get().updateFollowing(userId, !follow);
                }
                mHandler.post(new Runnable() {
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
               });
            }
        }.start();
    }
}
