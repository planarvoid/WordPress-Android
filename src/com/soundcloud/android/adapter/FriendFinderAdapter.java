package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.objects.Friend;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.android.view.UserlistSectionedRow;

public class FriendFinderAdapter extends SectionedAdapter implements IUserlistAdapter, FollowStatus.Listener {

    public static final String TAG = "FriendFinderAdapter";

    public FriendFinderAdapter(ScActivity activity) {
        super(activity);
        FollowStatus.get().requestUserFollowings(activity.getApp(), this, false);
    }

    public User getUserAt(int index) {
        if (getItem(index) instanceof Friend)
            return ((Friend) getItem(index)).user;
        else if (getItem(index) instanceof User)
            return ((User) getItem(index));

        throw new IllegalStateException("Unknown row type found: " + getItem(index) + " at " + index);
    }

    @Override public void onFollowings(boolean success, FollowStatus status) {
        notifyDataSetChanged();
    }

    @Override protected LazyRow createRow() {
        return new UserlistSectionedRow(mActivity, this);
    }
}
