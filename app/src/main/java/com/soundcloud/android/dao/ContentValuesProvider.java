package com.soundcloud.android.dao;

import android.content.ContentValues;
import com.soundcloud.android.provider.BulkInsertMap;
import org.jetbrains.annotations.NotNull;

public interface ContentValuesProvider {
    ContentValues buildContentValues();
    void putFullContentValues(@NotNull BulkInsertMap destination);
}
