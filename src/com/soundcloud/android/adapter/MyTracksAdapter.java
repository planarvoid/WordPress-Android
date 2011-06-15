
package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.provider.DatabaseHelper.Content;
import com.soundcloud.android.provider.DatabaseHelper.Recordings;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.android.view.MyTracklistRow;

import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Config;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class MyTracksAdapter extends TracklistAdapter {
    private Cursor mCursor;
    private boolean mDataValid;
    private ChangeObserver mChangeObserver;
    private List<Recording> mRecordingData;

    public MyTracksAdapter(ScActivity activity, ArrayList<Parcelable> data,
            Class<?> model) {
        super(activity, data, model);
        refreshCursor();
    }

    @Override
    protected LazyRow createRow() {
        return new MyTracklistRow(mActivity, this);
    }

    public int getPendingRecordingsCount(){
        return mRecordingData == null ? 0 : mRecordingData.size();
    }

    private void refreshCursor() {
        if (mCursor != null) {
            mCursor.unregisterContentObserver(mChangeObserver);
            mCursor.close();
        }

        mCursor = mActivity.getContentResolver().query(Content.RECORDINGS, null,
                Recordings.USER_ID + "= ? AND " + Recordings.UPLOAD_STATUS + " < 2", new String[] {Long.toString(mActivity.getCurrentUserId())},
                Recordings.TIMESTAMP + " DESC");

        mChangeObserver = new ChangeObserver();
        if (mCursor != null) {
            mDataValid = true;
            mCursor.registerContentObserver(mChangeObserver);
            mRecordingData = loadRecordings(mCursor);
        } else {
            mDataValid = false;
        }
    }

    private List<Recording> loadRecordings(Cursor cursor) {
        List<Recording> recordings = new ArrayList<Recording>();
        if (cursor != null && !cursor.isClosed()) {
            while (cursor.moveToNext()) {
                recordings.add(new Recording(cursor));
            }
        }
        return recordings;
    }

    /*
     * fix false upload statuses that may have resulted from a crash
     */
    public void checkUploadStatus(long uploadId) {
        if (mRecordingData == null || mRecordingData.size() == 0) return;

        boolean changed = false;
        for (Recording r : mRecordingData) {
            if (r.upload_status == 1 && uploadId != r.id) {
                r.upload_status = 0;
                changed = true;
            }
        }
        if (changed)  notifyDataSetChanged();
    }

    @Override
    public void reset() {
        mPage = 1;
        submenuIndex = -1;
        animateSubmenuIndex = -1;
        refreshCursor();
    }

    @Override
    public int getCount() {
        if (mDataValid && mRecordingData != null) {
            return mRecordingData.size() + super.getCount();
        } else {
            return super.getCount();
        }
    }
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

        submenuIndex = -1;
        animateSubmenuIndex = -1;
        mRecordingData = loadRecordings(mCursor);
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
}