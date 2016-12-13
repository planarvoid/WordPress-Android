package com.soundcloud.android.playlists;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.java.collections.Property;
import com.soundcloud.java.optional.Optional;

import java.util.List;

public class PlaylistProperty extends PlayableProperty {
    public static final Property<Long> PLAYLIST_DURATION = Property.of(PlaylistProperty.class, Long.class);
    public static final Property<Integer> TRACK_COUNT = Property.of(PlaylistProperty.class, Integer.class);
    public static final Property<Optional<List<String>>> TAGS = Property.ofOptionalList(PlaylistProperty.class,
                                                                                        String.class);
    public static final Property<Boolean> IS_ALBUM = Property.of(PlaylistProperty.class, Boolean.class);
    public static final Property<String> SET_TYPE = Property.of(PlaylistProperty.class, String.class);
    public static final Property<String> RELEASE_DATE = Property.of(PlaylistProperty.class, String.class);
}
