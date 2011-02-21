
package com.soundcloud.android.adapter;

import java.util.ArrayList;

import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.activity.LazyActivity;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.android.view.UserlistRow;

public class UserlistAdapter extends LazyBaseAdapter {

    public static final String IMAGE = "UserlistAdapter_image";

    public static final String TAG = "UserlistAdapter";

    public UserlistAdapter(LazyActivity context, ArrayList<Parcelable> data) {
        super(context, data);
    }

    @Override
    protected LazyRow createRow() {
        return new UserlistRow(mActivity, this);
    }

}
