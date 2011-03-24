
package com.soundcloud.android.adapter;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.objects.Recording.Recordings;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.android.view.MyTracklistRow;

import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Config;
import android.util.Log;
import android.widget.FilterQueryProvider;

import java.util.ArrayList;
public class MyTracksAdapter extends TracklistAdapter {

    protected Cursor mCursor;
    protected int mRowIDColumn;
    protected boolean mDataValid;
    protected ChangeObserver mChangeObserver;
    protected DataSetObserver mDataSetObserver = new MyDataSetObserver();
    protected FilterQueryProvider mFilterQueryProvider;

    public MyTracksAdapter(ScActivity activity, ArrayList<Parcelable> data) {
        super(activity, data);
        refreshCursor();
    }

    @Override
    protected LazyRow createRow() {
        return new MyTracklistRow(mActivity, this);
    }


    public void refreshCursor() {
        mCursor = mActivity.getContentResolver().query(Recordings.CONTENT_URI, null,
                Recordings.USER_ID + "='" + CloudUtils.getCurrentUserId(mActivity) + "'", null,
                null);
    }

    @Override
    public void reset() {
        mPage = 1;
        submenuIndex = -1;
        animateSubmenuIndex = -1;
        refreshCursor();
    }

    /**
     * @see android.widget.ListAdapter#getCount()
     */
    @Override
    public int getCount() {
        if (mDataValid && mCursor != null) {
            return mCursor.getCount() + super.getCount();
        } else {
            return super.getCount();
        }
    }

    /**
     * @see android.widget.ListAdapter#getItem(int)
     */
    @Override
    public Object getItem(int position) {
        if (mDataValid && mCursor != null) {
            if (position < mCursor.getCount()){
                mCursor.moveToPosition(position);
                return mCursor;
            } else
                return super.getItem(position - mCursor.getCount());
        } else {
            return super.getItem(position);
        }
    }

    /**
     * @see android.widget.ListAdapter#getItemId(int)
     */
    @Override
    public long getItemId(int position) {
        if (mDataValid && mCursor != null) {
            if (mCursor.moveToPosition(position)) {
                return mCursor.getLong(0);
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    /**
     * Called when the {@link ContentObserver} on the cursor receives a change notification.
     * The default implementation provides the auto-requery logic, but may be overridden by
     * sub classes.
     *
     * @see ContentObserver#onChange(boolean)
     */
    protected void onContentChanged() {
        if (mCursor != null && !mCursor.isClosed()) {
            if (Config.LOGV) Log.v("Cursor", "Auto requerying " + mCursor + " due to update");
            mDataValid = mCursor.requery();
        }
    }

    private class ChangeObserver extends ContentObserver {
        public ChangeObserver() {
            super(new Handler());
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            onContentChanged();
        }
    }

    private class MyDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            mDataValid = true;
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            mDataValid = false;
            notifyDataSetInvalidated();
        }
    }

}
