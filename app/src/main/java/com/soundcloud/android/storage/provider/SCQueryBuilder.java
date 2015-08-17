/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.soundcloud.android.storage.provider;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * This is a convenience class that helps build SQL queries to be sent to
 * {@link SQLiteDatabase} objects.
 *
 * Taken without modification from Android source tree (ICS,
 * {@link android.database.sqlite.SQLiteQueryBuilder}).
 * Added for bugfixes and easier testing w/ Robolectric.
 */
public class SCQueryBuilder
{
    private static final String TAG = "SQLiteQueryBuilder";
    private static final Pattern LIMIT_PATTERN =
            Pattern.compile("\\s*\\d+\\s*(,\\s*\\d+\\s*)?");

    private Map<String, String> projectionMap = null;
    private String tables = "";
    private StringBuilder whereClause = null;  // lazily created
    private boolean distinct;
    private SQLiteDatabase.CursorFactory factory;
    private boolean strict;

    public SCQueryBuilder() {
        distinct = false;
        factory = null;
    }

    /**
     * Returns the list of tables being queried
     *
     * @return the list of tables being queried
     */
    public String getTables() {
        return tables;
    }

    /**
     * Sets the list of tables to query. Multiple tables can be specified to perform a join.
     * For example:
     *   setTables("foo, bar")
     *   setTables("foo LEFT OUTER JOIN bar ON (foo.id = bar.foo_id)")
     *
     * @param inTables the list of tables to query on
     */
    public void setTables(String inTables) {
        tables = inTables;
    }

    /**
     * Append a chunk to the WHERE clause of the query. All chunks appended are surrounded
     * by parenthesis and ANDed with the selection passed to {@link #query}. The final
     * WHERE clause looks like:
     *
     * WHERE (&lt;executeAppendTask chunk 1>&lt;executeAppendTask chunk2>) AND (&lt;query() selection parameter>)
     *
     * @param inWhere the chunk of text to executeAppendTask to the WHERE clause.
     */
    public void appendWhere(CharSequence inWhere) {
        if (whereClause == null) {
            whereClause = new StringBuilder(inWhere.length() + 16);
        }
        if (whereClause.length() == 0) {
            whereClause.append('(');
        }
        whereClause.append(inWhere);
    }

    /**
     * Build an SQL query string from the given clauses.
     *
     * @param distinct true if you want each row to be unique, false otherwise.
     * @param tables The table names to compile the query against.
     * @param columns A list of which columns to return. Passing null will
     *            return all columns, which is discouraged to prevent reading
     *            data from storage that isn't going to be used.
     * @param where A filter declaring which rows to return, formatted as an SQL
     *            WHERE clause (excluding the WHERE itself). Passing null will
     *            return all rows for the given URL.
     * @param groupBy A filter declaring how to group rows, formatted as an SQL
     *            GROUP BY clause (excluding the GROUP BY itself). Passing null
     *            will cause the rows to not be grouped.
     * @param having A filter declare which row groups to include in the cursor,
     *            if row grouping is being used, formatted as an SQL HAVING
     *            clause (excluding the HAVING itself). Passing null will cause
     *            all row groups to be included, and is required when row
     *            grouping is not being used.
     * @param orderBy How to order the rows, formatted as an SQL ORDER BY clause
     *            (excluding the ORDER BY itself). Passing null will use the
     *            default sort order, which may be unordered.
     * @param limit Limits the number of rows returned by the query,
     *            formatted as LIMIT clause. Passing null denotes no LIMIT clause.
     * @return the SQL query string
     */
    public static String buildQueryString(
            boolean distinct, String tables, String[] columns, String where,
            String groupBy, String having, String orderBy, String limit) {
        if (TextUtils.isEmpty(groupBy) && !TextUtils.isEmpty(having)) {
            throw new IllegalArgumentException(
                    "HAVING clauses are only permitted when using a groupBy clause");
        }
        if (!TextUtils.isEmpty(limit) && !LIMIT_PATTERN.matcher(limit).matches()) {
            throw new IllegalArgumentException("invalid LIMIT clauses:" + limit);
        }

        StringBuilder query = new StringBuilder(120);

        query.append("SELECT ");
        if (distinct) {
            query.append("DISTINCT ");
        }
        if (columns != null && columns.length != 0) {
            appendColumns(query, columns);
        } else {
            query.append("* ");
        }
        query.append("FROM ").append(tables);
        appendClause(query, " WHERE ", where);
        appendClause(query, " GROUP BY ", groupBy);
        appendClause(query, " HAVING ", having);
        appendClause(query, " ORDER BY ", orderBy);
        appendClause(query, " LIMIT ", limit);

        return query.toString();
    }

