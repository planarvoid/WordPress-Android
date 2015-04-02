package com.soundcloud.android.offline;

import com.soundcloud.propeller.Property;

public final class OfflineProperty {
    public static final Property<DownloadState> DOWNLOAD_STATE = Property.of(OfflineProperty.class, DownloadState.class);

    public static final class Collection {
        public static final Property<Boolean> IS_MARKED_FOR_OFFLINE = Property.of(Collection.class, Boolean.class);
    }
}
