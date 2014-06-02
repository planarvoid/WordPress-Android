package com.soundcloud.android.storage;

import org.jetbrains.annotations.Nullable;

import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class Query {

    public static final String ORDER_ASC = "ASC";
    public static final String ORDER_DESC = "DESC";

    private static final String TAG = "DBQuery";
    private static final int NOT_SET = -1;

    @Nullable
    private String projection;
    @Nullable
    private String selection;
    @Nullable
    private Object[] selectionArgs;
    @Nullable
    private String order;

    private final String[] tables;
    private int limit;
    private int offset = NOT_SET;
    private String selectFunction;
    private String resultAlias;

    public static Query from(String... tables) {
        return new Query(tables);
    }

    private Query(String... tables) {
        this.tables = tables;
    }

    public Query select(Object... columns) {
        String[] strColumns = new String[columns.length];
        for (int i = 0; i < columns.length; i++) {
            // flatten column queries to strings
            if (columns[i] instanceof Query) {
                Query columnQuery = ((Query) columns[i]);
                if (columnQuery.selectionArgs != null) {
                    selectionArgs = appendSelectionArgs(selectionArgs, columnQuery.selectionArgs);
                }
            }
            strColumns[i] = columns[i].toString();
        }
        this.projection = TextUtils.join(",", strColumns);
        return this;
    }

    public Query exists() {
        Query existsQuery = new Query();
        existsQuery.selectFunction = "exists";
        existsQuery.projection = select("1").buildQueryForExecution();
        if (selectionArgs != null) {
            existsQuery.selectionArgs = appendSelectionArgs(existsQuery.selectionArgs, selectionArgs);
        }
        return existsQuery;
    }

    public Query count() {
        this.selectFunction = "count";
        return this;
    }

    public Query as(String resultAlias) {
        this.resultAlias = resultAlias;
        return this;
    }

    public Query where(String selection, Object... values) {
        this.selection = concatenateWhere(this.selection, selection);
        selectionArgs = appendSelectionArgs(selectionArgs, values);
        return this;
    }

    public Query whereEq(String column, Object value) {
        where(column + " = ?", value);
        return this;
    }

    public Query whereNotEq(String column, Object value) {
        where(column + " != ?", value);
        return this;
    }

    public Query whereGt(String column, Object value) {
        where(column + " > ?", value);
        return this;
    }

    public Query whereGe(String column, Object value) {
        where(column + " >= ?", value);
        return this;
    }

    public Query whereLt(String column, Object value) {
        where(column + " < ?", value);
        return this;
    }

    public Query whereLe(String column, Object value) {
        where(column + " <= ?", value);
        return this;
    }

    // Taken from Android's DatabaseUtilsCompat -- for some reason, that method always returned
    // null in unit tests, so copying this here verbatim.
    @Nullable
    private String concatenateWhere(@Nullable String a, @Nullable String b) {
        if (TextUtils.isEmpty(a)) {
            return b;
        }
        if (TextUtils.isEmpty(b)) {
            return a;
        }

        return "(" + a + ") AND (" + b + ")";
    }

    // Based on Android's DatabaseUtilsCompat -- we want Object arrays for heterogeneous where clauses,
    // not String[].
    private Object[] appendSelectionArgs(@Nullable Object[] originalValues, Object[] newValues) {
        if (originalValues == null || originalValues.length == 0) {
            return newValues;
        }
        Object[] result = new Object[originalValues.length + newValues.length];
        System.arraycopy(originalValues, 0, result, 0, originalValues.length);
        System.arraycopy(newValues, 0, result, originalValues.length, newValues.length);
        return result;
    }

    public Query whereIn(String column, String... values) {
        StringBuilder sb = new StringBuilder(column.length() + values.length * 2 + 5);
        sb.append(column).append(" IN (");
        List<String> wildcards = Collections.nCopies(values.length, "?");
        sb.append(TextUtils.join(",", wildcards));
        sb.append(')');
        selection = sb.toString();
        selectionArgs = appendSelectionArgs(selectionArgs, values);
        return this;
    }

    public Query order(String column, String order) {
        this.order = column + " " + order;
        return this;
    }

    public Query limit(int limit) {
        this.limit = limit;
        return this;
    }

    public Query limit(int limit, int offset) {
        this.limit = limit;
        this.offset = offset;
        return this;
    }

    public ManagedCursor runOn(SQLiteDatabase database) {
        final String sql = buildQueryForExecution();
        final String[] selectionArgs = resolveSelectionArgs();
        logQuery(sql, selectionArgs);
        return new ManagedCursor(database.rawQuery(sql, selectionArgs));
    }

    private static void logQuery(String sql, @Nullable String[] selectionArgs) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, String.format(sql.replaceAll("\\?", "%s"), selectionArgs));
        }
    }

    private String buildQueryForExecution() {
        return buildQueryString().insert(0, "SELECT ").toString();
    }

    private StringBuilder buildQueryString() {
        StringBuilder queryBuilder = new StringBuilder(256);
        buildSelectClause(queryBuilder);
        buildWhereClause(queryBuilder);
        buildOrderClause(queryBuilder);
        buildLimitClause(queryBuilder);
        buildAliasClause(queryBuilder);
        return queryBuilder;
    }

    private void buildAliasClause(StringBuilder queryBuilder) {
        if (resultAlias != null) {
            queryBuilder.append(" AS ").append(resultAlias);
        }
    }

    private void buildLimitClause(StringBuilder queryBuilder) {
        final String limit = resolveLimit();
        if (limit != null) {
            queryBuilder.append(" LIMIT ").append(limit);
        }
    }

    private void buildOrderClause(StringBuilder queryBuilder) {
        if (order != null) {
            queryBuilder.append(" ORDER BY ").append(order);
        }
    }

    private void buildWhereClause(StringBuilder queryBuilder) {
        if (!TextUtils.isEmpty(selection)) {
            queryBuilder.append(" WHERE ").append(selection);
        }
    }

    private void buildSelectClause(StringBuilder queryBuilder) {
        if (projection == null) {
            projection = "*";
        }
        if (selectFunction == null) {
            queryBuilder.append(projection);
        } else {
            queryBuilder.append(selectFunction).append('(').append(projection).append(')');
        }
        if (tables.length > 0) {
            queryBuilder.append(" FROM ").append(TextUtils.join(",", tables));
        }
    }

    @Override
    public String toString() {
        return buildQueryString().toString();
    }

    @Nullable
    private String[] resolveSelectionArgs() {
        if (selectionArgs == null) {
            return null;
        } else {
            String[] stringArgs = new String[selectionArgs.length];
            for (int i = 0; i < selectionArgs.length; i++) {
                stringArgs[i] = selectionArgs[i].toString();
            }
            return stringArgs;
        }
    }

    @Nullable
    private String resolveLimit() {
        String limitClause = null;
        if (limit > 0 && offset == -1) {
            limitClause = Long.toString(limit);
        } else if (limit > 0) {
            limitClause = String.format(Locale.US, "%d,%d", offset, limit);
        }
        return limitClause;
    }
}
