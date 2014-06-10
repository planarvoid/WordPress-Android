package com.soundcloud.android.track;

import static com.soundcloud.android.storage.CollectionStorage.CollectionItemTypes.LIKE;
import static com.soundcloud.android.storage.CollectionStorage.CollectionItemTypes.REPOST;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PropertySet;
import com.soundcloud.android.model.TrackProperty;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UserUrn;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.storage.ManagedCursor;
import com.soundcloud.android.storage.Query;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

import android.database.sqlite.SQLiteDatabase;

import javax.inject.Inject;
import javax.inject.Named;

public class TrackStorage extends ScheduledOperations {

    private final SQLiteDatabase database;

    @Inject
    public TrackStorage(@Named("read-only") SQLiteDatabase database) {
        this(database, ScSchedulers.STORAGE_SCHEDULER);
    }

    @VisibleForTesting
    TrackStorage(SQLiteDatabase database, Scheduler scheduler) {
        super(scheduler);
        this.database = database;
    }

    public Observable<PropertySet> track(final TrackUrn trackUrn, final UserUrn loggedInUserUrn) {
        return schedule(Observable.create(new Observable.OnSubscribe<PropertySet>() {
            @Override
            public void call(Subscriber<? super PropertySet> subscriber) {
                final Query query = Query.from(Table.SOUND_VIEW.name);
                query.select(
                        TableColumns.SoundView._ID,
                        TableColumns.SoundView.TITLE,
                        TableColumns.SoundView.USERNAME,
                        TableColumns.SoundView.DURATION,
                        TableColumns.SoundView.PLAYBACK_COUNT,
                        TableColumns.SoundView.TRACK_COUNT,
                        TableColumns.SoundView.LIKES_COUNT,
                        soundAssociationQuery(LIKE, loggedInUserUrn.numericId, TableColumns.SoundView.USER_LIKE),
                        soundAssociationQuery(REPOST, loggedInUserUrn.numericId, TableColumns.SoundView.USER_REPOST)
                );
                query.whereEq(TableColumns.SoundView._ID, trackUrn.numericId);
                query.runOn(database).emit(subscriber, new TrackItemMapper());
            }
        }));
    }

    /**
     * TODO: Duplicate code {@link com.soundcloud.android.stream.SoundStreamStorage#soundAssociationQuery(int, long, String)}
      */

    private Query soundAssociationQuery(int collectionType, long userId, String colName) {
        Query association = Query.from(Table.COLLECTION_ITEMS.name, Table.SOUNDS.name);
        association.joinOn(TableColumns.SoundView._ID, TableColumns.CollectionItems.ITEM_ID);
        association.joinOn(TableColumns.SoundView._TYPE, TableColumns.CollectionItems.RESOURCE_TYPE);
        association.whereEq(TableColumns.CollectionItems.COLLECTION_TYPE, collectionType);
        association.whereEq(TableColumns.CollectionItems.RESOURCE_TYPE, Playable.DB_TYPE_TRACK);
        association.whereEq(Table.COLLECTION_ITEMS.name + "." + TableColumns.CollectionItems.USER_ID, userId);
        return association.exists().as(colName);
    }


    private static final class TrackItemMapper implements ManagedCursor.RowMapper<PropertySet> {

        @Override
        public PropertySet call(ManagedCursor cursor) {
            final PropertySet propertySet = PropertySet.create(cursor.getColumnCount());

            propertySet.put(TrackProperty.URN, readSoundUrn(cursor));
            propertySet.put(PlayableProperty.TITLE, cursor.getString(TableColumns.SoundView.TITLE));
            propertySet.put(PlayableProperty.DURATION, cursor.getInt(TableColumns.SoundView.DURATION));
            propertySet.put(PlayableProperty.CREATOR, cursor.getString(TableColumns.SoundView.USERNAME));
            propertySet.put(TrackProperty.PLAY_COUNT, cursor.getInt(TableColumns.SoundView.PLAYBACK_COUNT));
            propertySet.put(PlayableProperty.LIKES_COUNT, cursor.getInt(TableColumns.SoundView.LIKES_COUNT));
            return propertySet;
        }

        private TrackUrn readSoundUrn(ManagedCursor cursor) {
            return Urn.forTrack(cursor.getInt(TableColumns.SoundView._ID));
        }
    }
}
