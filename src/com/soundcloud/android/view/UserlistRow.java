
package com.soundcloud.android.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.widget.ImageView;
import android.widget.TextView;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.CloudUtils.GraphicsSizes;
import com.soundcloud.android.objects.User;

public class UserlistRow extends LazyRow {

    private static final String TAG = "UserlistRow";

    protected User mUser;

    protected TextView mUsername;

    protected TextView mLocation;

    protected TextView mTracks;

    protected TextView mFollowers;

    protected ImageView mTracksIcon;

    protected ImageView mFollowersIcon;

    protected Boolean _isFollowing;

    public UserlistRow(Context _context) {
        super(_context);

        mUsername = (TextView) findViewById(R.id.username);
        mLocation = (TextView) findViewById(R.id.location);
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
    public void display(Parcelable p, boolean selected) {
        super.display(p, selected);
        mUser = (User) p;
        mUsername.setText(mUser.getUsername());
        setLocation();
        setTrackCount();
        setFollowerCount();
        if (mUser.getUserFollowing() != null && mUser.getUserFollowing().equalsIgnoreCase("true")) {
            _isFollowing = true;
        } else {
            _isFollowing = false;
        }
        if (getContext().getResources().getDisplayMetrics().density > 1) {
            mIcon.getLayoutParams().width = 67;
            mIcon.getLayoutParams().height = 67;
        }
    }

    @Override
    protected Drawable getTemporaryDrawable() {
        return mContext.getResources().getDrawable(R.drawable.artwork_badge);
    }

    @Override
    public ImageView getRowIcon() {
        return mIcon;
    }

    @Override
    public String getIconRemoteUri() {
        if (getContext().getResources().getDisplayMetrics().density > 1) {
            return CloudUtils.formatGraphicsUrl(mUser.getAvatarUrl(), GraphicsSizes.large);
        } else
            return CloudUtils.formatGraphicsUrl(mUser.getAvatarUrl(), GraphicsSizes.badge);
    }

    // **********************
    // givent their own functions to be easily overwritten by subclasses who may
    // not use them or use them differently

    protected void setLocation() {
        mLocation.setText(mUser.getLocation());
    }

    protected void setTrackCount() {
        String trackCount = mContext.getResources().getQuantityString(R.plurals.user_track_count,
                Integer.parseInt(mUser.getTrackCount()), Integer.parseInt(mUser.getTrackCount()));
        mTracks.setText(trackCount);
    }

    protected void setFollowerCount() {
        String followerCount = mContext.getResources().getQuantityString(
                R.plurals.user_follower_count, Integer.parseInt(mUser.getFollowersCount()),
                Integer.parseInt(mUser.getFollowersCount()));
        mFollowers.setText(followerCount);
    }

    // **********************End

}
