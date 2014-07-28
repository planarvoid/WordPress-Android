package com.soundcloud.android.api.json;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.soundcloud.android.tracks.TrackUrn;

public class SoundCloudModule extends SimpleModule {

    public SoundCloudModule() {
        addKeyDeserializer(TrackUrn.class, new TrackUrnKeyDeserializer());
    }
}
