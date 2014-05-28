package com.soundcloud.android.stream;

import static com.soundcloud.android.storage.CollectionStorage.CollectionItemTypes.LIKE;
import static com.soundcloud.android.storage.CollectionStorage.CollectionItemTypes.REPOST;
import static com.soundcloud.android.storage.ManagedCursor.RowMapper;
import static com.soundcloud.android.storage.TableColumns.ActivityView;
import static com.soundcloud.android.storage.TableColumns.CollectionItems;
import static com.soundcloud.android.storage.TableColumns.SoundView;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PlaylistProperty;
import com.soundcloud.android.model.PropertySet;
import com.soundcloud.android.model.TrackProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.storage.ManagedCursor;
import com.soundcloud.android.storage.Query;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.provider.Content;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

import android.database.sqlite.SQLiteDatabase;

import javax.inject.Inject;
import javax.inject.Named;

class SoundStreamStorage extends ScheduledOperations {

    private final SQLiteDatabase database;

    @Inject
    public SoundStreamStorage(@Named("read-only") SQLiteDatabase database) {
        this(database, ScSchedulers.STORAGE_SCHEDULER);
    }

    @VisibleForTesting
    SoundStreamStorage(SQLiteDatabase database, Scheduler scheduler) {
        super(scheduler);
        this.database = database;
    }

    public Observable<PropertySet> streamItemsBefore(final long timestamp, final Urn userUrn, final int limit) {
        return schedule(Observable.create(new Observable.OnSubscribe<PropertySet>() {
            @Override
            public void call(Subscriber<? super PropertySet> subscriber) {
                final Query query = Query.from(Table.ACTIVITY_VIEW.name);
                query.select(
                        ActivityView.SOUND_ID,
                        ActivityView.SOUND_TYPE,
                        SoundView.TITLE,
                        SoundView.USERNAME,
                        SoundView.DURATION,
                        SoundView.PLAYBACK_COUNT,
                        SoundView.TRACK_COUNT,
                        ActivityView.CREATED_AT,
                        ActivityView.TYPE,
                        ActivityView.USER_USERNAME,
                        soundAssociationQuery(LIKE, userUrn.numericId, SoundView.USER_LIKE),
                        soundAssociationQuery(REPOST, userUrn.numericId, SoundView.USER_REPOST)
                );
                query.whereEq(ActivityView.CONTENT_ID, Content.ME_SOUND_STREAM.id);
                query.whereLt(ActivityView.CREATED_AT, timestamp);
                query.limit(limit);

                query.runOn(database).emit(subscriber, new StreamItemMapper());
            }
        }));
    }

    private Query soundAssociationQuery(int collectionType, long userId, String colName) {
        Query association = Query.from(Table.COLLECTION_ITEMS.name, Table.SOUNDS.name);
        association.whereEq(ActivityView.SOUND_ID, CollectionItems.ITEM_ID);
        association.whereEq(ActivityView.SOUND_TYPE, CollectionItems.RESOURCE_TYPE);
        association.whereEq(CollectionItems.COLLECTION_TYPE, collectionType);
        association.whereEq(Table.COLLECTION_ITEMS.name + "." + CollectionItems.USER_ID, userId);
        return association.exists().as(colName);
    }

    private static final class StreamItemMapper implements RowMapper<PropertySet> {

        @Override
        public PropertySet call(ManagedCursor cursor) {
            final PropertySet propertySet = PropertySet.create(cursor.getColumnCount());

            propertySet.add(PlayableProperty.URN, readSoundUrn(cursor));
            propertySet.add(PlayableProperty.TITLE, cursor.getString(SoundView.TITLE));
            propertySet.add(PlayableProperty.DURATION, cursor.getInt(SoundView.DURATION));
            propertySet.add(PlayableProperty.CREATOR, cursor.getString(SoundView.USERNAME));
            propertySet.add(PlayableProperty.REPOSTED_AT, cursor.getDateFromTimestamp(ActivityView.CREATED_AT));
            addOptionalPlayCount(cursor, propertySet);
            addOptionalTrackCount(cursor, propertySet);
            addOptionalReposter(cursor, propertySet);

            return propertySet;
        }

        private void addOptionalPlayCount(ManagedCursor cursor, PropertySet propertySet) {
            final int playCount = cursor.getInt(SoundView.PLAYBACK_COUNT);
            if (playCount > -1) {
                propertySet.add(TrackProperty.PLAY_COUNT, playCount);
            }
        }

        private void addOptionalTrackCount(ManagedCursor cursor, PropertySet propertySet) {
            final int trackCount = cursor.getInt(SoundView.TRACK_COUNT);
            if (trackCount > -1) {
                propertySet.add(PlaylistProperty.TRACK_COUNT, trackCount);
            }
        }

        private void addOptionalReposter(ManagedCursor cursor, PropertySet propertySet) {
            if (cursor.getString(ActivityView.TYPE).endsWith("-repost")) {
                propertySet.add(PlayableProperty.REPOSTER, cursor.getString(ActivityView.USER_USERNAME));
            }
        }

        private Urn readSoundUrn(ManagedCursor cursor) {
            final int soundId = cursor.getInt(ActivityView.SOUND_ID);
            final int soundType = cursor.getInt(ActivityView.SOUND_TYPE);
            return soundType == Playable.DB_TYPE_TRACK ? Urn.forTrack(soundId) : Urn.forPlaylist(soundId);
        }
    }
}
