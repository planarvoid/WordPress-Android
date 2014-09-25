package com.soundcloud.android.playlists;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.propeller.Property;

public final class PlaylistProperty extends PlayableProperty {
    public static final Property<Integer> TRACK_COUNT = Property.of(PlaylistProperty.class, Integer.class);
}
