
package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.task.LoadFollowingsTask;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.android.view.UserlistRow;

import android.os.Parcelable;

import java.util.ArrayList;

public class UserlistAdapter extends LazyBaseAdapter implements LoadFollowingsTask.FollowingsListener {

    public static final String IMAGE = "UserlistAdapter_image";

    public static final String TAG = "UserlistAdapter";

    public boolean gotFollowings;

    public UserlistAdapter(ScActivity activity, ArrayList<Parcelable> data,  Class<?> model) {
        super(activity, data, model);

        activity.getSoundCloudApplication().requestUserFollowings(this, false);
    }

    @Override
    protected LazyRow createRow() {
        return new UserlistRow(mActivity, this);
    }


    public void refresh(boolean userRefresh) {
        if (userRefresh) mActivity.getSoundCloudApplication().requestUserFollowings(this, true);
        super.refresh(userRefresh);
    }

    public User getUserAt(int index) {
        return (User) mData.get(index);
    }

    public void onFollowings(boolean success){
        this.notifyDataSetChanged();
    }

}
