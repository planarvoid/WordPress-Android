package com.soundcloud.android.offline;

import com.soundcloud.propeller.Property;

public final class OfflineProperty {
    public static final Property<OfflineState> OFFLINE_STATE = Property.of(OfflineProperty.class, OfflineState.class);

    public static final class Collection {
        public static final Property<Boolean> OFFLINE_LIKES = Property.of(Collection.class, Boolean.class);
        public static final Property<Boolean> IS_MARKED_FOR_OFFLINE = Property.of(Collection.class, Boolean.class);
    }
}
