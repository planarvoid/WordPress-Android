package com.soundcloud.android.robolectric;

import com.xtremelabs.robolectric.util.SQLiteMap;

public class FileMap extends SQLiteMap {

    @Override
    public String getConnectionString() {
        return "jdbc:sqlite:file.db";
    }
}
