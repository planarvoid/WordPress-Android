package com.soundcloud.android.stream;

import static com.soundcloud.android.storage.CollectionStorage.CollectionItemTypes.LIKE;
import static com.soundcloud.android.storage.CollectionStorage.CollectionItemTypes.REPOST;
import static com.soundcloud.android.storage.ManagedCursor.RowMapper;
import static com.soundcloud.android.storage.TableColumns.ActivityView;
import static com.soundcloud.android.storage.TableColumns.CollectionItems;
import static com.soundcloud.android.storage.TableColumns.SoundView;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.storage.ManagedCursor;
import com.soundcloud.android.storage.PropertySet;
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

    private static final String TAG = "SoundStreamStorage";

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

    public Observable<PropertySet> loadStreamItemsAsync(
            final Urn userUrn, final long timestamp, final int limit) {
        return schedule(Observable.create(new Observable.OnSubscribe<PropertySet>() {
            @Override
            public void call(Subscriber<? super PropertySet> subscriber) {
                final Query query = Query.from(Table.ACTIVITY_VIEW.name);
                query.select(
                        ActivityView.SOUND_ID,
                        ActivityView.SOUND_TYPE,
                        SoundView.TITLE,
                        ActivityView.CREATED_AT,
                        ActivityView.TYPE,
                        ActivityView.USER_USERNAME,
                        soundAssociationQuery(LIKE, userUrn.numericId, SoundView.USER_LIKE),
                        soundAssociationQuery(REPOST, userUrn.numericId, SoundView.USER_REPOST)
                );
                query.whereLt(ActivityView.CREATED_AT, timestamp);
                query.whereEq(ActivityView.CONTENT_ID, Content.ME_SOUND_STREAM.id);
                query.limit(limit);

                query.runOn(database).emit(subscriber, new StreamItemMapper());
            }
        }));
    }

    private static Query soundAssociationQuery(int collectionType, long userId, String colName) {
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

            propertySet.add(StreamItemProperty.SOUND_URN, readSoundUrn(cursor));
            propertySet.add(StreamItemProperty.SOUND_TITLE, cursor.getString(SoundView.TITLE));
            propertySet.add(StreamItemProperty.CREATED_AT, cursor.getDateFromTimestamp(ActivityView.CREATED_AT));
            propertySet.add(StreamItemProperty.POSTER, cursor.getString(ActivityView.USER_USERNAME));
            propertySet.add(StreamItemProperty.REPOST, readRepostedFlag(cursor));

            return propertySet;
        }

        private Urn readSoundUrn(ManagedCursor cursor) {
            final int soundId = cursor.getInt(ActivityView.SOUND_ID);
            final int soundType = cursor.getInt(ActivityView.SOUND_TYPE);
            return soundType == Playable.DB_TYPE_TRACK ? Urn.forTrack(soundId) : Urn.forPlaylist(soundId);
        }

        private boolean readRepostedFlag(ManagedCursor cursor) {
            return cursor.getString(ActivityView.TYPE).endsWith("-repost");
        }
    };

}
