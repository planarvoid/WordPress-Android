package com.soundcloud.android.api.http.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.http.PublicApiWrapper;

import javax.inject.Singleton;
import java.io.IOException;

@Singleton
public class JacksonJsonTransformer implements JsonTransformer {

    private final ObjectMapper objectMapper;
    private final TypeFactory typeFactory;

    public JacksonJsonTransformer() {
        objectMapper = PublicApiWrapper.buildObjectMapper();
        typeFactory = objectMapper.getTypeFactory();
    }

    @Override
    public <T> T fromJson(String json, TypeToken<?> classToTransformTo) throws Exception {
        return objectMapper.readValue(json, typeFactory.constructType(classToTransformTo.getType()));

    }

    @Override
    public String toJson(Object source) throws IOException {
        return objectMapper.writeValueAsString(source);
    }
}
