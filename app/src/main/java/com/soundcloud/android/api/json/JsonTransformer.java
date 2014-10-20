package com.soundcloud.android.api.json;


import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiMapperException;

public interface JsonTransformer {

    <T> T fromJson(String json, TypeToken<?> classToTransformTo) throws ApiMapperException;

    String toJson(Object source) throws ApiMapperException;
}
