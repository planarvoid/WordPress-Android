package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.Friend;
import com.soundcloud.android.model.User;

import android.os.Parcelable;

public class FriendFinderAdapter extends SectionedUserlistAdapter {
    public FriendFinderAdapter(ScActivity activity) {
        super(activity);
    }
    @Override public void addItem(int index, Parcelable newItem) {
        if (!FollowStatus.get().isFollowing(newItem instanceof Friend ? ((Friend)newItem).user : ((User) newItem))){
            getData(index).add(newItem);
        }
    }

}
