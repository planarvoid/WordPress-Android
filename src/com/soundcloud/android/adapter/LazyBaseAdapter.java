
package com.soundcloud.android.adapter;

import java.util.List;

import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.activity.LazyActivity;
import com.soundcloud.android.view.LazyRow;

public class LazyBaseAdapter extends BaseAdapter implements Filterable {

    protected int mSelectedIndex = -1;

    protected LazyActivity mActivity;

    protected List<Parcelable> mData;

    protected int mPage = 1;

    protected Boolean mDone = false;

    protected ImageLoader mImageLoader;

    @SuppressWarnings("unchecked")
    public LazyBaseAdapter(LazyActivity context, List<? extends Parcelable> data) {
        mData = (List<Parcelable>) data;
        mActivity = context;
        mImageLoader = ImageLoader.get(context);
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
        rowView.display(mData.get(index), mSelectedIndex == index);

        BindResult result = BindResult.ERROR;
        try { // put the bind in a try catch to catch any loading error (or the
            // occasional bad url)
            result = mImageLoader.bind(this, rowView.getRowIcon(), rowView.getIconRemoteUri());
        } catch (Exception e) {
        }
        ;
        rowView.setTemporaryDrawable(result);

        return rowView;

    }

    protected LazyRow createRow() {
        return new LazyRow(mActivity);
    }

    public void setSelected(int position) {
        mSelectedIndex = position;
    }

    public void clear() {
        mData.clear();
        reset();
    }

    public void reset() {
        mPage = 1;
        mSelectedIndex = -1;
    }

    public void incrementPage() {
        mPage++;
    }

    public int getPage() {
        return mPage;
    }

    public void setStopLoading(boolean done) {
        mDone = done;
    }

    public Filter getFilter() {
        // XXX really needed
        // TODO Auto-generated method stub
        return null;
    }

}
