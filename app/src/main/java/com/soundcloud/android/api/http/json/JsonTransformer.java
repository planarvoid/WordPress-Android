package com.soundcloud.android.api.http.json;


import com.google.common.reflect.TypeToken;

public interface JsonTransformer {

    public <T> T fromJson(String json, TypeToken<?> classToTransformTo) throws Exception;
}
