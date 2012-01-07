package com.soundcloud.android.robolectric;

import java.sql.ResultSet;

import com.xtremelabs.robolectric.util.DatabaseConfig;

public class SQLiteMap implements DatabaseConfig.DatabaseMap {

    public String getDriverClassName() {
        return "org.sqlite.JDBC";
    }

    public String getConnectionString() {
        return "jdbc:sqlite::memory:";
//    return "jdbc:sqlite:foo.db";
    }

    public String getScrubSQL(String sql) {
        return sql;
    }

    public String getSelectLastInsertIdentity() {
        return "SELECT last_insert_rowid() AS id";
    }

    public int getResultSetType() {
        return ResultSet.TYPE_FORWARD_ONLY;
    }
}
