package com.soundcloud.android.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.TreeSet;

// Thanks Jeremie! http://jeremie-martinez.com/2016/02/16/unit-tests/

public class SchemaMigrationTest extends AndroidUnitTest {

    private static final File ORIGIN_FILE = new File("src/test/resources/origin.db");
    private static final int ORIGIN_VERSION = 36;

    private File newFile;
    private File upgradedFile;

    @Before
    public void setUp() throws Exception {
        File baseDir = context().getExternalFilesDir("migration-test");

        newFile = new File(baseDir, "new.db");
        upgradedFile = new File(baseDir, "upgraded.db");
        FileUtils.copyFile(ORIGIN_FILE, upgradedFile);
    }

    @Test
    public void shouldBeExactlyTheSameSchemaWhenUpgradingAndWhenRecreating() throws Exception {
        Context context = RuntimeEnvironment.application;
        DatabaseManager helper = DatabaseManager.getInstance(context);

        SQLiteDatabase newDb = SQLiteDatabase.openOrCreateDatabase(newFile, null);
        SQLiteDatabase upgradedDb = SQLiteDatabase.openDatabase(
                upgradedFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE
        );

        helper.onCreate(newDb);
        helper.onUpgrade(upgradedDb, ORIGIN_VERSION, DatabaseManager.DATABASE_VERSION);

        Set<String> newSchema = extractSchema(newFile.getAbsolutePath());
        Set<String> upgradedSchema = extractSchema(upgradedFile.getAbsolutePath());

        assertThat(upgradedSchema).isEqualTo(newSchema);
    }

    private Set<String> extractSchema(String url) throws Exception {
        final Set<String> schema = new TreeSet<>();
        Connection conn = null;
        ResultSet tables = null;
        ResultSet columns = null;

        try {
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
