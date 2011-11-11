
package com.soundcloud.android.adapter;

import android.view.animation.Animation;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.utils.ListAlphaAnimation;
import com.soundcloud.android.view.LazyRow;

import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class LazyBaseAdapter extends BaseAdapter {
    protected ScActivity mActivity;
    protected LazyEndlessAdapter mWrapper;
    protected List<Parcelable> mData;
    protected int mPage = 1;
    private Class<?> mLoadModel;

    protected Map<Integer, ListAlphaAnimation> mIconAnimations = new HashMap<Integer, ListAlphaAnimation>();

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

    public void setData(List<Parcelable> data) {
        mData = data;
        reset();
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
        LazyRow rowView = row instanceof LazyRow ? (LazyRow) row : createRow(index);
        // update the cell renderer, and handle selection state
        rowView.display(index);
        return rowView;
    }

    protected abstract LazyRow createRow(int position);

    public void reset() {
        mData.clear();
        mPage = 1;
        mIconAnimations.clear();
    }

    public Class<?> getLoadModel() {
        return mLoadModel;
    }

    public void onPostQueryExecute() {
        if (mWrapper != null) mWrapper.onPostQueryExecute();
    }

    public void onDestroy(){}

    public void addItem(Parcelable newItem) {
        getData().add(newItem);
    }

    public Animation getIconAnimation(int position){
        return mIconAnimations.get(position);
    }

    public ListAlphaAnimation setIconAnimation(Integer position, ListAlphaAnimation animation){
        return mIconAnimations.put(position, animation);
    }
}
