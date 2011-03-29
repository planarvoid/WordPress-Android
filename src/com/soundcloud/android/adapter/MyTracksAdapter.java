
package com.soundcloud.android.adapter;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.objects.Recording;
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

    protected ArrayList<Recording> mRecordingData;


    public MyTracksAdapter(ScActivity activity, ArrayList<Parcelable> data) {
        super(activity, data);
        refreshCursor();
    }

    @Override
    protected LazyRow createRow() {
        return new MyTracklistRow(mActivity, this);
    }


    public void refreshCursor() {

        if (mCursor != null) {
            mCursor.unregisterContentObserver(mChangeObserver);
            mCursor.close();
        }

        mCursor = mActivity.getContentResolver().query(Recordings.CONTENT_URI, null,
                Recordings.USER_ID + "='" + CloudUtils.getCurrentUserId(mActivity) + "' AND " + Recordings.UPLOAD_STATUS + " < 2", null,
                null);

        mChangeObserver = new ChangeObserver();
        if (mCursor != null) {
            mDataValid = true;
            mCursor.registerContentObserver(mChangeObserver);
            loadCursor();
        } else
            mDataValid = false;
    }

    private void loadCursor(){
        mRecordingData = new ArrayList<Recording>();
        if (mCursor == null || mCursor.isClosed() || mCursor.getCount() == 0)
            return;

        while(mCursor.moveToNext()){
            mRecordingData.add(new Recording(mCursor));
        }
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
        if (mDataValid && mRecordingData != null) {
            return mRecordingData.size() + super.getCount();
        } else {
            return super.getCount();
        }
    }

    /**
     * @see android.widget.ListAdapter#getItem(int)
     */
    @Override
    public Object getItem(int position) {
        if (mDataValid && mRecordingData != null) {
            if (position < mRecordingData.size()){
                return mRecordingData.get(position);
            } else
                return super.getItem(position - mRecordingData.size());
        } else {
            return super.getItem(position);
        }
    }

    /**
     * @see android.widget.ListAdapter#getItemId(int)
     */
    @Override
    public long getItemId(int position) {
        if (mDataValid && mRecordingData != null) {
            if (position < mRecordingData.size()){
                return mRecordingData.get(position).id;
            } else {
                return super.getItemId(position - mRecordingData.size());
            }
        } else {
            return super.getItemId(position);
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
        loadCursor();
        notifyDataSetChanged();
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