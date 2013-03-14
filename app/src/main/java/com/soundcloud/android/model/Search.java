package com.soundcloud.android.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import com.soundcloud.android.TempEndpoints;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.api.Request;

public class Search implements ModelLike {
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


    @Override
    public long getId() {
        return id;
    }

    @Override
    public Uri toUri() {
        return Content.SEARCH.forId(id);
    }

    public ContentValues buildContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.Searches.CREATED_AT, System.currentTimeMillis());
        cv.put(DBHelper.Searches.QUERY, query);
        cv.put(DBHelper.Searches.SEARCH_TYPE, search_type);
        return cv;
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
