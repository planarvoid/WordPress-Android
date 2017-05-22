package com.soundcloud.android.discovery;

import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JacksonJsonTransformer;
import com.soundcloud.java.reflect.TypeToken;
import com.squareup.sqldelight.ColumnAdapter;

import android.support.annotation.NonNull;

import java.util.List;

class StringListAdapter implements ColumnAdapter<List<String>, String> {

    private final JacksonJsonTransformer jacksonJsonTransformer;

    StringListAdapter(JacksonJsonTransformer jacksonJsonTransformer) {
        this.jacksonJsonTransformer = jacksonJsonTransformer;
    }

    @NonNull
    @Override
    public List<String> decode(String json) {
        try {
            return jacksonJsonTransformer.fromJson(json, new TypeToken<List<String>>(){});
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String encode(@NonNull List<String> strings) {
        try {
            return jacksonJsonTransformer.toJson(strings);
        } catch (ApiMapperException e) {
            throw new IllegalStateException(e);
        }
    }
}