    private static void appendClause(StringBuilder s, String name, String clause) {
        if (!TextUtils.isEmpty(clause)) {
            s.append(name);
            s.append(clause);
        }
    }

    /**
     * Add the names that are non-null in columns to s, separating
     * them with commas.
     */
    public static void appendColumns(StringBuilder s, String[] columns) {
        int n = columns.length;

        for (int i = 0; i < n; i++) {
            String column = columns[i];

            if (column != null) {
                if (i > 0) {
                    s.append(", ");
                }
                s.append(column);
            }
        }
        s.append(' ');
    }

    /**
     * Perform a query by combining all current settings and the
     * information passed into this method.
     *
     * @param db the database to query on
     * @param projectionIn A list of which columns to return. Passing
     *   null will return all columns, which is discouraged to prevent
     *   reading data from storage that isn't going to be used.
     * @param selection A filter declaring which rows to return,
     *   formatted as an SQL WHERE clause (excluding the WHERE
     *   itself). Passing null will return all rows for the given URL.
     * @param selectionArgs You may include ?s in selection, which
     *   will be replaced by the values from selectionArgs, in order
     *   that they appear in the selection. The values will be bound
     *   as Strings.
     * @param groupBy A filter declaring how to group rows, formatted
     *   as an SQL GROUP BY clause (excluding the GROUP BY
     *   itself). Passing null will cause the rows to not be grouped.
     * @param having A filter declare which row groups to include in
     *   the cursor, if row grouping is being used, formatted as an
     *   SQL HAVING clause (excluding the HAVING itself).  Passing
     *   null will cause all row groups to be included, and is
     *   required when row grouping is not being used.
     * @param sortOrder How to order the rows, formatted as an SQL
     *   ORDER BY clause (excluding the ORDER BY itself). Passing null
     *   will use the default sort order, which may be unordered.
     * @return a cursor over the result set
     * @see android.content.ContentResolver#query(android.net.Uri, String[],
     *      String, String[], String)
     */
    public Cursor query(SQLiteDatabase db, String[] projectionIn,
            String selection, String[] selectionArgs, String groupBy,
            String having, String sortOrder) {
        return query(db, projectionIn, selection, selectionArgs, groupBy, having, sortOrder,
                null /* limit */);
    }

