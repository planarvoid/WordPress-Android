package com.soundcloud.android.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.List;

public class SchemaMigrationHelperTest extends AndroidUnitTest {

    private final ApplicationProperties applicationProperties = new ApplicationProperties(resources());
    private final DatabaseManager databaseManager = new DatabaseManager(context(), applicationProperties);

    @Test
    public void shouldGetColumnNames() throws Exception {
        SQLiteDatabase db = databaseManager.getWritableDatabase();
        List<String> columns = SchemaMigrationHelper.getColumnNames(Table.Waveforms, db);
        assertThat(columns.isEmpty()).isFalse();
    }

    @Test
    public void shouldDropAndCreateTable() throws Exception {
        SQLiteDatabase db = databaseManager.getWritableDatabase();
        Table table = Table.Waveforms;

        assertThat(exists(table, db)).isTrue();
        SchemaMigrationHelper.drop(table, db);
        assertThat(exists(table, db)).isFalse();
        SchemaMigrationHelper.create(table, db);
        assertThat(exists(table, db)).isTrue();
        SchemaMigrationHelper.recreate(table, db);
        assertThat(exists(table, db)).isTrue();
    }

    @Test
    public void shouldAddColumnToWaveforms() throws Exception {
        SQLiteDatabase db = databaseManager.getWritableDatabase();

        String oldSchema = Table.Waveforms.createString;
        db.execSQL(oldSchema);

        final int insertIndex = Table.Waveforms.createString.lastIndexOf("PRIMARY");
        String newSchema = Table.Waveforms.createString.substring(0, insertIndex) +
                " new_column INTEGER," + Table.Waveforms.createString.substring(insertIndex);

        final int colCount = SchemaMigrationHelper.alterColumns(db,
                                                                Table.Waveforms.name(),
                                                                newSchema,
                                                                new String[0],
                                                                new String[0]).size();
        final List<String> columnNames = SchemaMigrationHelper.getColumnNames(db, Table.Waveforms.name());
        assertThat(columnNames).contains("new_column");
        assertThat(columnNames.size()).isEqualTo(colCount + 1);
    }

    @Test
    public void shouldAlterColumnsWithoutRenamingColumn() throws Exception {
        SQLiteDatabase db = databaseManager.getWritableDatabase();

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

        assertThat(db.insert("foo", null, cv)).isEqualTo(1L);

        assertThat(SchemaMigrationHelper.alterColumns(db, "foo", newSchema, new String[0], new String[0]))
                .containsExactly("_id", "keep_me");

        assertThat(SchemaMigrationHelper.getColumnNames(db, "foo")).containsExactly("_id", "keep_me", "new_column");

        Cursor c = db.query("foo", null, null, null, null, null, null);

        // make sure old data is still around
        assertThat(c.getCount()).isEqualTo(1);
        assertThat(c.moveToNext()).isTrue();
        assertThat(c.getLong(c.getColumnIndex("_id"))).isEqualTo(1L);
        assertThat(c.getString(c.getColumnIndex("keep_me"))).isEqualTo("blavalue");
        assertThat(c.isNull(c.getColumnIndex("new_column"))).isTrue();
    }

    @Test
    public void shouldAlterColumnsWithRenamingColumn() throws Exception {
        SQLiteDatabase db = databaseManager.getWritableDatabase();

        String oldSchema = Table.buildCreateString("foo", "(_id INTEGER PRIMARY KEY," +
                " keep_me VARCHAR(255)," +
                " drop_me INTEGER," +
                " rename_me INTEGER);", false);

        String newSchema = Table.buildCreateString("foo", "(_id INTEGER PRIMARY KEY," +
                " keep_me VARCHAR(255)," +
                " new_column INTEGER," +
                " renamed INTEGER); ", false);

        db.execSQL(oldSchema);

        ContentValues cv = new ContentValues();
        cv.put("keep_me", "blavalue");
        cv.put("drop_me", 100);
        cv.put("rename_me", 200);

        assertThat(db.insert("foo", null, cv)).isEqualTo(1L);

        assertThat(SchemaMigrationHelper.alterColumns(db,
                                                      "foo",
                                                      newSchema,
                                                      new String[]{"rename_me"},
                                                      new String[]{"renamed"}))
                .containsExactly("renamed", "_id", "keep_me");

        assertThat(SchemaMigrationHelper.getColumnNames(db, "foo")).containsExactly("_id",
                                                                                    "keep_me",
                                                                                    "new_column",
                                                                                    "renamed");

        Cursor c = db.query("foo", null, null, null, null, null, null);

        // make sure old data is still around
        assertThat(c.getCount()).isEqualTo(1);
        assertThat(c.moveToNext()).isTrue();
        assertThat(c.getLong(c.getColumnIndex("_id"))).isEqualTo(1L);
        assertThat(c.getString(c.getColumnIndex("keep_me"))).isEqualTo("blavalue");
        assertThat(c.getInt(c.getColumnIndex("renamed"))).isEqualTo(200);
    }

    private boolean exists(Table table, SQLiteDatabase db) {
        try {
            db.execSQL("SELECT 1 FROM " + table.name());
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

}
