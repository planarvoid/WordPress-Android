package com.soundcloud.android.api.http.json;


import com.google.common.reflect.TypeToken;

import java.io.IOException;

public interface JsonTransformer {

    public <T> T fromJson(String json, TypeToken<?> classToTransformTo) throws Exception;

    public String toJson(Object source) throws IOException;
}
