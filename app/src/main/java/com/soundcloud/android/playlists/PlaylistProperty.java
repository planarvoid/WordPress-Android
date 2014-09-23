package com.soundcloud.android.playlists;

import com.soundcloud.propeller.Property;

public final class PlaylistProperty {
    public static final Property<PlaylistUrn> URN = Property.of(PlaylistProperty.class, PlaylistUrn.class);
    public static final Property<Integer> TRACK_COUNT = Property.of(PlaylistProperty.class, Integer.class);
}
