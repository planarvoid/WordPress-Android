
package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.User;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.android.view.UserlistRow;

import android.os.Parcelable;

import java.util.ArrayList;

public class UserlistAdapter extends LazyBaseAdapter implements FollowStatus.Listener, IUserlistAdapter {
    public UserlistAdapter(ScActivity activity,
                           ArrayList<Parcelable> data,
                           Class<?> model) {
        super(activity, data, model);
        FollowStatus.get().requestUserFollowings(activity.getApp(), this, false);
    }

    @Override
    protected LazyRow createRow() {
        return new UserlistRow(mActivity, this);
    }


    public User getUserAt(int index) {
        return (User) mData.get(index);
    }

    @Override public void onChange(boolean success, FollowStatus status){
        notifyDataSetChanged();
    }
}
