package com.soundcloud.android.stream;

import static com.soundcloud.android.storage.TableColumns.ActivityView;
import static com.soundcloud.android.storage.TableColumns.CollectionItems;
import static com.soundcloud.android.storage.TableColumns.SoundView;
import static com.soundcloud.android.storage.provider.ScContentProvider.CollectionItemTypes.LIKE;
import static com.soundcloud.android.storage.provider.ScContentProvider.CollectionItemTypes.REPOST;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.storage.PropertySet;
import com.soundcloud.android.storage.Table;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;

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
                        ActivityView.SOUND_ID,
                        ActivityView.SOUND_TYPE,
                        SoundView.TITLE,
                        ActivityView.CREATED_AT,
                        ActivityView.TYPE,
                        ActivityView.USER_USERNAME,
                        userAssociationProjection(LIKE, userUrn.numericId, SoundView.USER_LIKE),
                        userAssociationProjection(REPOST, userUrn.numericId, SoundView.USER_REPOST)
                };
                final Cursor cursor = database.query(table.name, projection, null, null, null, null, null);
                emitToSubscriber(subscriber, cursor);
            }
        }));
    }

    private void emitToSubscriber(Subscriber<? super PropertySet> subscriber, Cursor cursor) {
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    final PropertySet propertySet = PropertySet.create(cursor.getColumnCount());

                    propertySet.add(StreamItemProperty.SOUND_URN, readSoundUrn(cursor));
                    propertySet.add(StreamItemProperty.SOUND_TITLE, readSoundTitle(cursor));
                    propertySet.add(StreamItemProperty.CREATED_AT, readCreatedAt(cursor));
                    propertySet.add(StreamItemProperty.POSTER, readPoster(cursor));
                    propertySet.add(StreamItemProperty.REPOST, readRepostedFlag(cursor));

                    subscriber.onNext(propertySet);
                }
                subscriber.onCompleted();
            } finally {
                cursor.close();
            }
        }
    }

    private Date readCreatedAt(Cursor cursor) {
        return new Date(cursor.getLong(cursor.getColumnIndex(ActivityView.CREATED_AT)));
    }

    private String readPoster(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex(ActivityView.USER_USERNAME));
    }

    private boolean readRepostedFlag(Cursor cursor) {
        final String itemType = cursor.getString(cursor.getColumnIndex(ActivityView.TYPE));
        return itemType.endsWith("-repost");
    }

    private String readSoundTitle(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex(SoundView.TITLE));
    }

    private String readSoundUrn(Cursor cursor) {
        final int soundId = cursor.getInt(cursor.getColumnIndex(ActivityView.SOUND_ID));
        final int soundType = cursor.getInt(cursor.getColumnIndex(ActivityView.SOUND_TYPE));
        final Urn soundUrn = soundType == Playable.DB_TYPE_TRACK ? Urn.forTrack(soundId) : Urn.forPlaylist(soundId);
        return soundUrn.toString();
    }

    private static String userAssociationProjection(int collectionType, long userId, String colName) {
        StringBuilder sb = new StringBuilder();
        sb.append("EXISTS (SELECT 1 FROM ").append(Table.COLLECTION_ITEMS);
        sb.append(", ").append(Table.SOUNDS.name);
        sb.append(" WHERE ").append(ActivityView.SOUND_ID).append(" = ").append(CollectionItems.ITEM_ID);
        sb.append(" AND ").append(ActivityView.SOUND_TYPE).append(" = ").append(CollectionItems.RESOURCE_TYPE);
        sb.append(" AND ").append(CollectionItems.COLLECTION_TYPE).append(" = ").append(collectionType);
        sb.append(" AND ").append(Table.COLLECTION_ITEMS.name)
                .append(".").append(CollectionItems.USER_ID).append(" = ").append(userId);
        sb.append(") AS ").append(colName);
        return sb.toString();
    }

}
