
package com.soundcloud.android.adapter;

import java.util.ArrayList;

import android.os.Parcelable;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.android.view.UserlistRow;

public class UserlistAdapter extends LazyBaseAdapter {

    public static final String IMAGE = "UserlistAdapter_image";

    public static final String TAG = "UserlistAdapter";

    public UserlistAdapter(ScActivity activity, ArrayList<Parcelable> data) {
        super(activity, data);
    }

    @Override
    protected LazyRow createRow() {
        return new UserlistRow(mActivity, this);
    }

}
