
package com.soundcloud.android.view;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.UserlistAdapter;
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
        mTracksIcon = (ImageView) findViewById(R.id.tracks_icon);
        mFollowersIcon = (ImageView) findViewById(R.id.followers_icon);
        mFollowingBtn = (Button)findViewById(R.id.toggleFollowing);
        mFollowBtn = (Button)findViewById(R.id.toggleFollow);

        if (mFollowingBtn != null) mFollowingBtn.setFocusable(false);
        if (mFollowBtn != null) mFollowBtn.setFocusable(false);
    }

    @Override
    protected int getRowResourceId() {
        return R.layout.user_list_item;
    }

    /** update the views with the data corresponding to selection index */
    @Override
    public void display(int position) {
        mUser = ((UserlistAdapter) mAdapter).getUserAt(position);
        super.display(position);
        mUsername.setText(mUser.username);
        setTrackCount();
        setFollowerCount();
        setFollowingStatus();

        _isFollowing = false;
    }

    public void setFollowingStatus() {

        if (mActivity.getSoundCloudApplication().followingsSet == null){
            mFollowingBtn.setVisibility(View.GONE);
            mFollowBtn.setVisibility(View.GONE);
        } else {
            Log.i("AAAAA","Check following of " + mUser.username + " " + mUser.id + " " + mActivity.getSoundCloudApplication().followingsSet.contains(mUser.id));
            mFollowingBtn.setVisibility(mActivity.getSoundCloudApplication().followingsSet.contains(mUser.id) ?
                    View.VISIBLE : View.GONE);
            mFollowBtn.setVisibility(mActivity.getSoundCloudApplication().followingsSet.contains(mUser.id) ?
                    View.GONE : View.VISIBLE);

            if (mFollowingBtn.getVisibility() == View.VISIBLE){
                mFollowingBtn.refreshDrawableState();
            }
        }
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
        } else
            return CloudUtils.formatGraphicsUrl(mUser.avatar_url, GraphicsSizes.BADGE);
    }

    // **********************
    // given their own functions to be easily overwritten by subclasses who may
    // not use them or use them differently

    protected void setTrackCount() {
        mTracks.setText(Integer.toString(mUser.track_count));
    }

    protected void setFollowerCount() {
       mFollowers.setText(Integer.toString(mUser.followers_count));
    }

    // **********************End

}
