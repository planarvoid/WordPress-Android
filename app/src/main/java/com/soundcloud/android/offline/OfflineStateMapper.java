package com.soundcloud.android.offline;

import com.soundcloud.android.storage.Tables.TrackDownloads;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.schema.Column;

import android.support.annotation.NonNull;

import java.util.Date;

public class OfflineStateMapper {

    public static OfflineState fromDates(CursorReader cursorReader, boolean unavailableEnabled) {
        final Date defaultDate = new Date(0);
        final Date requestedAt = getDateOr(cursorReader, TrackDownloads.REQUESTED_AT, defaultDate);
        final Date removedAt = getDateOr(cursorReader, TrackDownloads.REMOVED_AT, defaultDate);
        final Date downloadedAt = getDateOr(cursorReader, TrackDownloads.DOWNLOADED_AT, defaultDate);
        final Date unavailableAt = getDateOr(cursorReader, TrackDownloads.UNAVAILABLE_AT, defaultDate);

        return getOfflineState(unavailableEnabled, requestedAt, removedAt, downloadedAt, unavailableAt);
    }

    @NonNull
    public static OfflineState getOfflineState(boolean unavailableEnabled,
                                        Date requestedAt,
                                        Date removedAt,
                                        Date downloadedAt,
                                        Date unavailableAt) {
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
