package com.soundcloud.android.storage;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

@RunWith(SoundCloudTestRunner.class)
public class QueryTest {

    private Query query;

    @Mock
    private SQLiteDatabase database;

    @Before
    public void setup() {
        query = Query.from("table");
    }

    @Test
    public void shouldRunSelectOnSingleTable() {
        query.select("a", "b").runOn(database);
        verify(database).rawQuery("SELECT a,b FROM table", null);
    }

    @Test
    public void shouldRunSelectOnMultipleTables() {
        Query.from("t1", "t2").select("a", "b").runOn(database);
        verify(database).rawQuery("SELECT a,b FROM t1,t2", null);
    }

    @Test
    public void shouldRunSelectWithResultAlias() {
        query.select("a").as("custom").runOn(database);
        verify(database).rawQuery("SELECT a FROM table AS custom", null);
    }

    @Test
    public void shouldSelectAllByDefaultIfNoSelectSpecified() {
        query.runOn(database);
        verify(database).rawQuery("SELECT * FROM table", null);
    }

    @Test
    public void shouldRunSelectWithFunction() {
        query.select("a", Query.from("t1").whereEq("x", 1).exists()).runOn(database);
        verify(database).rawQuery("SELECT a,exists(SELECT 1 FROM t1 WHERE x = ?) FROM table", new String[]{"1"});
    }

    @Test
    public void shouldReturnManagedCursorForExecutedQuery() {
        Cursor cursor = mock(Cursor.class);
        when(database.rawQuery(anyString(), any(String[].class))).thenReturn(cursor);
        expect(query.runOn(database).getCursor()).toBe(cursor);
    }

    @Test
    public void shouldApplyWhereClause() {
        query.where("x = ?", "y").runOn(database);
        verify(database).rawQuery("SELECT * FROM table WHERE x = ?", new String[]{"y"});
    }

    @Test
    public void shouldApplyWhereEqualsClause() {
        query.whereEq("x", "y").runOn(database);
        verify(database).rawQuery("SELECT * FROM table WHERE x = ?", new String[]{"y"});
    }

    @Test
    public void shouldApplyWhereNotEqualsClause() {
        query.whereNotEq("x", "y").runOn(database);
        verify(database).rawQuery("SELECT * FROM table WHERE x != ?", new String[]{"y"});
    }

    @Test
    public void shouldApplyWhereGreaterThanClause() {
        query.whereGt("x", 1).runOn(database);
        verify(database).rawQuery("SELECT * FROM table WHERE x > ?", new String[]{"1"});
    }

    @Test
    public void shouldApplyWhereGreaterOrEqualsClause() {
        query.whereGe("x", 1).runOn(database);
        verify(database).rawQuery("SELECT * FROM table WHERE x >= ?", new String[]{"1"});
    }

    @Test
    public void shouldApplyWhereLessThanClause() {
        query.whereLt("x", 1).runOn(database);
        verify(database).rawQuery("SELECT * FROM table WHERE x < ?", new String[]{"1"});
    }

    @Test
    public void shouldApplyWhereLessOrEqualsClause() {
        query.whereLe("x", 1).runOn(database);
        verify(database).rawQuery("SELECT * FROM table WHERE x <= ?", new String[]{"1"});
    }

    @Test
    public void shouldConcatenateMultipleWhereClauses() {
        query.where("x = ?", "y").where("z = ?", 1).runOn(database);
        verify(database).rawQuery("SELECT * FROM table WHERE (x = ?) AND (z = ?)", new String[]{"y", "1"});
    }

    @Test
    public void shouldApplyWhereInClause() {
        query.whereIn("x", "1", "2").runOn(database);
        verify(database).rawQuery("SELECT * FROM table WHERE x IN (?,?)", new String[]{"1", "2"});
    }

    @Test
    public void shouldApplySimpleLimitClause() {
        query.limit(10).runOn(database);
        verify(database).rawQuery("SELECT * FROM table LIMIT 10", null);
    }

    @Test
    public void shouldApplyLimitWithOffsetClause() {
        query.limit(10, 5).runOn(database);
        verify(database).rawQuery("SELECT * FROM table LIMIT 5,10", null);
    }

    @Test
    public void shouldApplyOrderAscendingClause() {
        query.order("b", Query.ORDER_ASC).runOn(database);
        verify(database).rawQuery("SELECT * FROM table ORDER BY b ASC", null);
    }

    @Test
    public void shouldApplyOrderDescendingClause() {
        query.order("b", Query.ORDER_DESC).runOn(database);
        verify(database).rawQuery("SELECT * FROM table ORDER BY b DESC", null);
    }

    @Test
    public void shouldRunFullSelect() {
        Query.from("t1", "t2").select("a", "b")
                .where("x = ?", "z")
                .where("y < ?", 2)
                .where("z > ?", 3)
                .order("b", Query.ORDER_DESC)
                .limit(5)
                .runOn(database);
        verify(database).rawQuery(
                "SELECT a,b FROM t1,t2 WHERE ((x = ?) AND (y < ?)) AND (z > ?) ORDER BY b DESC LIMIT 5",
                new String[]{"z", "2", "3"});
    }

    @Test
    public void shouldRunCountQuery() {
        query.count().runOn(database);
        verify(database).rawQuery("SELECT count(*) FROM table", null);
    }

    @Test
    public void shouldRunSimpleExistsQuery() {
        Query.from("table").exists().runOn(database);
        verify(database).rawQuery("SELECT exists(SELECT 1 FROM table)", null);
    }

    @Test
    public void shouldRunExistsQueryWithConditions() {
        Query.from("table").whereEq("x", 1).exists().runOn(database);
        verify(database).rawQuery("SELECT exists(SELECT 1 FROM table WHERE x = ?)", new String[]{"1"});
    }
}