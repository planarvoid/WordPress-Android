package com.soundcloud.android.playlists;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.Property;

import java.util.Date;

public final class PlaylistTrackProperty {
    public static final Property<Urn> TRACK_URN = PlayableProperty.URN;
    public static final Property<Date> ADDED_AT = Property.of(PlaylistTrackProperty.class, Date.class);
    public static final Property<Date> REMOVED_AT = Property.of(PlaylistTrackProperty.class, Date.class);
}
