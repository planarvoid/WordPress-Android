package com.soundcloud.android.api.json;


import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.java.reflect.TypeToken;

import java.io.IOException;

public interface JsonTransformer {

    <T> T fromJson(String json, TypeToken<T> classToTransformTo) throws IOException, ApiMapperException;

    String toJson(Object source) throws ApiMapperException;
}
