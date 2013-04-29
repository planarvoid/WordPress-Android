
package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.create.ScCreate;
import com.soundcloud.android.activity.create.ScUpload;
import com.soundcloud.android.model.DeprecatedRecordingProfile;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper.Recordings;
import com.soundcloud.android.utils.PlayUtils;
import com.soundcloud.android.view.MyTracklistRow;
import com.soundcloud.android.view.adapter.IconLayout;
import com.soundcloud.android.view.adapter.PlayableRow;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MyTracksAdapter extends ScBaseAdapter implements PlayableAdapter {
    private Cursor mCursor;
    private boolean mDataValid;
    private List<Recording> mRecordingData;

    private static final int TYPE_PENDING_RECORDING = 0;
    private static final int TYPE_TRACK = 1;
    private ChangeObserver mChangeObserver;

    public MyTracksAdapter(ScActivity activity, Uri uri) {
        super(activity, uri);
        refreshCursor(activity.getContentResolver());

        mChangeObserver = new ChangeObserver(activity);
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
    protected IconLayout createRow(Context context, int position) {
        return getItemViewType(position) == TYPE_PENDING_RECORDING ?
                new MyTracklistRow(context) : new PlayableRow(context);
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

    private void refreshCursor(ContentResolver contentResolver) {
        mCursor = contentResolver.query(Content.RECORDINGS.uri, null,
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
        DeprecatedRecordingProfile.migrateRecordings(mRecordingData, contentResolver);
    }


    private List<Recording> loadRecordings(Cursor cursor) {
        List<Recording> recordings = new ArrayList<Recording>();
        while (cursor != null && cursor.moveToNext()) {
            recordings.add(new Recording(cursor));
        }
        return recordings;
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

    /**
     * Called when the {@link ContentObserver} on the cursor receives a change notification.
     * The default implementation provides the auto-requery logic, but may be overridden by
     * sub classes.
     *
     * @see ContentObserver#onChange(boolean)
     * @param activity
     */
    protected void onContentChanged(ScActivity activity) {
        mDataValid = false;
        if (activity.isForeground() && mCursor == null) {
            refreshCursor(activity.getContentResolver());
        }
        notifyDataSetChanged();
    }

    @Override
    public void onResume(ScActivity activity) {
        if (!mDataValid) {
            onContentChanged(activity);
        }
    }

    @Override
    public int handleListItemClick(Context context, int position, long id) {
        if (getItemViewType(position) == TYPE_PENDING_RECORDING){
            final Recording r = (Recording) getItem(position);
            if (r.upload_status == Recording.Status.UPLOADING) {
                context.startActivity(r.getMonitorIntent());
            } else {
                context.startActivity(new Intent(context,(r.external_upload ? ScUpload.class : ScCreate.class)).setData(r.toUri()));
            }
        } else {
            PlayUtils.playFromAdapter(context, this, mData, position - mRecordingData.size());
        }
        return ItemClickResults.LEAVING;
    }

    @Override
    public Uri getPlayableUri() {
        return mContentUri;
    }

    @Override
    public Playable getPlayable(int position) {
        if (mRecordingData != null) {
            if (position < mRecordingData.size()) {
                return null;
            } else {
                return (Playable) super.getItem(position - mRecordingData.size());
            }
        } else {
            return (Playable) super.getItem(position);
        }
    }

    private class ChangeObserver extends ContentObserver {
        private WeakReference<ScActivity> mContextRef;

        public ChangeObserver(ScActivity activity) {
            super(new Handler());
            mContextRef = new WeakReference<ScActivity>(activity);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            ScActivity activity = mContextRef.get();
            if (activity != null) onContentChanged(activity);
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