package com.soundcloud.android.offline;

import com.soundcloud.java.collections.Property;

public final class OfflineProperty {
    public static final Property<OfflineState> OFFLINE_STATE = Property.of(OfflineProperty.class, OfflineState.class);
    public static final Property<Boolean> IS_MARKED_FOR_OFFLINE = Property.of(OfflineProperty.class, Boolean.class);
}