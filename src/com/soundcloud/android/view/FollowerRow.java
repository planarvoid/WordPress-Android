
package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;

import android.view.View;
import android.widget.Button;

public class FollowerRow extends UserlistRow {

    private static final String TAG = "FollowerRow";

    protected Button mFollowingBtn;
    protected Button mFollowBtn;


    public FollowerRow(ScActivity _activity,LazyBaseAdapter _adapter) {
        super(_activity, _adapter);

        mFollowingBtn = (Button)findViewById(R.id.toggleFollowing);
        mFollowBtn = (Button)findViewById(R.id.toggleFollow);
    }

    @Override
    protected int getRowResourceId() {
        return R.layout.user_list_follower_item;
    }

    /** update the views with the data corresponding to selection index */
    @Override
    public void display(int position) {
        super.display(position);

        if (this.mUser.id%2 == 0){ //just for testing
            mFollowingBtn.setVisibility(View.VISIBLE);
            mFollowBtn.setVisibility(View.GONE);
            mFollowingBtn.refreshDrawableState();
        } else {
            mFollowingBtn.setVisibility(View.GONE);
            mFollowBtn.setVisibility(View.VISIBLE);
            mFollowBtn.refreshDrawableState();
        }
    }

    @Override
    protected void setFullname() {
       // do nothing
    }

    @Override
    protected void setTrackCount() {
        mTracks.setText(Integer.toString(mUser.track_count));
    }

    @Override
    protected void setFollowerCount() {
       mFollowers.setText(Integer.toString(mUser.followers_count));
    }

    // **********************End

}
