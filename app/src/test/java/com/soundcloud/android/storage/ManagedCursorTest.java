package com.soundcloud.android.storage;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;

import android.database.Cursor;

@RunWith(SoundCloudTestRunner.class)
public class ManagedCursorTest {

    private ManagedCursor managedCursor;

    private Cursor cursor;

    @Mock
    private Observer observer;

    @Before
    public void setup() {
        cursor = new TestCursor(new Object[][]{
                {1L, "x", 0},
                {2L, "y", 1}
        }, "_id", "str_col", "int_col");
        managedCursor = new ManagedCursor(cursor);
    }

    @Test
    public void shouldWrapGivenCursor() {
        expect(managedCursor.getCursor()).toBe(cursor);
    }

    @Test
    public void shouldGetResultCount() {
        expect(managedCursor.getResultCount()).toBe(2);
    }

    @Test
    public void shouldGetColumnCount() {
        expect(managedCursor.getColumnCount()).toBe(3);
    }

    @Test
    public void shouldReadFromCurrentCursorPosition() {
        cursor.moveToPosition(1);
        expect(managedCursor.getLong("_id")).toEqual(2L);
    }

    @Test
    public void shouldReadLongFromCursor() {
        cursor.moveToPosition(0);
        expect(managedCursor.getLong("_id")).toEqual(1L);
    }

    @Test
    public void shouldReadStringFromCursor() {
        cursor.moveToPosition(0);
        expect(managedCursor.getString("str_col")).toEqual("x");
    }

    @Test
    public void shouldReadIntFromCursor() {
        cursor.moveToPosition(0);
        expect(managedCursor.getInt("int_col")).toEqual(0);
    }

    @Test
    public void shouldReadBooleanFromCursor() {
        cursor.moveToPosition(0);
        expect(managedCursor.getBoolean("int_col")).toEqual(false);
        cursor.moveToPosition(1);
        expect(managedCursor.getBoolean("int_col")).toEqual(true);
    }

    @Test
    public void shouldReadDateFromCursorTimestamp() {
        cursor.moveToPosition(0);
        expect(managedCursor.getDateFromTimestamp("_id").getTime()).toBe(1L);
    }

    @Test
    public void shouldEmitCursorRowsToRxObserverUsingGivenRowMappingAndCloseCursor() {
        ManagedCursor.RowMapper<String> f = new ManagedCursor.RowMapper<String>() {
            @Override
            public String call(ManagedCursor managedCursor) {
                return managedCursor.getString("str_col");
            }
        };

        managedCursor.emit(observer, f);
        verify(observer).onNext("x");
        verify(observer).onNext("y");
        verify(observer).onCompleted();
        verifyNoMoreInteractions(observer);

        expect(cursor.isClosed()).toBeTrue();
    }

    @Test
    public void shouldEmitNothingAndCallOnErrorIfRowConversionFails() {
        final RuntimeException exception = new RuntimeException();
        ManagedCursor.RowMapper<String> f = new ManagedCursor.RowMapper<String>() {
            @Override
            public String call(ManagedCursor managedCursor) {
                throw exception;
            }
        };

        managedCursor.emit(observer, f);
        verify(observer).onError(exception);
        verifyNoMoreInteractions(observer);
    }
}