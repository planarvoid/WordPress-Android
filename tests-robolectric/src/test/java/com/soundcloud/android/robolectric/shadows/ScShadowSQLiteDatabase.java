package com.soundcloud.android.robolectric.shadows;

import static com.xtremelabs.robolectric.util.SQLite.buildInsertString;

import com.xtremelabs.robolectric.internal.Implements;
import com.xtremelabs.robolectric.shadows.ShadowSQLiteDatabase;
import com.xtremelabs.robolectric.util.SQLite;

import android.content.ContentValues;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Iterator;

@Implements(SQLiteDatabase.class)
public class ScShadowSQLiteDatabase extends ShadowSQLiteDatabase {

    @Override
    public long insertWithOnConflict(String table, String nullColumnHack, ContentValues initialValues, int conflictAlgorithm) throws SQLException {
        try {
            SQLite.SQLStringAndBindings sqlInsertString = buildInsertString(table, initialValues, conflictAlgorithm);
            PreparedStatement insert = getConnection().prepareStatement(sqlInsertString.sql, Statement.RETURN_GENERATED_KEYS);
            Iterator<Object> columns = sqlInsertString.columnValues.iterator();
            int i = 1;
            long result = -1;
            while (columns.hasNext()) {
                insert.setObject(i++, columns.next());
            }
            insert.executeUpdate();

            if (initialValues.containsKey("_id")) {
                result = initialValues.getAsInteger("_id");
            } else {
                ResultSet resultSet = insert.getGeneratedKeys();
                if (resultSet.next()) {
                    result = resultSet.getLong(1);
                }
                resultSet.close();
            }
            return result;
        } catch (java.sql.SQLException e) {
            throw new android.database.SQLException(e.getLocalizedMessage());
        }
    }
}
