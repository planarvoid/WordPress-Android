package com.soundcloud.android.offline;

import com.soundcloud.propeller.Property;

import java.util.Date;

public final class OfflineProperty {
    // This is actually a transient state and may not be a property. We may consider removing it
    // when introducing presenter models.
    public static final Property<Boolean> DOWNLOADING = Property.of(OfflineProperty.class, Boolean.class);

    public static final Property<Date> DOWNLOADED_AT = Property.of(OfflineProperty.class, Date.class);
    public static final Property<Date> REMOVED_AT = Property.of(OfflineProperty.class, Date.class);
    public static final Property<Date> REQUESTED_AT = Property.of(OfflineProperty.class, Date.class);
    public static final Property<Date> UNAVAILABLE_AT = Property.of(OfflineProperty.class, Date.class);
}
