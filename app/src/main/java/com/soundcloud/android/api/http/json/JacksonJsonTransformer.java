package com.soundcloud.android.api.http.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.http.Wrapper;

public class JacksonJsonTransformer implements JsonTransformer {

    private final ObjectMapper mObjectMapper;
    private final TypeFactory mTypeFactory;

    public JacksonJsonTransformer(){
        mObjectMapper = Wrapper.buildObjectMapper();
        mTypeFactory = mObjectMapper.getTypeFactory();
    }

    @Override
    public <T> T fromJson(String json, TypeToken<?> classToTransformTo) throws Exception {
        return mObjectMapper.readValue(json, mTypeFactory.constructType(classToTransformTo.getType()));

    }
}
