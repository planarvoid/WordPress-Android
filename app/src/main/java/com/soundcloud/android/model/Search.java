package com.soundcloud.android.model;

import com.soundcloud.android.TempEndpoints;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class Search {
    static final Content CONTENT  = Content.SEARCHES;

    public static final int ALL = 0;
    public static final int SOUNDS = 1;
    public static final int USERS  = 2;
    public static final int PLAYLISTS = 3;

    public int id;
    public int search_type;
    public String query;
    public long created_at;

    public Search(Cursor cursor) {
        this.id = cursor.getInt(cursor.getColumnIndex(DBHelper.Searches._ID));
        this.search_type = cursor.getInt(cursor.getColumnIndex(DBHelper.Searches.SEARCH_TYPE));
        this.query = cursor.getString(cursor.getColumnIndex(DBHelper.Searches.QUERY));
        this.created_at = cursor.getLong(cursor.getColumnIndex(DBHelper.Searches.CREATED_AT));
    }

    public Search(String query, int searchType) {
        this.query = query;
        this.search_type = searchType;
        this.created_at = this.id = -1;
    }

    public static Search forAll(String query) {
            return new Search(query, ALL);
    }

    public static Search forSounds(String query) {
        return new Search(query, SOUNDS);
    }

    public static Search forPlaylists(String query) {
        return new Search(query, PLAYLISTS);
    }

    public static Search forUsers(String query) {
        return new Search(query, USERS);
    }

    public boolean isEmpty() {
        return TextUtils.isEmpty(query);
    }

    public Request request() {
        String path;
        switch (search_type){
            case USERS:
                path = TempEndpoints.USER_SEARCH;
                break;
            case SOUNDS:
                path = TempEndpoints.TRACK_SEARCH;
                break;
            case PLAYLISTS:
                path = TempEndpoints.PLAYLIST_SEARCH;
                break;
            default:
                path = TempEndpoints.SEARCH;
        }
        return Request.to(path).with("q", query);
    }


    public ContentValues buildContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Searches.CREATED_AT, System.currentTimeMillis());
        cv.put(DBHelper.Searches.QUERY, query);
        cv.put(DBHelper.Searches.SEARCH_TYPE, search_type);
        return cv;
    }

    public Uri insert(ContentResolver resolver) {
        return resolver.insert(CONTENT.uri, buildContentValues());
    }

    public static List<Search> getHistory(ContentResolver resolver) {
        Cursor cursor = resolver.query(CONTENT.uri,
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

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Search that = (Search) o;

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
