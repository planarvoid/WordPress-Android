package com.soundcloud.android.testsupport;

import static com.soundcloud.android.testsupport.AndroidUnitTest.activity;
import static com.soundcloud.android.testsupport.AndroidUnitTest.resources;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.storage.DatabaseManager;
import com.soundcloud.propeller.PropellerDatabase;
import org.apache.commons.io.FileUtils;
import org.robolectric.RuntimeEnvironment;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.TreeSet;

public class DatabaseMigrationHelper {
    private static final File ORIGIN_FILE = new File("src/test/resources/origin.db");
    private static final int ORIGIN_VERSION = 36;

    private File newFile;
    private File upgradedFile;
    private int currentVersion;

    public DatabaseMigrationHelper() throws IOException {
        File baseDir = activity().getExternalFilesDir("migration-test");
        newFile = new File(baseDir, "new.db");
        upgradedFile = new File(baseDir, "upgraded.db");
        currentVersion = ORIGIN_VERSION;

        Context context = RuntimeEnvironment.application;
        DatabaseManager helper = DatabaseManager.getInstance(context, applicationProperties());
        FileUtils.copyFile(ORIGIN_FILE, upgradedFile);
        helper.onCreate(SQLiteDatabase.openOrCreateDatabase(newFile, null));
    }

    public void upgradeTo(int version) {
        Context context = RuntimeEnvironment.application;
        DatabaseManager helper = DatabaseManager.getInstance(context, applicationProperties());
        SQLiteDatabase upgradedDb = getUpgradedDatabase();
        helper.onUpgrade(upgradedDb, currentVersion, version);
        currentVersion = version;
    }

    private ApplicationProperties applicationProperties() {
        return new ApplicationProperties(resources());
    }

    public void upgradeToCurrent() {
        upgradeTo(DatabaseManager.DATABASE_VERSION);
    }

    public void assertSchemas() {
        try {
            Set<String> newSchema = extractSchema(newFile.getAbsolutePath());
            Set<String> upgradedSchema = extractSchema(upgradedFile.getAbsolutePath());
            assertThat(upgradedSchema).isEqualTo(newSchema);
        } catch (Exception e) {
            e.printStackTrace();
            fail("should not throw exceptions while extracting schemas");
        }
    }

    public void assertTableCount(String table, int count) {
        SQLiteDatabase upgradedDb = getUpgradedDatabase();
        Cursor cursor = upgradedDb.rawQuery("SELECT count(*) FROM " + table, new String[]{});
        cursor.moveToFirst();
        assertThat(cursor.getInt(0)).isEqualTo(count);
        cursor.close();
    }

    public void insert(String table, ContentValues values) {
        SQLiteDatabase upgradedDb = getUpgradedDatabase();
        upgradedDb.insert(table, null, values);
    }

    public SQLiteDatabase getUpgradedDatabase() {
        return SQLiteDatabase.openDatabase(
                upgradedFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE
        );
    }

    public PropellerDatabase getPropellerDatabase() {
        return new PropellerDatabase(getUpgradedDatabase());
    }

    private Set<String> extractSchema(String url) throws Exception {
        final Set<String> schema = new TreeSet<>();
        Connection conn = null;
        ResultSet tables = null;
        ResultSet columns = null;

        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + url);
            tables = conn.getMetaData().getTables(null, null, null, null);

            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                String tableType = tables.getString("TABLE_TYPE");
                schema.add(tableType + " " + tableName);

                columns = conn.getMetaData().getColumns(null, null, tableName, null);

                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    String columnType = columns.getString("TYPE_NAME");
                    String columnNullable = columns.getString("IS_NULLABLE");
                    String columnDefault = columns.getString("COLUMN_DEF");
                    schema.add("TABLE " + tableName +
                                       " COLUMN " + columnName + " " + columnType +
                                       " NULLABLE=" + columnNullable +
                                       " DEFAULT=" + columnDefault);
                }
            }
            return schema;
        } finally {
            closeResultSet(tables);
            closeResultSet(columns);
            closeConnection(conn);
        }
    }

    private void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException exception) {
                // ignore
            }
        }
    }

    private void closeResultSet(ResultSet tables) {
        try {
            if (tables != null) {
                tables.close();
            }
        } catch (Exception exception) {
            // ignore
        }
    }
}
