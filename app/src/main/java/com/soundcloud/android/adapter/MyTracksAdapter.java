
package com.soundcloud.android.adapter;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.activity.create.ScCreate;
import com.soundcloud.android.activity.create.ScUpload;
import com.soundcloud.android.model.DeprecatedRecordingProfile;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper.Recordings;
import com.soundcloud.android.utils.PlayUtils;
import com.soundcloud.android.view.MyTracklistRow;
import com.soundcloud.android.view.adapter.LazyRow;
import com.soundcloud.android.view.adapter.TrackInfoBar;

import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class MyTracksAdapter extends ScBaseAdapter implements PlayableAdapter {
    private Cursor mCursor;
    private boolean mDataValid;
    private List<Recording> mRecordingData;
    private ScActivity mActivity;

    private static final int TYPE_PENDING_RECORDING = 0;
    private static final int TYPE_TRACK = 1;
    private ChangeObserver mChangeObserver;

    public MyTracksAdapter(ScActivity activity, Uri uri) {
        super(activity, uri);
        mActivity = activity;
        refreshCursor();

        mChangeObserver = new ChangeObserver();
        activity.getContentResolver()
                .registerContentObserver(Content.RECORDINGS.uri, true, mChangeObserver);
    }

    @Override
    public int getItemViewType(int position) {
        int type = super.getItemViewType(position);
        if (type == IGNORE_ITEM_VIEW_TYPE) return type;

        return (position < getPendingRecordingsCount()) ? TYPE_PENDING_RECORDING : TYPE_TRACK;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    protected LazyRow createRow(int position) {
        return getItemViewType(position) == TYPE_PENDING_RECORDING ?
                new MyTracklistRow(mContext, this) : new TrackInfoBar(mContext,this);
    }

    @Override
    protected boolean isPositionOfProgressElement(int position) {
        return mIsLoadingData && (position == getItemCount());
    }

    @Override
    public int getItemCount() {
        return mRecordingData == null ? super.getItemCount() : mRecordingData.size() + super.getItemCount();
    }

    public boolean needsItems() {
        return getCount() == getPendingRecordingsCount();
    }

    public int getPendingRecordingsCount(){
        return mRecordingData == null ? 0 : mRecordingData.size();
    }

    private void refreshCursor() {
        mCursor = mContext.getContentResolver().query(Content.RECORDINGS.uri, null,
                Recordings.UPLOAD_STATUS + " < " + Recording.Status.UPLOADED + " OR " + Recordings.UPLOAD_STATUS + " = " + Recording.Status.ERROR,
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

        notifyDataSetChanged();

        // updated recording functionality requires special handling of old recordings
        DeprecatedRecordingProfile.migrateRecordings(mRecordingData, mContext.getContentResolver());
    }


    private List<Recording> loadRecordings(Cursor cursor) {
        List<Recording> recordings = new ArrayList<Recording>();
        while (cursor != null && cursor.moveToNext()) {
            recordings.add(new Recording(cursor));
        }
        return recordings;
    }

    @Override
    public void clearData() {
        super.clearData();
        refreshCursor();
    }

    @Override
    public int getCount() {
        if (mRecordingData != null) {
            return mRecordingData.size() + super.getCount();
        } else {
            return super.getCount();
        }
    }
    @Override
    public ScModel getItem(int position) {
        if (mRecordingData != null) {
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
        if (mRecordingData != null) {
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
        mDataValid = false;
        if (mActivity.isForeground() && mCursor == null) {
            refreshCursor();
        }
        notifyDataSetChanged();
    }

    public void onResume() {
        if (!mDataValid) {
            onContentChanged();
        }
    }

    @Override
    public int handleListItemClick(int position, long id) {
        if (getItemViewType(position) == TYPE_PENDING_RECORDING){
            final Recording r = (Recording) getItem(position);
            if (r.upload_status == Recording.Status.UPLOADING) {
                mContext.startActivity(r.getMonitorIntent());
            } else {
                mContext.startActivity(new Intent(mContext,(r.external_upload ? ScUpload.class : ScCreate.class)).setData(r.toUri()));
            }
        } else {
            PlayUtils.playFromAdapter(mContext, this, mData, position - mRecordingData.size(), getItem(position).id);
        }
        return ItemClickResults.LEAVING;
    }

    @Override
    public Uri getPlayableUri() {
        return mContentUri;
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

    @Override
    public String toString() {
        return "MyTracksAdapter{" +
                "mDataValid=" + mDataValid +
                ", mRecordingData=" + mRecordingData +
                ", mData=" + mData +
                '}';
    }
}