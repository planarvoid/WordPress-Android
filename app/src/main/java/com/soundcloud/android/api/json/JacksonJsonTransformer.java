package com.soundcloud.android.api.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.soundcloud.android.api.ApiDateFormat;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.java.reflect.TypeToken;

import javax.inject.Singleton;
import java.io.IOException;

@Singleton
public class JacksonJsonTransformer implements JsonTransformer {

    private final ObjectMapper objectMapper;
    private final TypeFactory typeFactory;

    public JacksonJsonTransformer() {
        objectMapper = buildObjectMapper();
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

    public static ObjectMapper buildObjectMapper() {
        return new ObjectMapper()
                .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setDateFormat(new ApiDateFormat());
    }
}