    /**
     * Perform a query by combining all current settings and the
     * information passed into this method.
     *
     * @param db the database to query on
     * @param projectionIn A list of which columns to return. Passing
     *   null will return all columns, which is discouraged to prevent
     *   reading data from storage that isn't going to be used.
     * @param selection A filter declaring which rows to return,
     *   formatted as an SQL WHERE clause (excluding the WHERE
     *   itself). Passing null will return all rows for the given URL.
     * @param selectionArgs You may include ?s in selection, which
     *   will be replaced by the values from selectionArgs, in order
     *   that they appear in the selection. The values will be bound
     *   as Strings.
     * @param groupBy A filter declaring how to group rows, formatted
     *   as an SQL GROUP BY clause (excluding the GROUP BY
     *   itself). Passing null will cause the rows to not be grouped.
     * @param having A filter declare which row groups to include in
     *   the cursor, if row grouping is being used, formatted as an
     *   SQL HAVING clause (excluding the HAVING itself).  Passing
     *   null will cause all row groups to be included, and is
     *   required when row grouping is not being used.
     * @param sortOrder How to order the rows, formatted as an SQL
     *   ORDER BY clause (excluding the ORDER BY itself). Passing null
     *   will use the default sort order, which may be unordered.
     * @param limit Limits the number of rows returned by the query,
     *   formatted as LIMIT clause. Passing null denotes no LIMIT clause.
     * @return a cursor over the result set
     * @see android.content.ContentResolver#query(android.net.Uri, String[],
     *      String, String[], String)
     */
    public Cursor query(SQLiteDatabase db, String[] projectionIn,
            String selection, String[] selectionArgs, String groupBy,
            String having, String sortOrder, String limit) {
        if (tables == null) {
            return null;
        }

        if (strict && selection != null && selection.length() > 0) {
            // Validate the user-supplied selection to detect syntactic anomalies
            // in the selection string that could indicate a SQL injection attempt.
            // The idea is to ensure that the selection clause is a valid SQL expression
            // by compiling it twice: once wrapped in parentheses and once as
            // originally specified. An attacker cannot create an expression that
            // would escape the SQL expression while maintaining balanced parentheses
            // in both the wrapped and original forms.
            String sqlForValidation = buildQuery(projectionIn, "(" + selection + ")", groupBy,
                    having, sortOrder, limit);
            validateSql(db, sqlForValidation); // will throw if query is invalid
        }

        String sql = buildQuery(
                projectionIn, selection, groupBy, having,
                sortOrder, limit);

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Performing query: " + sql);
        }
        return db.rawQueryWithFactory(
                factory, sql, selectionArgs,
                SQLiteDatabase.findEditTable(tables)); // will throw if query is invalid
    }

    /**
     * Verifies that a SQL statement is valid by compiling it.
     * If the SQL statement is not valid, this method will throw a {@link SQLiteException}.
     */
    private void validateSql(SQLiteDatabase db, String sql) {
        /*
        db.lock(sql);
        try {
            new SQLiteCompiledSql(db, sql).releaseSqlStatement();
        } finally {
            db.unlock();
        }
        */
    }

    /**
     * Construct a SELECT statement suitable for use in a group of
     * SELECT statements that will be joined through UNION operators
     * in buildUnionQuery.
     *
     * @param projectionIn A list of which columns to return. Passing
     *    null will return all columns, which is discouraged to
     *    prevent reading data from storage that isn't going to be
     *    used.
     * @param selection A filter declaring which rows to return,
     *   formatted as an SQL WHERE clause (excluding the WHERE
     *   itself).  Passing null will return all rows for the given
     *   URL.
     * @param groupBy A filter declaring how to group rows, formatted
     *   as an SQL GROUP BY clause (excluding the GROUP BY itself).
     *   Passing null will cause the rows to not be grouped.
     * @param having A filter declare which row groups to include in
     *   the cursor, if row grouping is being used, formatted as an
     *   SQL HAVING clause (excluding the HAVING itself).  Passing
     *   null will cause all row groups to be included, and is
     *   required when row grouping is not being used.
     * @param sortOrder How to order the rows, formatted as an SQL
     *   ORDER BY clause (excluding the ORDER BY itself). Passing null
     *   will use the default sort order, which may be unordered.
     * @param limit Limits the number of rows returned by the query,
     *   formatted as LIMIT clause. Passing null denotes no LIMIT clause.
     * @return the resulting SQL SELECT statement
     */
    public String buildQuery(
            String[] projectionIn, String selection, String groupBy,
            String having, String sortOrder, String limit) {
        String[] projection = computeProjection(projectionIn);

        StringBuilder where = new StringBuilder();
        boolean hasBaseWhereClause = whereClause != null && whereClause.length() > 0;

        if (hasBaseWhereClause) {
            where.append(whereClause.toString());
            where.append(')');
        }

        // Tack on the user's selection, if present.
        if (selection != null && selection.length() > 0) {
            if (hasBaseWhereClause) {
                where.append(" AND ");
            }

            where.append('(');
            where.append(selection);
            where.append(')');
        }

        return buildQueryString(
                distinct, tables, projection, where.toString(),
                groupBy, having, sortOrder, limit);
    }

    /**
     * @deprecated This method's signature is misleading since no SQL parameter
     * substitution is carried out.  The selection arguments parameter does not get
     * used at all.  To avoid confusion, call
     * {@link #buildQuery(String[], String, String, String, String, String)} instead.
     */
    @Deprecated
    public String buildQuery(
            String[] projectionIn, String selection, String[] selectionArgs,
            String groupBy, String having, String sortOrder, String limit) {
        return buildQuery(projectionIn, selection, groupBy, having, sortOrder, limit);
    }

    private String[] computeProjection(String[] projectionIn) {
        if (projectionIn != null && projectionIn.length > 0) {
            if (projectionMap != null) {
                String[] projection = new String[projectionIn.length];
                int length = projectionIn.length;

                for (int i = 0; i < length; i++) {
                    String userColumn = projectionIn[i];
                    String column = projectionMap.get(userColumn);

                    if (column != null) {
                        projection[i] = column;
                        continue;
                    }

                    if (!strict &&
                            ( userColumn.contains(" AS ") || userColumn.contains(" as "))) {
                        /* A column alias already exist */
                        projection[i] = userColumn;
                        continue;
                    }

                    throw new IllegalArgumentException("Invalid column "
                            + projectionIn[i]);
                }
                return projection;
            } else {
                return projectionIn;
            }
        } else if (projectionMap != null) {
            // Return all columns in projection map.
            Set<Entry<String, String>> entrySet = projectionMap.entrySet();
            String[] projection = new String[entrySet.size()];
            Iterator<Entry<String, String>> entryIter = entrySet.iterator();
            int i = 0;

            while (entryIter.hasNext()) {
                Entry<String, String> entry = entryIter.next();

                // Don't include the _count column when people ask for no projection.
                if (entry.getKey().equals(BaseColumns._COUNT)) {
                    continue;
                }
                projection[i++] = entry.getValue();
            }
            return projection;
        }
        return null;
    }
}
