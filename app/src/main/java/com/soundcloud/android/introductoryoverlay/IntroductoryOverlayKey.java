package com.soundcloud.android.introductoryoverlay;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IntroductoryOverlayKey {
    public static final String PLAY_QUEUE = "play_queue";
    public static final String CHROMECAST = "chromecast";
    public static final String EDIT_PLAYLIST = "edit_playlist";
    public static final String ADD_TO_COLLECTION = "add_to_collection";

    public static final List<String> ALL_KEYS = Collections.unmodifiableList(Arrays.asList(PLAY_QUEUE,
                                                                                           CHROMECAST,
                                                                                           EDIT_PLAYLIST,
                                                                                           ADD_TO_COLLECTION));
}
