package com.soundcloud.android.provider;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentValues;
import android.database.Cursor;
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
    public void shouldAlterColumnsWithoutRenamingColumn() throws Exception {
        SQLiteDatabase db = new DBHelper(DefaultTestRunner.application).getWritableDatabase();

        String oldSchema = "CREATE TABLE foo(_id INTEGER PRIMARY KEY," +
                " keep_me VARCHAR(255)," +
                " drop_me INTEGER);";

        String newSchema = "CREATE TABLE foo(_id INTEGER PRIMARY KEY," +
                " keep_me VARCHAR(255), " +
                " new_column INTEGER);";

        db.execSQL(oldSchema);

        ContentValues cv = new ContentValues();
        cv.put("keep_me", "blavalue");
        cv.put("drop_me", 100);

        expect(db.insert("foo", null, cv)).toEqual(1L);

        expect(Table.alterColumns(db, "foo", newSchema, new String[0], new String[0]))
                .toContainExactly("_id", "keep_me");

        expect(Table.getColumnNames(db, "foo")).toContainExactly("_id", "keep_me", "new_column");

        Cursor c = db.query("foo", null, null, null, null, null, null);

        // make sure old data is still around
        expect(c.getCount()).toEqual(1);
        expect(c.moveToNext()).toBeTrue();
        expect(c.getLong(c.getColumnIndex("_id"))).toEqual(1L);
        expect(c.getString(c.getColumnIndex("keep_me"))).toEqual("blavalue");
        expect(c.isNull(c.getColumnIndex("new_column"))).toBeTrue();
    }

    @Test
    public void shouldAlterColumnsWithRenamingColumn() throws Exception {
        SQLiteDatabase db = new DBHelper(DefaultTestRunner.application).getWritableDatabase();

        String oldSchema = "CREATE TABLE foo(_id INTEGER PRIMARY KEY," +
                " keep_me VARCHAR(255)," +
                " drop_me INTEGER," +
                " rename_me INTEGER); ";

        String newSchema = "CREATE TABLE foo(_id INTEGER PRIMARY KEY," +
                " keep_me VARCHAR(255)," +
                " new_column INTEGER," +
                " renamed INTEGER); ";

        db.execSQL(oldSchema);

        ContentValues cv = new ContentValues();
        cv.put("keep_me", "blavalue");
        cv.put("drop_me", 100);
        cv.put("rename_me", 200);

        expect(db.insert("foo", null, cv)).toEqual(1L);

        expect(Table.alterColumns(db, "foo", newSchema, new String[] { "rename_me"}, new String[] { "renamed" }))
                .toContainExactly("renamed", "_id", "keep_me");

        expect(Table.getColumnNames(db, "foo")).toContainExactly("_id", "keep_me", "new_column", "renamed");

        Cursor c = db.query("foo", null, null, null, null, null, null);

        // make sure old data is still around
        expect(c.getCount()).toEqual(1);
        expect(c.moveToNext()).toBeTrue();
        expect(c.getLong(c.getColumnIndex("_id"))).toEqual(1L);
        expect(c.getString(c.getColumnIndex("keep_me"))).toEqual("blavalue");
        expect(c.getInt(c.getColumnIndex("renamed"))).toEqual(200);
    }
}
