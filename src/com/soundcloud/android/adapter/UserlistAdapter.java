
package com.soundcloud.android.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.android.view.UserlistRow;

public class UserlistAdapter extends LazyBaseAdapter {

    public static final String IMAGE = "UserlistAdapter_image";

    public static final String TAG = "UserlistAdapter";

    public UserlistAdapter(Context context, ArrayList<Parcelable> data) {
        super(context, data);
    }

    @Override
    public View getView(int index, View row, ViewGroup parent) {

        UserlistRow rowView = null;

        if (row == null) {
            rowView = (UserlistRow) createRow();
        } else {
            rowView = (UserlistRow) row;
        }

        // update the cell renderer, and handle selection state
        rowView.display(mData.get(index), mSelectedIndex == index);

        BindResult result = BindResult.ERROR;
        try { // put the bind in a try catch to catch any loading error (or the
            // occasional bad url)
            if (CloudUtils.checkIconShouldLoad(rowView.getIconRemoteUri()))
                result = mImageLoader.bind(this, rowView.getRowIcon(), rowView.getIconRemoteUri());
            else
                mImageLoader.unbind(rowView.getRowIcon());
        } catch (Exception e) {
        }
        ;
        rowView.setTemporaryDrawable(result);

        return rowView;

    }

    @Override
    protected LazyRow createRow() {
        return new UserlistRow(mActivity);
    }

}
