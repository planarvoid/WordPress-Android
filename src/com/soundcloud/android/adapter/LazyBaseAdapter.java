
package com.soundcloud.android.adapter;

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

    @SuppressWarnings("unchecked")
    public LazyBaseAdapter(ScActivity activity, List<? extends Parcelable> data) {
        mData = (List<Parcelable>) data;
        mActivity = activity;
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
