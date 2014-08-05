package com.soundcloud.android.analytics;

import static com.soundcloud.android.analytics.TrackingDbHelper.EVENTS_TABLE;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.ResultMapper;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.WhereBuilder;
import org.jetbrains.annotations.Nullable;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Storage facade for tracking events based on SQLite.
 */
class TrackingStorage {

    static final int FIXED_BATCH_SIZE = 30;

    private final PropellerDatabase propeller;
    private final NetworkConnectionHelper networkConnectionHelper;

    @Inject
    TrackingStorage(@Named("tracking_db") PropellerDatabase propeller, NetworkConnectionHelper networkConnectionHelper) {
        this.propeller = propeller;
        this.networkConnectionHelper = networkConnectionHelper;
    }

    InsertResult insertEvent(TrackingEvent eventObject) throws UnsupportedEncodingException {
        return propeller.insert(EVENTS_TABLE, createValuesFromEvent(eventObject), SQLiteDatabase.CONFLICT_IGNORE);
    }

    List<TrackingEvent> getPendingEventsForBackend(String backend) {
        return getPendingEvents(backend);
    }

    List<TrackingEvent> getPendingEvents() {
        return getPendingEvents(null);
    }

    private List<TrackingEvent> getPendingEvents(@Nullable String backend) {
        final Query query = Query.from(EVENTS_TABLE);
        if (backend != null) {
            query.whereEq(TrackingDbHelper.TrackingColumns.BACKEND, backend);
        }
        if (!networkConnectionHelper.isWifiConnected()) {
            query.limit(FIXED_BATCH_SIZE);
        }

        return propeller.query(query).toList(new ResultMapper<TrackingEvent>() {
            @Override
            public TrackingEvent map(CursorReader reader) {
                return new TrackingEvent(
                        reader.getInt(TrackingDbHelper.TrackingColumns._ID),
                        reader.getLong(TrackingDbHelper.TrackingColumns.TIMESTAMP),
                        reader.getString(TrackingDbHelper.TrackingColumns.BACKEND),
                        reader.getString(TrackingDbHelper.TrackingColumns.URL));
            }
        });
    }

    ChangeResult deleteEvents(List<TrackingEvent> submittedEvents) {
        final List<String> idList = Lists.transform(submittedEvents, new Function<TrackingEvent, String>() {
            @Override
            public String apply(TrackingEvent input) {
                return Long.toString(input.getId());
            }
        });

        String[] ids = idList.toArray(new String[idList.size()]);

        return propeller.delete(EVENTS_TABLE, new WhereBuilder().whereIn(TrackingDbHelper.TrackingColumns._ID, ids));
    }


    private ContentValues createValuesFromEvent(TrackingEvent event) throws UnsupportedEncodingException {
        ContentValues values = new ContentValues();
        values.put(TrackingDbHelper.TrackingColumns.BACKEND, event.getBackend());
        values.put(TrackingDbHelper.TrackingColumns.TIMESTAMP, event.getTimeStamp());
        values.put(TrackingDbHelper.TrackingColumns.URL, event.getUrl());
        return values;
    }
}
