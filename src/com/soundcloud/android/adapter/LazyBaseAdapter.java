
package com.soundcloud.android.adapter;

import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.view.LazyRow;

import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;

public class LazyBaseAdapter extends BaseAdapter {

    public int submenuIndex = -1;

    public int animateSubmenuIndex = -1;

    protected ScActivity mActivity;

    protected List<Parcelable> mData;

    protected int mPage = 1;

    protected boolean mDone = false;

    // XXX assumes images
    protected ImageLoader mImageLoader;

    @SuppressWarnings("unchecked")
    public LazyBaseAdapter(ScActivity activity, List<? extends Parcelable> data) {
        mData = (List<Parcelable>) data;
        mActivity = activity;
        if (activity != null) mImageLoader = ImageLoader.get(activity);
    }

    public List<Parcelable> getData() {
        return mData;
    }

    public int getCount() {
        return mData.size();
    }

    public Object getItem(int location) {
        return mData.get(location);
    }

    public long getItemId(int i) {
        return i;
    }

    public View getView(int index, View row, ViewGroup parent) {
        LazyRow rowView = null;

        if (row == null) {
            rowView = createRow();
        } else {
            rowView = (LazyRow) row;
        }

        // update the cell renderer, and handle selection state
        rowView.display(index);


        BindResult result = BindResult.ERROR;
        try { // put the bind in a try catch to catch any loading error (or the
            // occasional bad url)
            if (CloudUtils.checkIconShouldLoad(rowView.getIconRemoteUri()))
                result = mImageLoader.bind(this, rowView.getRowIcon(), rowView.getIconRemoteUri());
            else
                mImageLoader.unbind(rowView.getRowIcon());
        } catch (Exception e) {
        }

        rowView.setTemporaryDrawable(result);

        return rowView;
    }

    protected LazyRow createRow() {
        return new LazyRow(mActivity, this);
    }

    public void clear() {
        mData.clear();
        reset();
    }

    public void reset() {
        mPage = 1;
        submenuIndex = -1;
        animateSubmenuIndex = -1;
    }
}
