package com.soundcloud.android.offline;

import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.rx.RxResultMapper;

import java.util.Date;

public class DownloadStateMapper extends RxResultMapper<PropertySet> {
    @Override
    public PropertySet  map(CursorReader reader) {
        return addOptionalOfflineSyncDates(reader);
    }

    private PropertySet addOptionalOfflineSyncDates(CursorReader cursorReader) {
        final Date defaultDate = new Date(0);
        final Date requestedAt = getDateOr(cursorReader, TableColumns.TrackDownloads.REQUESTED_AT, defaultDate);
        final Date removedAt = getDateOr(cursorReader, TableColumns.TrackDownloads.REMOVED_AT, defaultDate);
        final Date downloadedAt = getDateOr(cursorReader, TableColumns.TrackDownloads.DOWNLOADED_AT, defaultDate);
        final Date unavailableAt = getDateOr(cursorReader, TableColumns.TrackDownloads.UNAVAILABLE_AT, defaultDate);

        final PropertySet propertySet = PropertySet.create(1);
        if (isMostRecentDate(requestedAt, removedAt, downloadedAt, unavailableAt)) {
            propertySet.put(OfflineProperty.OFFLINE_STATE, OfflineState.REQUESTED);
        } else if (isMostRecentDate(removedAt, requestedAt, downloadedAt, unavailableAt)) {
            propertySet.put(OfflineProperty.OFFLINE_STATE, OfflineState.NO_OFFLINE);
        } else if (isMostRecentDate(downloadedAt, requestedAt, removedAt, unavailableAt)) {
            propertySet.put(OfflineProperty.OFFLINE_STATE, OfflineState.DOWNLOADED);
        } else if (isMostRecentDate(unavailableAt, requestedAt, removedAt, downloadedAt)) {
            propertySet.put(OfflineProperty.OFFLINE_STATE, OfflineState.UNAVAILABLE);
        }
        return propertySet;
    }

    private Date getDateOr(CursorReader cursorReader, String columnName, Date defaultDate) {
        if (cursorReader.isNotNull(columnName)) {
            return cursorReader.getDateFromTimestamp(columnName);
        }
        return defaultDate;
    }

    private boolean isMostRecentDate(Date dateToTest, Date... dates) {
        for (Date aDate : dates) {
            if (aDate.after(dateToTest) || aDate.equals(dateToTest)) {
                return false;
            }
        }
        return true;
    }
}
