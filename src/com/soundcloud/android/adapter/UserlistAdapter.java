
package com.soundcloud.android.adapter;

import android.graphics.drawable.Drawable;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.android.view.UserlistRow;

import android.os.Parcelable;

import java.util.ArrayList;

public class UserlistAdapter extends LazyBaseAdapter implements FollowStatus.Listener, IUserlistAdapter {
    private boolean mUseFollowBack;

    public UserlistAdapter(UserBrowser userBrowser, ArrayList<Parcelable> parcelables, Class<User> userClass) {
        this(userBrowser, parcelables, userClass, false);
    }

    public UserlistAdapter(ScActivity activity, ArrayList<Parcelable> data, Class<?> model, boolean useFollowBack) {
        super(activity, data, model);
        mUseFollowBack = useFollowBack;
        if (activity != null) {
            FollowStatus.get().requestUserFollowings(activity.getApp(), this, false);
        }
    }

    @Override
    protected LazyRow createRow(int position) {
        return new UserlistRow(mContext, this, mUseFollowBack);
    }


    public User getUserAt(int index) {
        return (User) mData.get(index);
    }

    @Override public void onChange(boolean success, FollowStatus status){
        notifyDataSetChanged();
    }
}
