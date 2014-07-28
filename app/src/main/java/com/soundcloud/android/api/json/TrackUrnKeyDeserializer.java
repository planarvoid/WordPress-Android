package com.soundcloud.android.api.json;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.soundcloud.android.tracks.TrackUrn;

import java.io.IOException;

public class TrackUrnKeyDeserializer extends KeyDeserializer {

    @Override
    public Object deserializeKey(final String key, final DeserializationContext ctxt) throws IOException {
        if (key.length() == 0) { // [JACKSON-360] : taken from the Joda Time Module demo.
            return null;
        }
        return TrackUrn.parse(key);
    }

}
