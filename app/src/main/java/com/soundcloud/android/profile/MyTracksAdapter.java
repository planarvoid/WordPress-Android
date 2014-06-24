package com.soundcloud.android.profile;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.ScBaseAdapter;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.creators.upload.UploadActivity;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.model.DeprecatedRecordingProfile;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.view.adapters.CellPresenter;
import com.soundcloud.android.view.adapters.PendingRecordingItemPresenter;
import com.soundcloud.android.view.adapters.PlaylistItemPresenter;
import com.soundcloud.android.view.adapters.TrackItemPresenter;
import com.soundcloud.propeller.PropertySet;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MyTracksAdapter extends ScBaseAdapter<ScResource> {

    private static final int TYPE_PENDING_RECORDING = 0;
    private static final int TYPE_NEW_TRACK = 1;
    private static final int TYPE_NEW_PLAYLIST = 2;

    private Cursor cursor;
    private boolean dataValid;
    private List<Recording> recordingData;
    private ChangeObserver changeObserver;
    private final ScActivity activity;

    @Inject PlaybackOperations playbackOperations;
    @Inject TrackItemPresenter trackItemPresenter;
    @Inject PlaylistItemPresenter playlistItemPresenter;
    @Inject ImageOperations imageOperations;
    @Inject PendingRecordingItemPresenter pendingRecordingItemPresenter;

    public MyTracksAdapter(ScActivity activity) {
        super(Content.ME_SOUNDS.uri);
        this.activity = activity;
        ContentResolver contentResolver = activity.getApplicationContext().getContentResolver();
        refreshCursor(contentResolver);

        changeObserver = new ChangeObserver(activity);
        contentResolver.registerContentObserver(Content.RECORDINGS.uri, true, changeObserver);

        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public int getViewTypeCount() {
        return 4;
    }

    @Override
    public int getItemViewType(int position) {
        int type = super.getItemViewType(position);
        if (type == IGNORE_ITEM_VIEW_TYPE) return type;

        if (isPendingRecording(position)) {
            return TYPE_PENDING_RECORDING;
        } else if (isNewTrackPresenter(position)) {
            return TYPE_NEW_TRACK;
        } else {
            return TYPE_NEW_PLAYLIST;
        }
    }

    private boolean isPendingRecording(int position) {
        return position < getPendingRecordingsCount();
    }

    private boolean isNewTrackPresenter(int position) {
        final ScResource item = getItem(position);
        return item instanceof SoundAssociation && ((SoundAssociation) item).getPlayable() instanceof Track;
    }

    @Override
    protected View createRow(Context context, int position, ViewGroup parent) {
        if (getItemViewType(position) == TYPE_PENDING_RECORDING) {
            return pendingRecordingItemPresenter.createItemView(position, parent);
        } else {
            return getCellPresenter(position).createItemView(position, parent);
        }
    }

    @Override
    protected void bindRow(int position, View rowView) {
        if (getItemViewType(position) == TYPE_PENDING_RECORDING) {
            pendingRecordingItemPresenter.bindItemView(position, rowView, recordingData);
        } else {
            getCellPresenter(position).bindItemView(position - getPendingRecordingsCount(), rowView, toPropertySet(getItems()));
        }
    }

    private CellPresenter<PropertySet> getCellPresenter(final int position) {
        if (isNewTrackPresenter(position)) {
            return trackItemPresenter;
        } else {
            return playlistItemPresenter;
        }
    }

    private List<PropertySet> toPropertySet(List<? extends ScResource> items) {
        List<PropertySet> propertySets = new ArrayList<PropertySet>(items.size());
        for (ScResource resource : items) {
            final PropertySet propertySet;
            if (resource instanceof SoundAssociation) {
                Playable playable = ((SoundAssociation) resource).getPlayable();
                propertySet = toPropertySet(playable);
            } else {
                throw new IllegalArgumentException("Items is not a SoundAssociation. Item : " + resource);
            }
            propertySets.add(propertySet);
        }
        return propertySets;
    }

    private PropertySet toPropertySet(Playable playable) {
        PropertySet propertySet = playable.toPropertySet();
        if (activity instanceof ProfileActivity) {
            User reposter = ((ProfileActivity) activity).getUser();
            propertySet.put(PlayableProperty.REPOSTER, reposter.getUsername());
        }
        return propertySet;
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

    public int getPendingRecordingsCount() {
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
     * @param activity
     * @see ContentObserver#onChange(boolean)
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
        if (getItemViewType(position) == TYPE_PENDING_RECORDING) {
            final Recording r = (Recording) getItem(position);
            if (r.upload_status == Recording.Status.UPLOADING) {
                context.startActivity(r.getMonitorIntent());
            } else {
                context.startActivity(new Intent(context, (r.external_upload ? UploadActivity.class : RecordActivity.class)).setData(r.toUri()));
            }
        } else {
            playbackOperations.playFromAdapter(context, data, position - recordingData.size(), contentUri, screen);
        }
        return ItemClickResults.LEAVING;
    }

    public void onDestroy() {
        Context context = changeObserver.contextRef.get();
        if (context != null) {
            context.getContentResolver().unregisterContentObserver(changeObserver);
        }
    }

    private class ChangeObserver extends ContentObserver {
        private WeakReference<ScActivity> contextRef;

        public ChangeObserver(ScActivity activity) {
            super(new Handler());
            contextRef = new WeakReference<ScActivity>(activity);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            ScActivity activity = contextRef.get();
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