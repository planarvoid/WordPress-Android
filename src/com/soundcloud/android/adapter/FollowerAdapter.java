
package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.view.FollowerRow;
import com.soundcloud.android.view.LazyRow;

import android.os.Parcelable;

import java.util.ArrayList;

public class FollowerAdapter extends UserlistAdapter {

    public static final String IMAGE = "FollowerAdapter_image";

    public static final String TAG = "FollowerAdapter";

    public FollowerAdapter(ScActivity activity, ArrayList<Parcelable> data) {
        super(activity, data);
    }

    @Override
    protected LazyRow createRow() {
        return new FollowerRow(mActivity, this);
    }

}
