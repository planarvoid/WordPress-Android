package com.soundcloud.android.storage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestCursor extends com.xtremelabs.robolectric.tester.android.database.TestCursor {

    protected int count;
    protected Object[][] results;
    protected List<String> columnNames = Collections.emptyList();
    protected int rowIndex = -1;
    private boolean closed;

    TestCursor(int count) {
        this.count = count;
        this.results = new Object[count][];
    }

    TestCursor(Object[][] fakeResults, String... columnNames) {
        this.count = fakeResults.length;
        this.results = fakeResults;
        this.columnNames = Arrays.asList(columnNames);
    }

    @Override
    public int getColumnIndex(String columnName) {
        return columnNames.indexOf(columnName);
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnNames.get(columnIndex);
    }

    @Override
    public int getColumnCount() {
        return columnNames.size();
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public int getPosition() {
        return rowIndex;
    }

    @Override
    public boolean moveToPosition(int position) {
        if (position < count) {
            this.rowIndex = position;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean moveToNext() {
        return moveToPosition(rowIndex + 1);
    }

    @Override
    public String getString(int columnIndex) {
        return (String) results[rowIndex][columnIndex];
    }

    @Override
    public long getLong(int columnIndex) {
        return (Long) results[rowIndex][columnIndex];
    }

    @Override
    public int getInt(int columnIndex) {
        return (Integer) results[rowIndex][columnIndex];
    }

    @Override
    public void close() {
        this.closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }
}
