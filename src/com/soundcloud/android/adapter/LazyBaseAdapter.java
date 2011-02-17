
package com.soundcloud.android.adapter;

import android.content.Context;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.view.LazyRow;

import java.util.List;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class LazyBaseAdapter extends BaseAdapter {

    protected int mSelectedIndex = -1;

    protected Context mActivity;

    protected List<Parcelable> mData;

    protected int mPage = 1;

    protected Boolean mDone = false;

    protected ImageLoader mImageLoader;

    @SuppressWarnings("unchecked")
    public LazyBaseAdapter(Context context, List<? extends Parcelable> data) {
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
            Log.e(TAG, "error", e);
        }
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


}
