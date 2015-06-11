package com.soundcloud.android.storage;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.model.Urn;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Deprecated
public class TrackStorage extends ScheduledOperations implements Storage<PublicApiTrack> {
    private TrackDAO trackDAO;
    private ContentResolver resolver;
    private ScModelManager modelManager;

    // Use @Inject instead
    @Deprecated
    public TrackStorage() {
        this(SoundCloudApplication.instance.getContentResolver(),
                new TrackDAO(SoundCloudApplication.instance.getContentResolver()),
                SoundCloudApplication.sModelManager);
    }

    @Inject
    public TrackStorage(ContentResolver contentResolver, TrackDAO trackDAO, ScModelManager modelManager){
        this(contentResolver, trackDAO, modelManager, ScSchedulers.HIGH_PRIO_SCHEDULER);
    }

    @VisibleForTesting
    TrackStorage(ContentResolver resolver, TrackDAO trackDAO, ScModelManager modelManager, Scheduler scheduler){
        super(scheduler);
        this.resolver = resolver;
        this.trackDAO = trackDAO;
        this.modelManager = modelManager;
    }

    @Override
    public PublicApiTrack store(PublicApiTrack track) {
        modelManager.cache(track, PublicApiResource.CacheUpdateMode.FULL);
        trackDAO.create(track);
        return track;
    }

    public Observable<Collection<PublicApiTrack>> storeCollectionAsync(final Collection<PublicApiTrack> tracks) {
        return schedule(Observable.create(new Observable.OnSubscribe<Collection<PublicApiTrack>>() {
            @Override
            public void call(Subscriber<? super Collection<PublicApiTrack>> observer) {
                storeCollection(tracks);
                observer.onNext(tracks);
                observer.onCompleted();
            }
        }));
    }

    private int storeCollection(Collection<PublicApiTrack> tracks) {
        return trackDAO.createCollection(tracks);
    }


    public long createOrUpdate(PublicApiTrack track) {
        return trackDAO.createOrUpdate(track);
    }

    public Observable<PublicApiTrack> getTrackAsync(final long id) {
        Preconditions.checkArgument(id != Consts.NOT_SET, "Trying to load non-existant track");
        return schedule(Observable.create(new Observable.OnSubscribe<PublicApiTrack>() {
            @Override
            public void call(Subscriber<? super PublicApiTrack> observer) {
                try {
                    final PublicApiTrack cachedTrack = modelManager.getCachedTrack(id);
                    if (cachedTrack != null){
                        observer.onNext(cachedTrack);
                    } else {
                        observer.onNext(getTrack(id));
                    }
                    observer.onCompleted();
                } catch (NotFoundException e) {
                    observer.onError(e);
                }
            }
        }));
    }

    public PublicApiTrack getTrack(long id) throws NotFoundException {
        Preconditions.checkArgument(id != Consts.NOT_SET, "Trying to load non-existant track");
        final PublicApiTrack track = trackDAO.queryById(id);
        if (track == null) {
            throw new NotFoundException(id);
        } else {
            return modelManager.cache(track);
        }
    }

    // TODO: this should not depend on content URIs, since we're trying to move away from it. Difficult to do without
    // migrating the front end first to not use content URIs
    @SuppressWarnings("PMD.NPathComplexity")
    public Observable<List<Urn>> getTracksForUriAsync(final Uri uri) {
        return schedule(Observable.create(new Observable.OnSubscribe<List<Urn>>() {
            @Override
            public void call(Subscriber<? super List<Urn>> observer) {

                final boolean isActivityCursor = Content.match(uri).isActivitiesItem();
                final String idColumn;
                final String fullIdColumn;
                final String fullTypeColumn;
                if (Content.match(uri) == Content.ME_SOUNDS) {
                    idColumn = TableColumns.Posts.TARGET_ID;
                    fullIdColumn = Table.Posts.field(TableColumns.Posts.TARGET_ID) + " as " + idColumn;
                    fullTypeColumn = Table.Posts.field(TableColumns.Posts.TARGET_TYPE);
                } else if (isActivityCursor) {
                    idColumn = TableColumns.ActivityView.SOUND_ID;
                    fullIdColumn = Table.ActivityView + "." + TableColumns.ActivityView.SOUND_ID;
                    fullTypeColumn = Table.ActivityView + "." + TableColumns.ActivityView.SOUND_TYPE;
                } else {
                    idColumn = TableColumns.SoundView._ID;
                    fullIdColumn = Table.SoundView + "." + TableColumns.SoundView._ID + " as " + BaseColumns._ID;
                    fullTypeColumn = Table.SoundView + "." + TableColumns.SoundView._TYPE;
                }

                // if playlist, adjust load uri to request the tracks instead of meta_data
                final Uri adjustedUri = (Content.match(uri) == Content.PLAYLIST) ?
                        Content.PLAYLIST_TRACKS.forQuery(uri.getLastPathSegment()) : uri;

                Cursor cursor = resolver.query(adjustedUri, new String[] { fullIdColumn }, fullTypeColumn + " = ?",
                        new String[]{String.valueOf(Playable.DB_TYPE_TRACK)}, null);
                if (!observer.isUnsubscribed()) {
                    try {
                        observer.onNext(toTrackUrns(idColumn, cursor));
                        observer.onCompleted();
                    } finally {
                        cursor.close();
                    }
                }
            }

            private List<Urn> toTrackUrns(String idColumn, Cursor cursor) {
                if (cursor == null) {
                    return Collections.emptyList();
                }

                List<Urn> newQueue = Lists.newArrayListWithExpectedSize(cursor.getCount());
                while (cursor.moveToNext()) {
                    newQueue.add(Urn.forTrack(cursor.getLong(cursor.getColumnIndex(idColumn))));
                }
                return newQueue;
            }
        }));
    }

}


