
package com.soundcloud.android.adapter;

import android.util.Log;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.task.LoadFollowingsTask;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.android.view.UserlistRow;

import android.os.Parcelable;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Handler;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class UserlistAdapter extends LazyBaseAdapter implements LoadFollowingsTask.FollowingsListener {

    public static final String TAG = "UserlistAdapter";

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
