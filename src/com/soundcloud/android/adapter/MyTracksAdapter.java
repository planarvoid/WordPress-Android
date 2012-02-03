
package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper.Recordings;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.android.view.MyTracklistRow;
import com.soundcloud.android.view.TracklistRow;

import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class MyTracksAdapter extends TracklistAdapter {
    private Cursor mCursor;
    private boolean mDataValid;
    private List<Recording> mRecordingData;

    private static final int TYPE_PENDING_RECORDING = 0;
    private static final int TYPE_TRACK = 1;
    private ChangeObserver mChangeObserver;

    public MyTracksAdapter(ScActivity activity, ArrayList<Parcelable> data,
            Class<?> model) {
        super(activity, data, model);
        refreshCursor();

        mChangeObserver = new ChangeObserver();
        activity.getContentResolver()
                .registerContentObserver(Content.RECORDINGS.uri, true, mChangeObserver);
    }

    @Override
    public int getItemViewType(int position) {
        return (position < getPendingRecordingsCount()) ? TYPE_PENDING_RECORDING : TYPE_TRACK;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    protected LazyRow createRow(int position) {
        return getItemViewType(position) == TYPE_PENDING_RECORDING ? new MyTracklistRow(mContext, this) : new TracklistRow(mContext,this);
    }

    public boolean needsItems() {
        return getCount() == getPendingRecordingsCount();
    }

    public int getPendingRecordingsCount(){
        return mRecordingData == null ? 0 : mRecordingData.size();
    }

    @Override
    public int positionOffset() {
        return getPendingRecordingsCount();
    }

    private void refreshCursor() {
        mCursor = mContext.getContentResolver().query(Content.RECORDINGS.uri, null,
                Recordings.UPLOAD_STATUS + " < 2",
                null,
                Recordings.TIMESTAMP + " DESC");

        if (mCursor != null) {
            mDataValid = true;
            mRecordingData = loadRecordings(mCursor);
        } else {
            mDataValid = false;
        }

        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
    }

    private List<Recording> loadRecordings(Cursor cursor) {
        List<Recording> recordings = new ArrayList<Recording>();
        while (cursor != null && cursor.moveToNext()) {
            recordings.add(new Recording(cursor));
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
                mContext.getContentResolver().update(r.toUri(), r.buildContentValues(), null, null);
                changed = true;
            }
        }
        if (changed)  onContentChanged();
    }

    @Override
    public void reset() {
        super.reset();
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
            if (position < mRecordingData.size()) {
                return mRecordingData.get(position);
            } else {
                return super.getItem(position - mRecordingData.size());
            }
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
        if (mCursor == null) {
            refreshCursor();
            notifyDataSetChanged();
        }
    }

    public void onDestroy(){
        mContext.getContentResolver().unregisterContentObserver(mChangeObserver);
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