package com.soundcloud.android.utils;

import com.soundcloud.android.model.Entity;
import com.soundcloud.android.model.Urn;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class EntityUtils {
    public static <T extends Entity> Map<Urn, T> toEntityMap(Collection<? extends T> entities) {
        Map<Urn,T> entityMap = new HashMap<>();
        for (T entity : entities) {
            entityMap.put(entity.getUrn(), entity);
        }
        return entityMap;
    }
}
