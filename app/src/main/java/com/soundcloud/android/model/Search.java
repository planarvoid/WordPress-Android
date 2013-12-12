package com.soundcloud.android.model;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.TempEndpoints;
import com.soundcloud.api.Request;

import android.text.TextUtils;

public class Search {
    public static final int ALL = 0;
    public static final int TRACKS = 1;
    public static final int USERS  = 2;
    public static final int PLAYLISTS = 3;

    public int search_type;
    public String query;
    public long created_at;

    public Search(String query, int searchType) {
        this.query = query;
        this.search_type = searchType;
        this.created_at = -1;
    }

    public static Search forAll(String query) {
            return new Search(query, ALL);
    }

    public static Search forTracks(String query) {
        return new Search(query, TRACKS);
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
            case TRACKS:
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

    public Screen getScreen(){
        switch (search_type){
            case USERS:
                return Screen.SEARCH_USERS;
            case TRACKS:
                return Screen.SEARCH_TRACKS;
            case PLAYLISTS:
                return Screen.SEARCH_PLAYLISTS;
            default:
                return Screen.SEARCH_EVERYTHING;
        }
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
