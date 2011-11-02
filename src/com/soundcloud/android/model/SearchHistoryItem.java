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
    }
