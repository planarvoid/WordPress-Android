package com.soundcloud.android.storage;

import rx.Observer;
import rx.functions.Func1;

import android.database.Cursor;

import java.util.Date;

public final class ManagedCursor {

    private static final int TRUE_VALUE = 1;

    private final Cursor cursor;

    ManagedCursor(Cursor cursor) {
        this.cursor = cursor;
    }

    public Cursor getCursor() {
        return cursor;
    }

    public int getResultCount() {
        return cursor.getCount();
    }

    public int getColumnCount() {
        return cursor.getColumnCount();
    }

    public String getString(String column) {
        return cursor.getString(cursor.getColumnIndex(column));
    }

    public int getInt(String column) {
        return cursor.getInt(cursor.getColumnIndex(column));
    }

    public long getLong(String column) {
        return cursor.getLong(cursor.getColumnIndex(column));
    }

    public boolean getBoolean(String column) {
        return getInt(column) == TRUE_VALUE;
    }

    public Date getDateFromTimestamp(String column) {
        return new Date(getLong(column));
    }

    public <T> void emit(Observer<? super T> observer, RowMapper<T> rowFunction) {
        try {
            while (cursor.moveToNext()) {
                observer.onNext(rowFunction.call(this));
            }
            observer.onCompleted();
        } catch (Exception e) {
            observer.onError(e);
        } finally {
            cursor.close();
        }
    }

    public interface RowMapper<T> extends Func1<ManagedCursor, T> {
    }
}
