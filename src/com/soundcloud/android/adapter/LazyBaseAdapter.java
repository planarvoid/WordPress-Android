
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
    protected LazyEndlessAdapter mWrapper;
    protected List<Parcelable> mData;
    protected int mPage = 1;
    private Class<?> mLoadModel;

    @SuppressWarnings("unchecked")
    public LazyBaseAdapter(ScActivity activity, List<? extends Parcelable> data, Class<?> model) {
        mData = (List<Parcelable>) data;
        mActivity = activity;
        mLoadModel = model;
    }

    public void setWrapper(LazyEndlessAdapter wrapper) {
        mWrapper = wrapper;
    }

    public void setModel( Class<?> model ) {
        mLoadModel = model;
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
        LazyRow rowView = row instanceof LazyRow ? (LazyRow) row : createRow();
        // update the cell renderer, and handle selection state
        rowView.display(index);
        return rowView;
    }

    protected LazyRow createRow() {
        return new LazyRow(mActivity, this);
    }

    public void reset() {
        mData.clear();
        mPage = 1;
        submenuIndex = -1;
        animateSubmenuIndex = -1;
    }

    public Class<?> getLoadModel() {
        return mLoadModel;
    }

    public boolean isQuerying() { return false;}

    public void onPostQueryExecute() {
        if (mWrapper != null) mWrapper.onPostQueryExecute();
    }

}
