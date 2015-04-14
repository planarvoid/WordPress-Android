package com.soundcloud.android.profile;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.legacy.model.DeprecatedRecordingProfile;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.api.legacy.model.behavior.PlayableHolder;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.creators.upload.UploadActivity;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.storage.provider.ScContentProvider;
import com.soundcloud.android.tracks.TrackItemPresenter;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.view.adapters.CellPresenter;
import com.soundcloud.android.view.adapters.LegacyAdapterBridge;
import com.soundcloud.android.view.adapters.PlaylistItemPresenter;
import com.soundcloud.propeller.PropertySet;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import javax.inject.Provider;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

@Deprecated
public class MyTracksAdapter extends LegacyAdapterBridge<PublicApiResource> {

    private static final int TYPE_PENDING_RECORDING = 0;
    private static final int TYPE_NEW_TRACK = 1;
    private static final int TYPE_NEW_PLAYLIST = 2;

    private Cursor cursor;
    private boolean dataValid;
    private List<Recording> recordingData;
    private final ChangeObserver changeObserver;
    private final ScActivity activity;

    @Inject PlaybackOperations playbackOperations;
    @Inject TrackItemPresenter trackItemPresenter;
    @Inject PlaylistItemPresenter playlistItemPresenter;
    @Inject PendingRecordingItemPresenter pendingRecordingItemPresenter;
    @Inject EventBus eventBus;
    @Inject Provider<ExpandPlayerSubscriber> subscriberProvider;

    private Subscription eventSubscriptions = Subscriptions.empty();

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
        if (type == IGNORE_ITEM_VIEW_TYPE) {
            return type;
        }

        if (isPendingRecording(position)) {
            return TYPE_PENDING_RECORDING;
        } else if (isTrack(position)) {
            return TYPE_NEW_TRACK;
        } else {
            return TYPE_NEW_PLAYLIST;
        }
    }

    private boolean isPendingRecording(int position) {
        return position < getPendingRecordingsCount();
    }

    private int getPositionExcludingRecordings(int position) {
        return position - getPendingRecordingsCount();
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
            getCellPresenter(position).bindItemView(getPositionExcludingRecordings(position), rowView, (List) listItems);
        }
    }

    private CellPresenter<? extends PlayableItem> getCellPresenter(final int position) {
        if (isTrack(position)) {
            return trackItemPresenter;
        } else {
            return playlistItemPresenter;
        }
    }

    @Override
    protected boolean isTrack(int position) {
        return super.isTrack(getPositionExcludingRecordings(position));
    }

    @Override
    protected PropertySet toPropertySet(PublicApiResource resource) {
        SoundAssociation soundAssociation = (SoundAssociation) resource;
        PropertySet propertySet = soundAssociation.getPlayable().toPropertySet();
        if (soundAssociation.associationType == ScContentProvider.CollectionItemTypes.REPOST
                && activity instanceof ProfileActivity) {
            PublicApiUser reposter = ((ProfileActivity) activity).getUser();
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
    public PublicApiResource getItem(int position) {
        if (recordingData != null) {
            if (position < recordingData.size()) {
                return recordingData.get(position);
            } else {
                return super.getItem(getPositionExcludingRecordings(position));
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
    public int handleListItemClick(Context context, int position, long id, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        if (getItemViewType(position) == TYPE_PENDING_RECORDING) {
            final Recording r = (Recording) getItem(position);
            if (r.upload_status == Recording.Status.UPLOADING) {
                context.startActivity(r.getMonitorIntent());
            } else {
                context.startActivity(new Intent(context, (r.external_upload ? UploadActivity.class : RecordActivity.class)).setData(r.toUri()));
            }
        } else {
            playTrack(context, position, screen);
        }
        return ItemClickResults.LEAVING;
    }

    private void playTrack(Context context, int position, Screen screen) {
        int positionExcludingRecordings = getPositionExcludingRecordings(position);
        Playable playable = ((PlayableHolder) data.get(positionExcludingRecordings)).getPlayable();
        if (playable instanceof PublicApiTrack) {
            List<Urn> trackUrns = toTrackUrn(filterPlayables(data));
            int adjustedPosition = filterPlayables(data.subList(0, positionExcludingRecordings)).size();
            Urn initialTrack = trackUrns.get(adjustedPosition);
            playbackOperations
                    .playTracksFromUri(contentUri, adjustedPosition, initialTrack, new PlaySessionSource(screen))
                    .subscribe(subscriberProvider.get());
        } else if (playable instanceof PublicApiPlaylist) {
            PlaylistDetailActivity.start(context, playable.getUrn(), Screen.SIDE_MENU_STREAM);
        }
    }

    public void onDestroy() {
        Context context = changeObserver.contextRef.get();
        if (context != null) {
            context.getContentResolver().unregisterContentObserver(changeObserver);
        }
    }

    private class ChangeObserver extends ContentObserver {
        private final WeakReference<ScActivity> contextRef;

        public ChangeObserver(ScActivity activity) {
            super(new Handler());
            contextRef = new WeakReference<>(activity);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            ScActivity activity = contextRef.get();
            if (activity != null) {
                onContentChanged(activity);
            }
        }
    }

    @Override
    public void onViewCreated() {
        eventSubscriptions = new CompositeSubscription(
                eventBus.subscribe(EventQueue.PLAY_QUEUE_TRACK, new UpdatePlayingTrackSubscriber(this, trackItemPresenter)),
                eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new PlayableChangedSubscriber())
        );
    }

    @Override
    public void onDestroyView() {
        eventSubscriptions.unsubscribe();
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
