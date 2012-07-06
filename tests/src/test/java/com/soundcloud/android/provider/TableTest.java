package com.soundcloud.android.provider;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class TableTest {

    @Test
    public void shouldProvideACreateStringForViews() throws Exception {
        Table view = Table.TRACK_VIEW;
        expect(view.view).toBe(true);
        expect(view.createString).toMatch("CREATE VIEW IF NOT EXISTS "+view.name);
    }

    @Test
    public void shouldProvideACreateStringForTables() throws Exception {
        Table table = Table.TRACKS;
        expect(table.view).toBe(false);
        expect(table.createString).toMatch("CREATE TABLE IF NOT EXISTS " + table.name);
    }

    @Test
    public void shouldHaveFieldMethod() throws Exception {
        Table table = Table.TRACKS;
        expect(table.field("foo")).toEqual("Tracks.foo");
    }

    @Test
    public void shouldHaveAllFieldMethod() throws Exception {
        Table table = Table.TRACKS;
        expect(table.allFields()).toEqual("Tracks.*");
    }

    @Test
    public void shouldGetTableByName() throws Exception {
        Table t = Table.get("Tracks");
        expect(t).not.toBeNull();
        expect(t.name).toEqual("Tracks");
        expect(Table.get("zombo")).toBeNull();
    }

    @Test
    public void shouldGetColumnNames() throws Exception {
        SQLiteDatabase db = new DBHelper(DefaultTestRunner.application).getWritableDatabase();
        List<String> columns = Table.TRACKS.getColumnNames(db);
        expect(columns.isEmpty()).toBeFalse();
    }

    @Test
    public void shouldDropAndCreateTable() throws Exception {
        SQLiteDatabase db = new DBHelper(DefaultTestRunner.application).getWritableDatabase();
        Table table = Table.TRACKS;

        expect(table.exists(db)).toBeTrue();
        table.drop(db);
        expect(table.exists(db)).toBeFalse();
        table.create(db);
        expect(table.exists(db)).toBeTrue();
        table.recreate(db);
        expect(table.exists(db)).toBeTrue();
    }

    @Test
    public void shouldAddColumnToTracks() throws Exception {
        SQLiteDatabase db = new DBHelper(DefaultTestRunner.application).getWritableDatabase();

        String oldSchema = Table.TRACKS.createString;
        db.execSQL(oldSchema);

        String newSchema = Table.TRACKS.createString.substring(0, Table.TRACKS.createString.lastIndexOf(")")) + ", new_column INTEGER);";
        final int colCount = Table.alterColumns(db, Table.TRACKS.name, newSchema, new String[0], new String[0]).size();
        final List<String> columnNames = Table.getColumnNames(db, Table.TRACKS.name);
        expect(columnNames).toContain("new_column");
        expect(columnNames.size()).toEqual(colCount + 1);
    }

    @Test
    public void shouldAlterColumnsWithoutRenamingColumn() throws Exception {
        SQLiteDatabase db = new DBHelper(DefaultTestRunner.application).getWritableDatabase();

        String oldSchema = "CREATE TABLE foo("+
                "_id INTEGER PRIMARY KEY," +
                "keep_me VARCHAR(255)," +
                "drop_me INTEGER);";

        String newSchema = "CREATE TABLE foo("+
                "_id INTEGER PRIMARY KEY," +
                "keep_me VARCHAR(255), " +
                "new_column INTEGER);";

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

    @Test
    public void shouldSnapshotSchema() throws Exception {
        String snapshot = Table.schemaSnapshot();
        expect(snapshot).toMatch("CREATE TABLE");
        expect(snapshot).toMatch("CREATE VIEW");
    }

    @Test
    public void shouldInsertAndUpsertEntries() {
        SQLiteDatabase db = new DBHelper(DefaultTestRunner.application).getWritableDatabase();

        long id = Table.RECORDINGS.insertOrReplaceArgs(db,
            DBHelper.Recordings.WHAT_TEXT,  "what",
            DBHelper.Recordings.WHERE_TEXT, "where"
        );
        expect(id).not.toBe(0l);

        int changed = Table.RECORDINGS.upsertSingleArgs(db,
            DBHelper.Recordings._ID, id,
            DBHelper.Recordings.WHAT_TEXT, "was"
        );
        expect(changed).toEqual(1);

        Cursor c = DefaultTestRunner.application.getContentResolver().
                query(Content.RECORDINGS.forId(id),
                        null, null, null, null);

        expect(c.moveToFirst()).toBeTrue();
        String what = c.getString(c.getColumnIndex(DBHelper.Recordings.WHAT_TEXT));
        String where = c.getString(c.getColumnIndex(DBHelper.Recordings.WHERE_TEXT));

        expect(what).toEqual("was");
        expect(where).toEqual("where");
    }

    public static void main(String[] args) throws IOException {
        File schema = new File("tests/src/resources/com/soundcloud/android/provider/schema_"
                +DBHelper.DATABASE_VERSION+".sql");

        FileOutputStream fos = new FileOutputStream(schema);
        fos.write(Table.schemaSnapshot().getBytes());
        fos.close();
    }
}
