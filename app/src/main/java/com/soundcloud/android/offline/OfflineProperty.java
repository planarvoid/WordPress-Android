package com.soundcloud.android.offline;

import com.soundcloud.propeller.Property;

import java.util.Date;

public final class OfflineProperty {
    public static final Property<DownloadState> DOWNLOAD_STATE = Property.of(Collection.class, DownloadState.class);

    public static final class Track {
        public static final Property<Date> DOWNLOADED_AT = Property.of(OfflineProperty.Track.class, Date.class);
        public static final Property<Date> REMOVED_AT = Property.of(OfflineProperty.Track.class, Date.class);
        public static final Property<Date> REQUESTED_AT = Property.of(OfflineProperty.Track.class, Date.class);
        public static final Property<Date> UNAVAILABLE_AT = Property.of(OfflineProperty.Track.class, Date.class);

    }

    public static final class Collection {
        public static final Property<Boolean> IS_MARKED_FOR_OFFLINE = Property.of(Collection.class, Boolean.class);
    }
}
