package com.soundcloud.android.model.behavior;

import com.soundcloud.android.provider.BulkInsertMap;
import org.jetbrains.annotations.NotNull;

import android.content.ContentValues;
import android.net.Uri;

public interface Persisted {
    ContentValues buildContentValues();
    void putFullContentValues(@NotNull BulkInsertMap destination);
    void putDependencyValues(@NotNull BulkInsertMap destination);
    Uri toUri();
    Uri getBulkInsertUri();
}
