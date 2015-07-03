package com.soundcloud.android.storage;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.xtremelabs.robolectric.tester.android.database.TestCursor;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.Arrays;

@RunWith(DefaultTestRunner.class)
public class BaseDAOTest extends AbstractDAOTest<BaseDAO<PublicApiTrack>> {

    public BaseDAOTest() {
        super(new TestDAO(mock(ContentResolver.class)));
    }

    @Test
    public void shouldStoreSingleRecordWithDependencies() {
        ContentResolver resolverMock = getDAO().getContentResolver();
        PublicApiTrack record = new PublicApiTrack();
        record.user = new PublicApiUser();

        when(resolverMock.insert(any(Uri.class), any(ContentValues.class))).thenReturn(record.toUri());

        getDAO().create(record);

        verify(resolverMock).insert(eq(getDAO().getContent().uri), any(ContentValues.class));
        verify(resolverMock).bulkInsert(eq(Content.USERS.uri), any(ContentValues[].class));
    }

    @Test
    public void shouldSetRecordIdForNewRecords() {
        ContentResolver resolverMock = getDAO().getContentResolver();
        PublicApiTrack record = new PublicApiTrack(0); // 0 is not a valid record ID

        Uri newResourceUri = Uri.parse("http://com.soundcloud.android.provider.ScContentProvider/tracks/123");
        when(resolverMock.insert(any(Uri.class), any(ContentValues.class))).thenReturn(newResourceUri);

        getDAO().create(record);

        expect(record.getId()).toBe(123L);
    }

    @Test
    public void shouldStoreCollectionOfRecords() {
        ContentResolver resolverMock = getDAO().getContentResolver();

        PublicApiTrack track1 = new PublicApiTrack();
        PublicApiTrack track2 = new PublicApiTrack();
        getDAO().createCollection(Arrays.asList(track1, track2));

        verify(resolverMock).bulkInsert(eq(getDAO().getContent().uri), any(ContentValues[].class));
    }

    @Test
    public void shouldCountRecordsWithWhereClause() {
        ContentResolver resolverMock = getDAO().getContentResolver();
        Cursor filteredCount = resolverMock.query(
                eq(getDAO().getContent().uri),
                isNull(String[].class),
                eq("x > ?"),
                eq(new String[]{"5"}),
                isNull(String.class));

        when(filteredCount).thenReturn(new CursorStub(1));

        expect(getDAO().count("x > ?", "5")).toBe(1);
    }

    @Test
    public void shouldAllowToBuildCustomQueries() {
        ContentResolver resolverMock = getDAO().getContentResolver();

        getDAO().buildQuery().select("ColA", "ColB").where("x = ?", "1").order("b ASC").queryAll();

        verify(resolverMock).query(
                eq(getDAO().getContent().uri),
                eq(new String[]{"ColA", "ColB"}),
                eq("x = ?"),
                eq(new String[]{"1"}),
                eq("b ASC"));
    }

    @Test
    public void shouldLimitResults() {
        ContentResolver resolverMock = getDAO().getContentResolver();

        getDAO().buildQuery().limit(1).queryAll();

        verify(resolverMock).query(
                eq(getDAO().getContent().uri.buildUpon().appendQueryParameter("limit", "1").build()),
                isNull(String[].class),
                isNull(String.class),
                isNull(String[].class),
                isNull(String.class));

    }

    @Test
    public void shouldQueryForFirstRecord() {
        ContentResolver resolverMock = getDAO().getContentResolver();

        // no results found
        expect(getDAO().buildQuery().first()).toBeNull();

        // one result found
        Cursor query = resolverMock.query(
                eq(getDAO().getContent().uri.buildUpon().appendQueryParameter("limit", "1").build()),
                isNull(String[].class),
                isNull(String.class),
                isNull(String[].class),
                isNull(String.class));

        when(query).thenReturn(new CursorStub(1));
        expect(getDAO().buildQuery().first()).toBeInstanceOf(PublicApiTrack.class);
    }

    @Test
    public void shouldQueryAllRecords() {
        ContentResolver resolverMock = getDAO().getContentResolver();

        // we also stub this call with a result cursor to improve code coverage
        when(resolverMock.query(getDAO().getContent().uri, null, null, null, null)).thenReturn(new CursorStub(1));

        getDAO().queryAll();

        verify(resolverMock).query(getDAO().getContent().uri, null, null, null, null);
    }

    @Test
    public void shouldQueryAllRecordsWithWhereClause() {
        ContentResolver resolverMock = getDAO().getContentResolver();

        getDAO().buildQuery().where("a = ? AND b < ?", "foo", "2").queryAll();

        verify(resolverMock).query(
                eq(getDAO().getContent().uri),
                isNull(String[].class),
                eq("a = ? AND b < ?"),
                eq(new String[]{"foo", "2"}),
                isNull(String.class));
    }

    @Test
    public void shouldQueryAllRecordsWithWhereInClause() {
        ContentResolver resolverMock = getDAO().getContentResolver();

        getDAO().buildQuery().whereIn("_id", "1", "2", "3").queryAll();

        verify(resolverMock).query(
                eq(getDAO().getContent().uri),
                isNull(String[].class),
                eq("_id IN (?,?,?)"),
                eq(new String[]{"1", "2", "3"}),
                isNull(String.class));
    }

    @Test
    public void shouldCombineWhereAndWhereInClauses() {
        ContentResolver resolverMock = getDAO().getContentResolver();

        getDAO().buildQuery().whereIn("_id", "1", "2", "3").where("AND x = ?", "4").queryAll();

        verify(resolverMock).query(
                eq(getDAO().getContent().uri),
                isNull(String[].class),
                eq("_id IN (?,?,?) AND x = ?"),
                eq(new String[]{"1", "2", "3", "4"}),
                isNull(String.class));
    }

    @Test
    public void shouldQueryForIds() {
        ContentResolver resolverMock = getDAO().getContentResolver();

        getDAO().buildQuery().where("x = ?", "1").queryIds();

        verify(resolverMock).query(
                eq(getDAO().getContent().uri),
                eq(new String[]{BaseColumns._ID}),
                eq("x = ?"),
                eq(new String[]{"1"}),
                isNull(String.class));
    }

    @Test
    public void shouldDeleteAllRecords() {
        ContentResolver resolverMock = getDAO().getContentResolver();

        getDAO().deleteAll();

        verify(resolverMock).delete(eq(Content.TRACKS.uri), isNull(String.class), eq(new String[]{}));
    }

    private static class TestDAO extends BaseDAO<PublicApiTrack> {

        protected TestDAO(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        public Content getContent() {
            return Content.TRACKS;
        }

        @Override
        protected PublicApiTrack objFromCursor(Cursor cursor) {
            return new PublicApiTrack();
        }
    }

    private static class CursorStub extends TestCursor {
        private int resultCount, itemsLeft;

        private CursorStub(int resultCount) {
            this.resultCount = resultCount;
            this.itemsLeft = resultCount;
        }

        @Override
        public int getCount() {
            return resultCount;
        }

        @Override
        public void close() {
        }

        @Override
        public Uri getNotificationUri() {
            return null;
        }

        @Override
        public boolean moveToNext() {
            if (itemsLeft == 0) {
                return false;
            }
            itemsLeft -= 1;
            return true;
        }
    }

}
