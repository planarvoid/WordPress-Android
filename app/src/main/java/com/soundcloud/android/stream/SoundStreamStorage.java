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
import com.soundcloud.android.model.TrackUrn;
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
                        SoundView.LIKES_COUNT,
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

    public Observable<TrackUrn> trackUrns() {
        return schedule(Observable.create(new Observable.OnSubscribe<TrackUrn>() {
            @Override
            public void call(Subscriber<? super TrackUrn> subscriber) {
                Query.from(Table.ACTIVITY_VIEW.name)
                        .select(ActivityView.SOUND_ID)
                        .whereEq(ActivityView.CONTENT_ID, Content.ME_SOUND_STREAM.id)
                        .whereEq(ActivityView.SOUND_TYPE, Playable.DB_TYPE_TRACK)
                        .runOn(database).emit(subscriber, new TrackUrnMapper());
            }
        }));
    }

    private Query soundAssociationQuery(int collectionType, long userId, String colName) {
        Query association = Query.from(Table.COLLECTION_ITEMS.name, Table.SOUNDS.name);
        association.joinOn(ActivityView.SOUND_ID, CollectionItems.ITEM_ID);
        association.joinOn(ActivityView.SOUND_TYPE, CollectionItems.RESOURCE_TYPE);
        association.whereEq(CollectionItems.COLLECTION_TYPE, collectionType);
        association.whereEq(Table.COLLECTION_ITEMS.name + "." + CollectionItems.USER_ID, userId);
        return association.exists().as(colName);
    }

    private static final class TrackUrnMapper implements RowMapper<TrackUrn> {
        @Override
        public TrackUrn call(ManagedCursor cursor) {
            return Urn.forTrack(cursor.getLong(ActivityView.SOUND_ID));
        }
    }

    private static final class StreamItemMapper implements RowMapper<PropertySet> {

        @Override
        public PropertySet call(ManagedCursor cursor) {
            final PropertySet propertySet = PropertySet.create(cursor.getColumnCount());

            propertySet.put(PlayableProperty.URN, readSoundUrn(cursor));
            propertySet.put(PlayableProperty.TITLE, cursor.getString(SoundView.TITLE));
            propertySet.put(PlayableProperty.DURATION, cursor.getInt(SoundView.DURATION));
            propertySet.put(PlayableProperty.CREATOR, cursor.getString(SoundView.USERNAME));
            propertySet.put(PlayableProperty.CREATED_AT, cursor.getDateFromTimestamp(ActivityView.CREATED_AT));
            addOptionalPlaylistLike(cursor, propertySet);
            addOptionalLikesCount(cursor, propertySet);
            addOptionalPlayCount(cursor, propertySet);
            addOptionalTrackCount(cursor, propertySet);
            addOptionalReposter(cursor, propertySet);

            return propertySet;
        }

        private void addOptionalPlaylistLike(ManagedCursor cursor, PropertySet propertySet) {
            if (getSoundType(cursor) == Playable.DB_TYPE_PLAYLIST) {
                propertySet.put(PlayableProperty.IS_LIKED, cursor.getBoolean(SoundView.USER_LIKE));
            }
        }

        private void addOptionalPlayCount(ManagedCursor cursor, PropertySet propertySet) {
            if (getSoundType(cursor) == Playable.DB_TYPE_TRACK) {
                propertySet.put(TrackProperty.PLAY_COUNT, cursor.getInt(SoundView.PLAYBACK_COUNT));
            }
        }

        private void addOptionalLikesCount(ManagedCursor cursor, PropertySet propertySet) {
            if (getSoundType(cursor) == Playable.DB_TYPE_PLAYLIST) {
                propertySet.put(PlayableProperty.LIKES_COUNT, cursor.getInt(SoundView.LIKES_COUNT));
            }
        }

        private void addOptionalTrackCount(ManagedCursor cursor, PropertySet propertySet) {
            if (getSoundType(cursor) == Playable.DB_TYPE_PLAYLIST) {
                propertySet.put(PlaylistProperty.TRACK_COUNT, cursor.getInt(SoundView.TRACK_COUNT));
            }
        }

        private void addOptionalReposter(ManagedCursor cursor, PropertySet propertySet) {
            if (cursor.getString(ActivityView.TYPE).endsWith("-repost")) {
                propertySet.put(PlayableProperty.REPOSTER, cursor.getString(ActivityView.USER_USERNAME));
            }
        }

        private Urn readSoundUrn(ManagedCursor cursor) {
            final int soundId = cursor.getInt(ActivityView.SOUND_ID);
            return getSoundType(cursor) == Playable.DB_TYPE_TRACK ? Urn.forTrack(soundId) : Urn.forPlaylist(soundId);
        }
    }

    private static int getSoundType(ManagedCursor cursor) {
        return cursor.getInt(ActivityView.SOUND_TYPE);
    }
}
