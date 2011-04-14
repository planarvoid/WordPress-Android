
package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.CloudUtils.GraphicsSizes;

import android.widget.ImageView;
import android.widget.TextView;

public class UserlistRow extends LazyRow {

    private static final String TAG = "UserlistRow";

    protected User mUser;

    protected TextView mUsername;
    protected TextView mFullname;
    protected TextView mTracks;
    protected TextView mFollowers;

    protected ImageView mTracksIcon;
    protected ImageView mFollowersIcon;

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

    }

    @Override
    protected int getRowResourceId() {
        return R.layout.user_list_item;
    }

    /** update the views with the data corresponding to selection index */
    @Override
    public void display(int position) {
        mUser = (User) mAdapter.getData().get(position);
        super.display(position);
        mUsername.setText(mUser.username);
        setFullname();
        setTrackCount();
        setFollowerCount();

        _isFollowing = false;
    }

    @Override
    public ImageView getRowIcon() {
        if (getContext().getResources().getDisplayMetrics().density > 1) {
            mIcon.getLayoutParams().width = 67;
            mIcon.getLayoutParams().height = 67;
        }
        return mIcon;
    }

    @Override
    public String getIconRemoteUri() {
        if (mUser.avatar_url == null)
            return "";
        if (getContext().getResources().getDisplayMetrics().density > 1) {
            return CloudUtils.formatGraphicsUrl(mUser.avatar_url, GraphicsSizes.LARGE);
        } else
            return CloudUtils.formatGraphicsUrl(mUser.avatar_url, GraphicsSizes.BADGE);
    }

    // **********************
    // givent their own functions to be easily overwritten by subclasses who may
    // not use them or use them differently

    protected void setFullname() {
        mFullname.setText(mUser.full_name);
    }

    protected void setTrackCount() {
        String trackCount = mActivity.getResources().getQuantityString(R.plurals.user_track_count,
                mUser.track_count, mUser.track_count);
        mTracks.setText(trackCount);
    }

    protected void setFollowerCount() {
        String followerCount = mActivity.getResources().getQuantityString(
                R.plurals.user_follower_count, mUser.followers_count,
                mUser.followers_count);
        mFollowers.setText(followerCount);
    }

    // **********************End

}
