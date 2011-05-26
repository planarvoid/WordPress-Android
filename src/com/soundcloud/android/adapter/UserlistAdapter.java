
package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.android.view.UserlistRow;

import android.os.Parcelable;

import java.util.ArrayList;

public class UserlistAdapter extends LazyBaseAdapter {

    public static final String IMAGE = "UserlistAdapter_image";

    public static final String TAG = "UserlistAdapter";

    public boolean gotFollowings;

    public UserlistAdapter(ScActivity activity, ArrayList<Parcelable> data,  Class<?> model) {
        super(activity, data, model);
    }

    @Override
    protected LazyRow createRow() {
        return new UserlistRow(mActivity, this);
    }

}
