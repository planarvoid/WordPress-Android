package com.soundcloud.android.offline;

import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.storage.Tables.TrackDownloads;
import com.soundcloud.propeller.query.Where;

public class OfflineFilters {

    public static final Where REQUESTED_DOWNLOAD_FILTER =
            filter().whereNull(TrackDownloads.REMOVED_AT)
                    .whereNull(TrackDownloads.DOWNLOADED_AT)
                    .whereNull(TrackDownloads.UNAVAILABLE_AT)
                    .whereNotNull(TrackDownloads.REQUESTED_AT);

    public static final Where CREATOR_OPT_OUT_FILTER =
            filter().whereNotNull(TrackDownloads.UNAVAILABLE_AT)
                    .whereNull(TrackDownloads.DOWNLOADED_AT)
                    .orWhereGe(TrackDownloads.UNAVAILABLE_AT, TrackDownloads.DOWNLOADED_AT)
                    .whereNull(TrackDownloads.REMOVED_AT)
                    .orWhereGe(TrackDownloads.UNAVAILABLE_AT, TrackDownloads.REMOVED_AT);

    public static final Where OFFLINE_TRACK_FILTER =
            filter().whereNotNull(TrackDownloads.DOWNLOADED_AT)
                    .whereNull(TrackDownloads.REMOVED_AT)
                    .whereNull(TrackDownloads.UNAVAILABLE_AT);
}
