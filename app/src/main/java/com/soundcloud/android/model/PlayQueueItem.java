package com.soundcloud.android.model;

import com.soundcloud.android.model.behavior.Persisted;
import com.soundcloud.android.storage.provider.BulkInsertMap;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.storage.provider.DBHelper;
import org.jetbrains.annotations.NotNull;

import android.database.Cursor;
import android.net.Uri;
import android.os.Parcelable;

public class PlayQueueItem extends ScModel implements Parcelable, Persisted {

    private long mTrackId;
    private String mSource;
    private String mSourceVersion;

    public PlayQueueItem(long trackId, String source, String sourceVersion) {
        this.mTrackId = trackId;
        this.mSource = source;
        this.mSourceVersion = sourceVersion;
    }

    public PlayQueueItem(Cursor cursor) {
        mTrackId = cursor.getLong(cursor.getColumnIndex(DBHelper.PlayQueue.TRACK_ID));
        mSource = cursor.getString(cursor.getColumnIndex(DBHelper.PlayQueue.SOURCE));
        mSourceVersion = cursor.getString(cursor.getColumnIndex(DBHelper.PlayQueue.SOURCE_VERSION));
    }

    public long getTrackId() {
        return mTrackId;
    }

    public String getSource() {
        return mSource;
    }

    public String getSourceVersion() {
        return mSourceVersion;
    }

    @Override
    public void putFullContentValues(@NotNull BulkInsertMap destination) {
        destination.add(getBulkInsertUri(), buildContentValues());
    }

    @Override
    public void putDependencyValues(@NotNull BulkInsertMap destination) {
    }

    @Override
    public Uri toUri() {
        return Content.PLAY_QUEUE.forId(getId());
    }

    @Override
    public Uri getBulkInsertUri() {
        return Content.PLAY_QUEUE.uri;
    }
}
