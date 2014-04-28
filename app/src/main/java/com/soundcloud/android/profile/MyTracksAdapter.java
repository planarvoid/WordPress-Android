
package com.soundcloud.android.profile;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.collections.ScBaseAdapter;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.creators.upload.UploadActivity;
import com.soundcloud.android.model.DeprecatedRecordingProfile;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.collections.views.IconLayout;
import com.soundcloud.android.collections.views.PlayableRow;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MyTracksAdapter extends ScBaseAdapter<ScResource> {
    private Cursor cursor;
    private boolean dataValid;
    private List<Recording> recordingData;

    private static final int TYPE_PENDING_RECORDING = 0;
    private static final int TYPE_TRACK = 1;
    private ChangeObserver changeObserver;
    private PlaybackOperations playbackOperations;
    private ImageOperations imageOperations;

    public MyTracksAdapter(ScActivity activity, ImageOperations imageOperations) {
        super(Content.ME_SOUNDS.uri);
        ContentResolver contentResolver = activity.getApplicationContext().getContentResolver();
        refreshCursor(contentResolver);

        playbackOperations = new PlaybackOperations();
        changeObserver = new ChangeObserver(activity);
        this.imageOperations = imageOperations;
        contentResolver.registerContentObserver(Content.RECORDINGS.uri, true, changeObserver);
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
                new MyTracklistRow(context, imageOperations) : new PlayableRow(context, imageOperations);
    }

    @Override
    protected boolean isPositionOfProgressElement(int position) {
        return isLoadingData && (position == getItemCount());
    }

    @Override
    public int getItemCount() {
        return recordingData == null ? super.getItemCount() : recordingData.size() + super.getItemCount();
    }

    public boolean needsItems() {
        return getCount() == getPendingRecordingsCount();
    }

    public int getPendingRecordingsCount(){
        return recordingData == null ? 0 : recordingData.size();
    }

    private void refreshCursor(ContentResolver contentResolver) {
        cursor = contentResolver.query(Content.RECORDINGS.uri, null,
                TableColumns.Recordings.UPLOAD_STATUS + " < " + Recording.Status.UPLOADED + " OR " + TableColumns.Recordings.UPLOAD_STATUS + " = " + Recording.Status.ERROR,
                null,
                TableColumns.Recordings.TIMESTAMP + " DESC");

        if (cursor != null) {
            dataValid = true;
            recordingData = loadRecordings(cursor);
        } else {
            dataValid = false;
        }

        if (cursor != null) {
            cursor.close();
            cursor = null;
        }

        notifyDataSetChanged();

        // updated recording functionality requires special handling of old recordings
        DeprecatedRecordingProfile.migrateRecordings(recordingData, contentResolver);
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
        if (recordingData != null) {
            return recordingData.size() + super.getCount();
        } else {
            return super.getCount();
        }
    }
    @Override
    public ScResource getItem(int position) {
        if (recordingData != null) {
            if (position < recordingData.size()) {
                return recordingData.get(position);
            } else {
                return super.getItem(position - recordingData.size());
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
        dataValid = false;
        if (activity.isForeground() && cursor == null) {
            refreshCursor(activity.getContentResolver());
        }
        notifyDataSetChanged();
    }

    @Override
    public void onResume(ScActivity activity) {
        if (!dataValid) {
            onContentChanged(activity);
        }
    }

    @Override
    public int handleListItemClick(Context context, int position, long id, Screen screen) {
        if (getItemViewType(position) == TYPE_PENDING_RECORDING){
            final Recording r = (Recording) getItem(position);
            if (r.upload_status == Recording.Status.UPLOADING) {
                context.startActivity(r.getMonitorIntent());
            } else {
                context.startActivity(new Intent(context,(r.external_upload ? UploadActivity.class : RecordActivity.class)).setData(r.toUri()));
            }
        } else {
            playbackOperations.playFromAdapter(context, data, position - recordingData.size(), contentUri, screen);
        }
        return ItemClickResults.LEAVING;
    }

    public void onDestroy() {
        Context context = changeObserver.mContextRef.get();
        if (context != null ) {
            context.getContentResolver().unregisterContentObserver(changeObserver);
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
                "dataValid=" + dataValid +
                ", recordingData=" + recordingData +
                ", data=" + data +
                '}';
    }
}