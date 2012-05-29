package com.soundcloud.android.provider;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.database.sqlite.SQLiteDatabase;

@RunWith(DefaultTestRunner.class)
public class DBHelperTest {

    @Test
    public void shouldGetWritableDatabase() throws Exception {
        SQLiteDatabase db = new DBHelper(DefaultTestRunner.application).getWritableDatabase();
        expect(db.isOpen()).toBeTrue();
        expect(db.isReadOnly()).toBeFalse();
    }

    @Test
    public void shouldDropAndCreatAllTableAndViews() throws Exception {
        SQLiteDatabase db = new DBHelper(DefaultTestRunner.application).getWritableDatabase();
        for (Table t : Table.values()) {
            // skip unused tables
            if (t.createString == null) continue;

            expect(t.exists(db)).toBeTrue();
            t.drop(db);
            expect(t.exists(db)).toBeFalse();
            t.create(db);
            expect(t.exists(db)).toBeTrue();
            t.recreate(db);
            expect(t.exists(db)).toBeTrue();
        }
    }
}
