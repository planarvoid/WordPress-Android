package com.soundcloud.android.adapter;

import android.os.Parcelable;
import android.util.Log;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.Friend;
import com.soundcloud.android.model.User;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.android.view.UserlistSectionedRow;

public class FriendFinderAdapter extends SectionedUserlistAdapter {
    public FriendFinderAdapter(ScActivity activity) {
        super(activity);
    }
    @Override public void addItem(int index, Parcelable newItem) {
        if (!FollowStatus.get().isFollowing(newItem instanceof Friend ? ((Friend)newItem).user.id : ((User) newItem).id)){
            getData(index).add(newItem);
        }
    }

}
