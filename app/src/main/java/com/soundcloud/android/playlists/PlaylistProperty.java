package com.soundcloud.android.playlists;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.propeller.Property;

import java.util.List;

public class PlaylistProperty extends PlayableProperty {
    public static final Property<Integer> TRACK_COUNT = Property.of(PlaylistProperty.class, Integer.class);
    public static final Property<List<String>> TAGS = Property.ofList(PlaylistProperty.class, String.class);//TODO: make Optional<List<String>>
    public static final Property<Boolean> IS_MARKED_FOR_OFFLINE = Property.of(PlaylistProperty.class, Boolean.class);
}
