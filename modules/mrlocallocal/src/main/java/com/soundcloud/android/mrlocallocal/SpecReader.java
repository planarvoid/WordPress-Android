package com.soundcloud.android.mrlocallocal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.soundcloud.android.mrlocallocal.data.Spec;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

class SpecReader {

    private final Context context;
    private final ObjectMapper yamlMapper;

    SpecReader(Context context) {
        this.context = context;
        yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    Spec readSpec(String specName) throws IOException {
        InputStream inputStream = context.getResources().getAssets().open(specName);
        return yamlMapper.readValue(inputStream, Spec.class);
    }
}
