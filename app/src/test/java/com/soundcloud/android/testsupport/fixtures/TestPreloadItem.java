package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackType;
import com.soundcloud.android.playback.PreloadItem;

public class TestPreloadItem {

    public static final Urn URN = TestPlayerTransitions.URN;

    public static PreloadItem audio() {
        return PreloadItem.create(URN, PlaybackType.AUDIO_DEFAULT);
    }

    public static PreloadItem snippet() {
        return PreloadItem.create(URN, PlaybackType.AUDIO_SNIPPET);
    }
}
