package com.soundcloud.android.provider;

import android.database.sqlite.SQLiteDatabase;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(DefaultTestRunner.class)
public class DBHelperTest {

    @Test
    public void shouldCreateDatabase() throws Exception {
        SQLiteDatabase db = SQLiteDatabase.openDatabase("path", null, 0);
        new DBHelper(DefaultTestRunner.application).onCreate(db);
    }
}
