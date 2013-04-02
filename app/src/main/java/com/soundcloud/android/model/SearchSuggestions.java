package com.soundcloud.android.model;

import com.soundcloud.android.adapter.SuggestionsAdapter;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import org.jetbrains.annotations.NotNull;

import android.app.SearchManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 Suggestions from the /search/suggest endpoint.

 <pre>
   {
     "tx_id" : "92dbb484c0d144afa6c193ece99514f3",
     "query_time_in_millis" : 0,
     "query" : "f",
     "limit" : 5,
     "suggestions" : [
        {
         "query" : "Foo Fighters",
         "kind" : "user",
         "id" : 2097360,
         "score" : 889523
        }, ...
     ]
   }
 </pre>

 @see <a href="https://github.com/soundcloud/search-suggest#setting-up-the-suggest-engine">search-suggest</a>
 */
public class SearchSuggestions implements Iterable<SearchSuggestions.Query> {
    public String tx_id;
    public long query_time_in_millis;
    public String query;
    public int limit;

    public @NotNull List<Query> suggestions;

    public final static SearchSuggestions EMPTY = new SearchSuggestions();

    public SearchSuggestions() {
        suggestions = new ArrayList<Query>();
    }

    /**
     * Search suggestions from a local cursor, expect data to be in standard android suggest
     * format
     * @param cursor cursor with data scheme compatible with
     *   <a href="http://developer.android.com/guide/topics/search/adding-custom-suggestions.html#SuggestionTable">
     *  SuggestionTable</a>
     */
    public SearchSuggestions(Cursor cursor) {
        suggestions = new ArrayList<Query>(cursor.getCount());
        fromLocalCursor(cursor);
    }

    public void putRemoteIds(List<Long> missingTracks, List<Long> missingUsers, List<Long> missingPlaylists) {
        for (Query q : this) {
            if (!q.isLocal()) {
                if (Query.KIND_USER.equals(q.kind)) {
                    missingUsers.add(q.id);
                } else if (Query.KIND_TRACK.equals(q.kind)) {
                    missingTracks.add(q.id);
                } else if (Query.KIND_PLAYLIST.equals(q.kind)) {
                    missingPlaylists.add(q.id);
                }
            }
        }
    }

    public Cursor asCursor() {
        final MatrixCursor cursor = new MatrixCursor(SuggestionsAdapter.COLUMN_NAMES);
        for (SearchSuggestions.Query q : this) {
            if (!Query.SUPPORTED_KINDS.contains(q.kind)) continue;

            cursor.addRow(new Object[] {
                    -1,                // suggestion id
                    q.id,              // id
                    q.query,           // SUGGEST_COLUMN_TEXT_1
                    q.getIntentData(), // SUGGEST_COLUMN_INTENT_DATA
                    q.getIconUri(),    //
                    q.isLocal() ? 1 : 0,
                    buildHighlightData(q)
            });
        }
        return cursor;
    }

    //FIXME: this is a wild hack, but we need to pipe the highlight data through the cursor somehow.
    //I don't think SuggestionsAdapter has to be a CursorAdapter to begin with, but should operate directly
    //on SearchSuggestions
    private String buildHighlightData(Query q) {
        if (q.highlights == null || q.highlights.isEmpty()) {
            return null;
        }

        StringBuilder highlightData = new StringBuilder();
        Iterator<Map<String, Integer>> iterator = q.highlights.iterator();
        while (iterator.hasNext()) {
            Map<String, Integer> highlight = iterator.next();
            highlightData.append(highlight.get("pre") + "," + highlight.get("post"));
            if (iterator.hasNext()) highlightData.append(";");
        }
        return highlightData.toString();
    }

    public void add(Query q) {
        suggestions.add(q);
    }

    public boolean contains(Query q) {
        return suggestions.contains(q);
    }

    public SearchSuggestions merge(SearchSuggestions other) {
        SearchSuggestions merged = new SearchSuggestions();
        for (Query q : this) {
            merged.add(q);
        }
        for (Query q : other) {
            if (!merged.contains(q)) {
                merged.add(q);
            }
        }
        return merged;
    }

    @Override
    public Iterator<Query> iterator() {
        return suggestions.iterator();
    }

    public int size() {
        return suggestions.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    private void fromLocalCursor(Cursor cursor) {
        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndex(DBHelper.Suggestions.ID));
            String query = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1));
            String intentData = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_INTENT_DATA));
            String iconUrl = cursor.getString(cursor.getColumnIndex(DBHelper.Suggestions.ICON_URL));
            Query q = new Query();
            q.id = id;
            q.query = query;
            q.iconUri = iconUrl;
            q.intentData = intentData;
            q.kind = Query.kindFromContentUri(Uri.parse(intentData));
            suggestions.add(q);
        }
    }

    public static class Query {

        public static final String KIND_USER = "user";
        public static final String KIND_TRACK = "track";
        public static final String KIND_PLAYLIST = "playlist"; //TODO: check if it will be called that way
        public static final Set<String> SUPPORTED_KINDS = new HashSet<String>();

        static {
            SUPPORTED_KINDS.add(KIND_USER);
            SUPPORTED_KINDS.add(KIND_TRACK);
            //TODO: SUPPORTED_KINDS.add(KIND_PLAYLIST);
        }

        // Search suggest API fields
        public String query;
        public String kind;
        public long   id;
        public long   score;
        public List<Map<String, Integer>> highlights;

        // internal fields
        private String iconUri;
        private String intentData;

        public static String kindFromContentUri(Uri uri) {
            switch (Content.match(uri)) {
                case TRACK: case TRACKS: return KIND_TRACK;
                case USER:  case USERS: return KIND_USER;
                case PLAYLIST: case PLAYLISTS: return KIND_PLAYLIST;
                default:
                    throw new IllegalStateException("Unsupported content URI: " + uri);
            }
        }

        public String getIconUri() {
            if (iconUri != null) return iconUri;
            return getClientUri().imageUri().toString();
        }

        public String getIntentData() {
            if (intentData != null) return intentData;
            return getClientUri().contentProviderUri().toString();
        }

        public ClientUri getClientUri() {
            return new ClientUri("soundcloud:" + kind + "s:" + id);
        }

        @Override
        public String toString() {
            return "Query{" +
                    "query='" + query + '\'' +
                    ", kind='" + kind + '\'' +
                    ", id=" + id +
                    ", score=" + score +
                    ", iconUri=" + iconUri +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Query query = (Query) o;
            return id == query.id && !(kind != null ? !kind.equals(query.kind) : query.kind != null);
        }

        @Override
        public int hashCode() {
            int result = kind != null ? kind.hashCode() : 0;
            result = 31 * result + (int) (id ^ (id >>> 32));
            return result;
        }

        public boolean isLocal() {
            return score == 0;
        }
    }

    @Override
    public String toString() {
        return "SearchSuggestions{" +
                "query_time_in_millis=" + query_time_in_millis +
                ", query='" + query + '\'' +
                ", limit=" + limit +
                ", suggestions=" + suggestions +
                '}';
    }
}
