package com.soundcloud.android.waveform;

import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.reflect.TypeToken;
import com.vincentbrison.openlibraries.android.dualcache.CacheSerializer;

import javax.inject.Inject;
import java.io.IOException;

public class WaveformCacheSerializer implements CacheSerializer<WaveformData> {

    private final JsonTransformer jsonTransformer;

    @Inject
    public WaveformCacheSerializer(JsonTransformer jsonTransformer) {
        this.jsonTransformer = jsonTransformer;
    }

    @Override
    public WaveformData fromString(String s) {
        try {
            return jsonTransformer.fromJson(s, TypeToken.of(WaveformData.class));
        } catch (IOException | ApiMapperException e) {
            ErrorUtils.handleSilentException(e);
        }
        return null;
    }

    @Override
    public String toString(WaveformData waveformData) {
        try {
            return jsonTransformer.toJson(waveformData);
        } catch (ApiMapperException e) {
            ErrorUtils.handleSilentException(e);
        }
        return null;
    }

}
