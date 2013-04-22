package com.soundcloud.android.dao;

import com.soundcloud.android.provider.BulkInsertMap;
import org.jetbrains.annotations.NotNull;

import android.content.ContentValues;

public interface ContentValuesProvider {
    ContentValues buildContentValues();
    void putFullContentValues(@NotNull BulkInsertMap destination);
    void putDependencyValues(@NotNull BulkInsertMap destination);

}
