package com.soundcloud.android.analytics;

import static com.soundcloud.android.analytics.TrackingDbHelper.EVENTS_TABLE;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.ResultMapper;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import org.jetbrains.annotations.Nullable;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Storage facade for tracking events based on SQLite.
 */
class TrackingStorage {

    @VisibleForTesting
    static final int FIXED_BATCH_SIZE = 30;

    private final PropellerDatabase propeller;
    private final NetworkConnectionHelper networkConnectionHelper;

    @Inject
    TrackingStorage(@Named(AnalyticsModule.TRACKING_DB) PropellerDatabase propeller, NetworkConnectionHelper networkConnectionHelper) {
        this.propeller = propeller;
        this.networkConnectionHelper = networkConnectionHelper;
    }

    InsertResult insertEvent(TrackingRecord eventObject) throws UnsupportedEncodingException {
        return propeller.insert(EVENTS_TABLE, createValuesFromEvent(eventObject), SQLiteDatabase.CONFLICT_IGNORE);
    }

    List<TrackingRecord> getPendingEventsForBackend(String backend) {
        return getPendingEvents(backend);
    }

    List<TrackingRecord> getPendingEvents() {
        return getPendingEvents(null);
    }

    private List<TrackingRecord> getPendingEvents(@Nullable String backend) {
        final Query query = Query.from(EVENTS_TABLE.name());
        if (backend != null) {
            query.whereEq(TrackingDbHelper.TrackingColumns.BACKEND, backend);
        }
        if (!networkConnectionHelper.isWifiConnected()) {
            query.limit(FIXED_BATCH_SIZE);
        }

        return propeller.query(query).toList(new ResultMapper<TrackingRecord>() {
            @Override
            public TrackingRecord map(CursorReader reader) {
                return new TrackingRecord(
                        reader.getInt(TrackingDbHelper.TrackingColumns._ID),
                        reader.getLong(TrackingDbHelper.TrackingColumns.TIMESTAMP),
                        reader.getString(TrackingDbHelper.TrackingColumns.BACKEND),
                        reader.getString(TrackingDbHelper.TrackingColumns.DATA));
            }
        });
    }

    /**
     * Delete these events from the database.
     * Will perform delete in batches, and abort if one batch delete fails
     * @param submittedEvents The events to be deleted
     * @return the last {@link com.soundcloud.propeller.ChangeResult}
     */
    ChangeResult deleteEvents(List<TrackingRecord> submittedEvents) {
        final List<String> idList = Lists.transform(submittedEvents, new Function<TrackingRecord, String>() {
            @Override
            public String apply(TrackingRecord input) {
                return Long.toString(input.getId());
            }
        });

        int start = 0;
        ChangeResult changeResult;
        do {
            final int end = Math.min(start + FIXED_BATCH_SIZE, idList.size());
            final List<String> idBatch = idList.subList(start, end);
            final Where whereClause = filter().whereIn(TrackingDbHelper.TrackingColumns._ID, idBatch);
            changeResult = propeller.delete(EVENTS_TABLE, whereClause);

            start += FIXED_BATCH_SIZE;

        } while (start < idList.size() && changeResult.success());

        if (changeResult.success()) {
            return new ChangeResult(idList.size());
        } else {
            return (ChangeResult) new ChangeResult(start - FIXED_BATCH_SIZE).fail(changeResult.getFailure());
        }
    }


    private ContentValues createValuesFromEvent(TrackingRecord event) throws UnsupportedEncodingException {
        ContentValues values = new ContentValues();
        values.put(TrackingDbHelper.TrackingColumns.BACKEND, event.getBackend());
        values.put(TrackingDbHelper.TrackingColumns.TIMESTAMP, event.getTimeStamp());
        values.put(TrackingDbHelper.TrackingColumns.DATA, event.getData());
        return values;
    }
}
