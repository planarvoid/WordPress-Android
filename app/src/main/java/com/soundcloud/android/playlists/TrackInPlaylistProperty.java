package com.soundcloud.android.playlists;

import com.soundcloud.propeller.Property;

final class TrackInPlaylistProperty extends PlaylistProperty {
    public static final Property<Boolean> ADDED_TO_URN = Property.of(TrackInPlaylistProperty.class, Boolean.class);
}
