package com.soundcloud.android.stream;

import static com.soundcloud.android.storage.provider.ScContentProvider.CollectionItemTypes.LIKE;
import static com.soundcloud.android.storage.provider.ScContentProvider.CollectionItemTypes.REPOST;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.storage.PropertySet;
import com.soundcloud.android.storage.provider.DBHelper;
import com.soundcloud.android.storage.provider.Table;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

import android.database.Cursor;
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

    public Observable<PropertySet> loadStreamItemsAsync(final Urn userUrn) {
        return schedule(Observable.create(new Observable.OnSubscribe<PropertySet>() {
            @Override
            public void call(Subscriber<? super PropertySet> subscriber) {
                final Table table = Table.ACTIVITY_VIEW;

                final String[] projection = {
                        table.name + ".*",
                        userAssociationProjection(LIKE, userUrn.numericId, DBHelper.SoundView.USER_LIKE),
                        userAssociationProjection(REPOST, userUrn.numericId, DBHelper.SoundView.USER_REPOST)
                };
                final Cursor cursor = database.query(table.name, projection, null, null, null, null, null);
                if (cursor != null) {
                    try {
                        while (cursor.moveToNext()) {
                            final PropertySet propertySet = PropertySet.create(cursor.getColumnCount());
                            final int soundId = cursor.getInt(cursor.getColumnIndex(DBHelper.ActivityView.SOUND_ID));
                            final int soundType = cursor.getInt(cursor.getColumnIndex(DBHelper.ActivityView.SOUND_TYPE));
                            final Urn soundUrn = soundType == Playable.DB_TYPE_TRACK ? Urn.forTrack(soundId) : Urn.forPlaylist(soundId);
                            final String soundTitle = cursor.getString(cursor.getColumnIndex(DBHelper.SoundView.TITLE));
                            final String itemType = cursor.getString(cursor.getColumnIndex(DBHelper.ActivityView.TYPE));
                            final boolean reposted = itemType.endsWith("-repost");

                            propertySet.add(StreamItemProperty.SOUND_URN, soundUrn.toString());
                            propertySet.add(StreamItemProperty.SOUND_TITLE, soundTitle);
                            propertySet.add(StreamItemProperty.REPOSTED, reposted);

                            subscriber.onNext(propertySet);
                        }
                        subscriber.onCompleted();
                    } catch (Throwable t) {
                        subscriber.onError(t);
                    } finally {
                        cursor.close();
                    }
                }

            }
        }));
    }

    private static String userAssociationProjection(int collectionType, long userId, String colName) {
        return "EXISTS (SELECT 1 FROM " + Table.COLLECTION_ITEMS + ", " + Table.SOUNDS.name
                + " WHERE " + DBHelper.ActivityView.SOUND_ID + " = " + DBHelper.CollectionItems.ITEM_ID
                + " AND " + DBHelper.ActivityView.SOUND_TYPE + " = " + DBHelper.CollectionItems.RESOURCE_TYPE
                + " AND " + DBHelper.CollectionItems.COLLECTION_TYPE + " = " + collectionType
                + " AND " + Table.COLLECTION_ITEMS.name + "." + DBHelper.CollectionItems.USER_ID + " = " + userId
                + ") AS " + colName;
    }

}
