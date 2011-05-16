
package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.task.LoadFollowingsTask.FollowingsListener;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.android.view.UserlistRow;

import android.os.Parcelable;

import java.util.ArrayList;

public class FollowingsAdapter extends LazyBaseAdapter implements FollowingsListener {

    public static final String IMAGE = "UserlistAdapter_image";

    public static final String TAG = "UserlistAdapter";

    public boolean gotFollowings;

    public FollowingsAdapter(ScActivity activity, ArrayList<Parcelable> data,  Class<?> model) {
        super(activity, data, model);
    }

    @Override
    public void refresh() {
        super.refresh();
        gotFollowings = false;
        mActivity.getSoundCloudApplication().followingsMap = null;
        mActivity.getSoundCloudApplication().requestUserFollowings(this);
    }

    @Override
    protected LazyRow createRow() {
        return new UserlistRow(mActivity, this);
    }

    @Override
    public void onFollowings(boolean success) {

    }

}
