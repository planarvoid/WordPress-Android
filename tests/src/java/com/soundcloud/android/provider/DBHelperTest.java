package com.soundcloud.android.provider;

import static com.soundcloud.android.Expect.expect;

import android.database.sqlite.SQLiteDatabase;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class DBHelperTest {

    @Test
    public void shouldGetWritableDatabase() throws Exception {
        SQLiteDatabase db = new DBHelper(DefaultTestRunner.application).getWritableDatabase();
        expect(db.isOpen()).toBeTrue();
        expect(db.isReadOnly()).toBeFalse();
    }
}
