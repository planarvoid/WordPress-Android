package com.soundcloud.android.storage;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.storage.provider.ScContentProvider;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowContentResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class TableTest {

    @Before
    public void setup() {
        ContentProvider provider = new ScContentProvider(new DatabaseManager(Robolectric.application));
        provider.onCreate();
        ShadowContentResolver.registerProvider(ScContentProvider.AUTHORITY, provider);
    }

    @Test
    public void shouldProvideACreateStringForViews() throws Exception {
        Table view = Table.SoundView;
        expect(view.view).toBe(true);
        expect(view.createString).toMatch("CREATE VIEW IF NOT EXISTS " + view);
    }

    @Test
    public void shouldProvideACreateStringForTables() throws Exception {
        Table table = Table.Sounds;
        expect(table.view).toBe(false);
        expect(table.createString).toMatch("CREATE TABLE IF NOT EXISTS " + table);
    }

    @Test
    public void shouldHaveFieldMethod() throws Exception {
        Table table = Table.Sounds;
        expect(table.field("foo")).toEqual("Sounds.foo");
    }

    @Test
    public void shouldHaveAllFieldMethod() throws Exception {
        Table table = Table.Sounds;
        expect(table.allFields()).toEqual("Sounds.*");
    }

    @Test
    public void shouldGetTableByName() throws Exception {
        Table t = Table.get("Sounds");
        expect(t).not.toBeNull();
        expect(t.name()).toEqual("Sounds");
        expect(Table.get("zombo")).toBeNull();
    }

    @Test
    public void shouldGetColumnNames() throws Exception {
        SQLiteDatabase db = new DatabaseManager(Robolectric.application).getWritableDatabase();
        List<String> columns = Table.Sounds.getColumnNames(db);
        expect(columns.isEmpty()).toBeFalse();
    }

    @Test
    public void shouldDropAndCreateTable() throws Exception {
        SQLiteDatabase db = new DatabaseManager(Robolectric.application).getWritableDatabase();
        Table table = Table.Sounds;

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
        SQLiteDatabase db = new DatabaseManager(Robolectric.application).getWritableDatabase();

        String oldSchema = Table.Sounds.createString;
        db.execSQL(oldSchema);

        final int insertIndex = Table.Sounds.createString.lastIndexOf("PRIMARY");
        String newSchema = Table.Sounds.createString.substring(0, insertIndex) +
               " new_column INTEGER," + Table.Sounds.createString.substring(insertIndex);

        final int colCount = Table.alterColumns(db, Table.Sounds.name(), newSchema, new String[0], new String[0]).size();
        final List<String> columnNames = Table.getColumnNames(db, Table.Sounds.name());
        expect(columnNames).toContain("new_column");
        expect(columnNames.size()).toEqual(colCount + 1);
    }

    @Test
    public void shouldAlterColumnsWithoutRenamingColumn() throws Exception {
        SQLiteDatabase db = new DatabaseManager(Robolectric.application).getWritableDatabase();

        String oldSchema = Table.buildCreateString("foo", "(" +
                "_id INTEGER PRIMARY KEY," +
                "keep_me VARCHAR(255)," +
                "drop_me INTEGER);", false);

        String newSchema = Table.buildCreateString("foo", "(" +
                "_id INTEGER PRIMARY KEY," +
                "keep_me VARCHAR(255), " +
                "new_column INTEGER);", false);

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
        SQLiteDatabase db = new DatabaseManager(Robolectric.application).getWritableDatabase();

        String oldSchema =Table.buildCreateString("foo","(_id INTEGER PRIMARY KEY," +
                        " keep_me VARCHAR(255)," +
                        " drop_me INTEGER," +
                        " rename_me INTEGER);",false);

        String newSchema =Table.buildCreateString("foo","(_id INTEGER PRIMARY KEY," +
                        " keep_me VARCHAR(255)," +
                        " new_column INTEGER," +
                        " renamed INTEGER); ",false);

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
    public void upsertShouldInsertContentValues() throws Exception {
        SQLiteDatabase db = new DatabaseManager(Robolectric.application).getWritableDatabase();

        ContentValues[] values = new ContentValues[3];

        ContentValues contentValues = new ContentValues();
        contentValues.put(TableColumns.Recordings._ID, 1l);
        contentValues.put(TableColumns.Recordings.DURATION, 32);

        values[1] = contentValues; //skip 0 to check null handling

        contentValues = new ContentValues();
        contentValues.put(TableColumns.Recordings._ID, 2l);
        contentValues.put(TableColumns.Recordings.DURATION, 868726);

        values[2] = contentValues;

        expect(Table.Recordings.upsert(db,values)).toBe(2);
        Cursor cursor = db.rawQuery("select _id from " + Table.Recordings, null);
        expect(cursor.moveToNext()).toBeTrue();
        expect(cursor).toHaveColumn(BaseColumns._ID, 1);
        expect(cursor.moveToNext()).toBeTrue();
        expect(cursor).toHaveColumn(BaseColumns._ID, 2);
    }

    @Test
    public void upsertShouldUpdateContentValues() throws Exception {
        SQLiteDatabase db = new DatabaseManager(Robolectric.application).getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(TableColumns.Recordings._ID, 1l);
        contentValues.put(TableColumns.Recordings.DURATION, 32);

        expect(Table.Recordings.upsert(db, new ContentValues[]{contentValues})).toBe(1);

        contentValues.put(TableColumns.Recordings.DURATION, 4000);
        expect(Table.Recordings.upsert(db, new ContentValues[]{contentValues})).toBe(1);
        Cursor cursor = db.rawQuery("select duration from " + Table.Recordings, null);
        expect(cursor.moveToNext()).toBeTrue();
        expect(cursor).toHaveColumn(TableColumns.Recordings.DURATION, 4000);
    }

    @Test
    public void upsertShouldRespectCompositePrimaryKeys() throws Exception {
        SQLiteDatabase db = new DatabaseManager(Robolectric.application).getWritableDatabase();

        ContentValues trackValues = new ContentValues();
        trackValues.put(TableColumns.Sounds._ID, 1l);
        trackValues.put(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_TRACK);
        ContentValues playlistValues = new ContentValues();
        playlistValues.put(TableColumns.Sounds._ID, 1l);
        playlistValues.put(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_PLAYLIST);

        expect(Table.Sounds.upsert(db, new ContentValues[]{trackValues})).toBe(1);
        expect(Table.Sounds.upsert(db, new ContentValues[]{playlistValues})).toBe(1);

        Cursor cursor = db.query(Table.Sounds.name(), null, null, null, null, null, null);
        expect(cursor).toHaveCount(2);
    }

//    @Test
//    public void shouldInsertAndUpsertEntries() {
//        SQLiteDatabase db = new DatabaseManager(Robolectric.application).getWritableDatabase();
//
//        long id = Table.Recordings.insertOrReplaceArgs(db,
//            TableColumns.Recordings.WHAT_TEXT,  "what",
//            TableColumns.Recordings.WHERE_TEXT, "where"
//        );
//        expect(id).not.toBe(0l);
//
//        Table.Recordings.upsertSingleArgs(db,
//            TableColumns.Recordings._ID, id,
//            TableColumns.Recordings.WHAT_TEXT, "was"
//        );
//
//        Cursor c = Robolectric.application.getContentResolver().
//                query(Content.RECORDINGS.forId(id),
//                        null, null, null, null);
//
//        expect(c.moveToFirst()).toBeTrue();
//        String what = c.getString(c.getColumnIndex(TableColumns.Recordings.WHAT_TEXT));
//        String where = c.getString(c.getColumnIndex(TableColumns.Recordings.WHERE_TEXT));
//
//        expect(what).toEqual("was");
//        expect(where).toEqual("where");
//    }

    public static void main(String[] args) throws IOException {
        File schema = new File("tests/src/resources/com/soundcloud/android/provider/schema_"
                + DatabaseManager.DATABASE_VERSION+".sql");

        FileOutputStream fos = new FileOutputStream(schema);
        fos.write(Table.schemaSnapshot().getBytes());
        fos.close();
    }
}
