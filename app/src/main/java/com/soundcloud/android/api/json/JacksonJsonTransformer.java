package com.soundcloud.android.api.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.legacy.PublicApi;

import javax.inject.Singleton;
import java.io.IOException;

@Singleton
public class JacksonJsonTransformer implements JsonTransformer {

    private final ObjectMapper objectMapper;
    private final TypeFactory typeFactory;

    public JacksonJsonTransformer() {
        objectMapper = PublicApi.buildObjectMapper();
        typeFactory = objectMapper.getTypeFactory();
    }

    @Override
    public <T> T fromJson(String json, TypeToken<T> classToTransformTo) throws IOException, ApiMapperException {
        try {
            return objectMapper.readValue(json, typeFactory.constructType(classToTransformTo.getType()));
        } catch (JsonProcessingException e) {
            throw new ApiMapperException(e);
        }
    }

    @Override
    public String toJson(Object source) throws ApiMapperException {
        try {
            return objectMapper.writeValueAsString(source);
        } catch (JsonProcessingException e) {
            throw new ApiMapperException(e);
        }
    }
}
