package com.soundcloud.android.model;

import com.google.common.base.Objects;
import com.soundcloud.android.model.behavior.Persisted;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.BulkInsertMap;
import com.soundcloud.android.storage.provider.Content;
import org.jetbrains.annotations.NotNull;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class PlayQueueItem extends ScModel implements Persisted {

    private long mTrackId;
    private String mSource;
    private String mSourceVersion;

    public PlayQueueItem(long trackId, String source, String sourceVersion) {
        this.mTrackId = trackId;
        this.mSource = source;
        this.mSourceVersion = sourceVersion;
    }

    @SuppressWarnings("UnusedDeclaration")
    public PlayQueueItem(Cursor cursor) {
        setId(cursor.getLong(cursor.getColumnIndex(TableColumns.PlayQueue._ID)));
        mTrackId = cursor.getLong(cursor.getColumnIndex(TableColumns.PlayQueue.TRACK_ID));
        mSource = cursor.getString(cursor.getColumnIndex(TableColumns.PlayQueue.SOURCE));
        mSourceVersion = cursor.getString(cursor.getColumnIndex(TableColumns.PlayQueue.SOURCE_VERSION));
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
    public ContentValues buildContentValues() {
        ContentValues cv = super.buildContentValues();
        cv.put(TableColumns.PlayQueue.TRACK_ID, mTrackId);
        cv.put(TableColumns.PlayQueue.SOURCE, mSource);
        cv.put(TableColumns.PlayQueue.SOURCE_VERSION, mSourceVersion);
        return cv;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PlayQueueItem that = (PlayQueueItem) o;
        if (mTrackId != that.mTrackId) return false;
        return Objects.equal(mSource, that.mSource) && Objects.equal(mSourceVersion, that.mSourceVersion);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (mTrackId ^ (mTrackId >>> 32));
        result = 31 * result + (mSource != null ? mSource.hashCode() : 0);
        result = 31 * result + (mSourceVersion != null ? mSourceVersion.hashCode() : 0);
        return result;
    }
}
