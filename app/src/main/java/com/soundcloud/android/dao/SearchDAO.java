package com.soundcloud.android.dao;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import com.soundcloud.android.model.Search;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;

import java.util.ArrayList;
import java.util.List;

public class SearchDAO extends BaseDAO<Search> {

    protected SearchDAO(ContentResolver contentResolver) {
        super(contentResolver);
    }

    public static Uri insert(ContentResolver resolver, Search search) {
        return resolver.insert(Content.SEARCHES.uri, search.buildContentValues());
    }

    public static List<Search> getHistory(ContentResolver resolver) {
        Cursor cursor = resolver.query(Content.SEARCHES.uri,
                null,
                null,
                null,
                DBHelper.Searches.CREATED_AT + " DESC");

        List<Search> list = new ArrayList<Search>();
        while (cursor != null && cursor.moveToNext()) {
            list.add(new Search(cursor));
        }
        if (cursor != null) cursor.close();
        return list;
    }

    public static void clearState(ContentResolver resolver, long userId) {
        resolver.delete(Content.SEARCHES.uri,
                DBHelper.Searches.USER_ID + " = ?",
                new String[]{String.valueOf(userId)});
    }

    @Override
    public Content getContent() {
        return null;
    }
}
