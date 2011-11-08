package com.soundcloud.android.model;

import android.database.Cursor;
import com.soundcloud.android.provider.DatabaseHelper;

public class SearchHistoryItem {
        public int id;
        public int search_type;
        public String query;
        public long created_at;

        public SearchHistoryItem() {
            super();
        }

        public SearchHistoryItem(Cursor cursor) {
            super();
            this.id = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.Searches._ID));
            this.search_type = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.Searches.SEARCH_TYPE));
            this.query = cursor.getString(cursor.getColumnIndex(DatabaseHelper.Searches.QUERY));
            this.created_at = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.Searches.CREATED_AT));
        }

    public SearchHistoryItem(String query, int search_type) {
        this.query = query;
        this.search_type = search_type;
        this.created_at = this.id = -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SearchHistoryItem that = (SearchHistoryItem) o;

        if (search_type != that.search_type) return false;
        if (query != null ? !query.equals(that.query) : that.query != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = search_type;
        result = 31 * result + (query != null ? query.hashCode() : 0);
        return result;
    }
}
