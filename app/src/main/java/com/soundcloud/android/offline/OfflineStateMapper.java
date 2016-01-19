package com.soundcloud.android.offline;

import com.soundcloud.android.storage.Tables.OfflineContent;
import com.soundcloud.android.storage.Tables.TrackDownloads;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;
import com.soundcloud.propeller.schema.Column;

import java.util.Date;

public class OfflineStateMapper extends RxResultMapper<PropertySet> {

    @Override
    public PropertySet map(CursorReader reader) {
        return addOptionalOfflineSyncDates(reader);
    }

    private PropertySet addOptionalOfflineSyncDates(CursorReader cursorReader) {
        final PropertySet propertySet = PropertySet.create(1);
        propertySet.put(OfflineProperty.OFFLINE_STATE, fromDates(cursorReader, cursorReader.isNotNull(OfflineContent._ID)));
        return propertySet;
    }

    public static OfflineState fromDates(CursorReader cursorReader, boolean unavailableEnabled){
        final Date defaultDate = new Date(0);
        final Date requestedAt = getDateOr(cursorReader, TrackDownloads.REQUESTED_AT, defaultDate);
        final Date removedAt = getDateOr(cursorReader, TrackDownloads.REMOVED_AT, defaultDate);
        final Date downloadedAt = getDateOr(cursorReader, TrackDownloads.DOWNLOADED_AT, defaultDate);
        final Date unavailableAt = getDateOr(cursorReader, TrackDownloads.UNAVAILABLE_AT, defaultDate);

        if (isMostRecentDate(requestedAt, removedAt, downloadedAt, unavailableAt)) {
            return OfflineState.REQUESTED;
        } else if (isMostRecentDate(downloadedAt, requestedAt, removedAt, unavailableAt)) {
            return OfflineState.DOWNLOADED;
        } else if (unavailableEnabled && isMostRecentDate(unavailableAt, requestedAt, removedAt, downloadedAt)) {
            return OfflineState.UNAVAILABLE;
        } else {
            return OfflineState.NOT_OFFLINE;
        }
    }

    private static Date getDateOr(CursorReader cursorReader, Column columnName, Date defaultDate) {
        if (cursorReader.isNotNull(columnName)) {
            return cursorReader.getDateFromTimestamp(columnName);
        }
        return defaultDate;
    }

    private static boolean isMostRecentDate(Date dateToTest, Date... dates) {
        for (Date aDate : dates) {
            if (aDate.after(dateToTest) || aDate.equals(dateToTest)) {
                return false;
            }
        }
        return true;
    }
}
