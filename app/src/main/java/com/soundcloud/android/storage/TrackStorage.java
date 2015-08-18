package com.soundcloud.android.storage;

import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.storage.provider.Content;
import rx.Observable;
import rx.Subscriber;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Deprecated
public class TrackStorage {
    private ContentResolver resolver;

    @Inject
    public TrackStorage(ContentResolver contentResolver){
        this.resolver = contentResolver;
    }

    // TODO: this should not depend on content URIs, since we're trying to move away from it. Difficult to do without
    // migrating the front end first to not use content URIs
    @SuppressWarnings("PMD.NPathComplexity")
    public Observable<List<Urn>> getTracksForUriAsync(final Uri uri) {
        return Observable.create(new Observable.OnSubscribe<List<Urn>>() {
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

                List<Urn> newQueue = new ArrayList<>(cursor.getCount());
                while (cursor.moveToNext()) {
                    newQueue.add(Urn.forTrack(cursor.getLong(cursor.getColumnIndex(idColumn))));
                }
                return newQueue;
            }
        }).subscribeOn(ScSchedulers.HIGH_PRIO_SCHEDULER);
    }

}


