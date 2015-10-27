package com.soundcloud.android.api.legacy.model;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.search.suggestions.Shortcut;
import com.soundcloud.android.search.suggestions.SuggestionsAdapter;
import com.soundcloud.android.storage.provider.Content;
import org.jetbrains.annotations.NotNull;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Suggestions from the /search/suggest endpoint.
 * <p/>
 * <pre>
 * {
 * "query_urn" : "soundcloud:search-suggest:4aceafa4290a4580bfc9f5d306ffb917",
 * "query_time_in_millis" : 0,
 * "query" : "f",
 * "limit" : 5,
 * "suggestions" : [
 * {
 * "query" : "Foo Fighters",
 * "kind" : "user",
 * "id" : 2097360,
 * "score" : 889523
 * }, ...
 * ]
 * }
 * </pre>
 *
 * @see <a href="https://github.com/soundcloud/search-suggest#setting-up-the-suggest-engine">search-suggest</a>
 */
public class SearchSuggestions implements Iterable<SearchSuggestions.Query> {
    public final static SearchSuggestions EMPTY = new SearchSuggestions();
    public String query_urn;
    public long query_time_in_millis;
    public String query;
    public int limit;
    public @NotNull List<Query> suggestions;

    public SearchSuggestions() {
        suggestions = new ArrayList<>();
    }

    /**
     * Search suggestions from a local cursor, expect data to be in standard android suggest
     * format
     *
     * @param cursor cursor with data scheme compatible with
     *               <a href="http://developer.android.com/guide/topics/search/adding-custom-suggestions.html#SuggestionTable">
     *               SuggestionTable</a>
     */
    public SearchSuggestions(List<Shortcut> shortcuts) {
        suggestions = new ArrayList<>(shortcuts.size());
        fromLocalShortcuts(shortcuts);
    }

    public void putRemoteIds(List<Long> missingTracks, List<Long> missingUsers, List<Long> missingPlaylists) {
        for (Query q : this) {
            if (q.isRemote()) {
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
        int searchQueryIndex = 0;
        final MatrixCursor cursor = new MatrixCursor(SuggestionsAdapter.COLUMN_NAMES);

        for (SearchSuggestions.Query q : this) {
            if (!Query.SUPPORTED_KINDS.contains(q.kind)) {
                continue;
            }
            addQueryToCursor(cursor, q, searchQueryIndex);

            if(q.isRemote()) {
                searchQueryIndex = searchQueryIndex + 1;
            }
        }
        return cursor;
    }

    private void addQueryToCursor(MatrixCursor cursor, Query q, int searchQueryIndex) {
        boolean isLocal = q.isLocal();

        cursor.addRow(new Object[]{
                -1,                                 // suggestion id
                q.id,                               // id
                q.query,                            // SUGGEST_COLUMN_TEXT_1
                q.getIntentData(),                  // SUGGEST_COLUMN_INTENT_DATA
                null,                               // this is not used anymore, we can remove this column
                isLocal ? 1 : 0,                    // local
                buildHighlightData(q),              // highlight
                isLocal ? null : query_urn,         // Set query_urn only on remote suggestions
                isLocal ? -1 : searchQueryIndex     // Set query_position only on remote suggestions
        });
    }

    public void add(Query q) {
        suggestions.add(q);
    }

    public boolean contains(Query q) {
        return suggestions.contains(q);
    }

    public SearchSuggestions mergeWithRemote(SearchSuggestions remoteSuggestions) {
        SearchSuggestions merged = new SearchSuggestions();

        if (remoteSuggestions.query_urn != null) {
            merged.query_urn = remoteSuggestions.query_urn;
        }

        for (Query q : this) {
            merged.add(q);
        }
        for (Query q : remoteSuggestions) {
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

    @Override
    public String toString() {
        return "SearchSuggestions{" +
                "query_time_in_millis=" + query_time_in_millis +
                ", query='" + query + '\'' +
                ", limit=" + limit +
                ", suggestions=" + suggestions +
                ", query_urn=" + query_urn +
                '}';
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
            highlightData
                    .append(highlight.get("pre"))
                    .append(',')
                    .append(highlight.get("post"));
            if (iterator.hasNext()) {
                highlightData.append(';');
            }
        }
        return highlightData.toString();
    }

    private void fromLocalShortcuts(List<Shortcut> shortcuts) {

        for (Shortcut shortcut : shortcuts){
            final Urn urn = shortcut.getUrn();
            Query q = new Query();
            q.id = urn.getNumericId();
            q.query = shortcut.getDisplayText();
            q.intentData = Query.getDataUri(urn).toString();
            q.kind = Query.kindFromUrn(urn);
            q.query_urn = query_urn;
            q.query_position = -1;
            suggestions.add(q);
        }
    }

    public static class Query {

        public static final String KIND_USER = "user";
        public static final String KIND_TRACK = "track";
        public static final String KIND_PLAYLIST = "playlist"; //TODO: check if it will be called that way
        public static final Set<String> SUPPORTED_KINDS = new HashSet<>();

        // Search suggest API fields
        public String query;
        public String kind;
        public long id;
        public long score;
        public List<Map<String, Integer>> highlights;
        public String query_urn;
        public int query_position;

        static {
            SUPPORTED_KINDS.add(KIND_USER);
            SUPPORTED_KINDS.add(KIND_TRACK);
            //TODO: SUPPORTED_KINDS.add(KIND_PLAYLIST);
        }

        // internal fields
        private String iconUri;
        private String intentData;

        public String getIntentData() {
            if (intentData != null) {
                return intentData;
            }
            return contentProviderUri().toString();
        }

        static String kindFromUrn(Urn urn) {
            if (urn.isUser()) {
                return KIND_USER;
            } else if (urn.isTrack()) {
                return KIND_TRACK;
            } else if (urn.isPlaylist()) {
                return KIND_PLAYLIST;
            } else {
                throw new IllegalStateException("Unknown urn type " + urn);
            }
        }

        static Uri getDataUri(Urn urn) {
            if (urn.isUser()) {
                return Content.USER.forId(urn.getNumericId());
            } else if (urn.isTrack() || urn.isPlaylist()) {
                return Content.TRACK.forId(urn.getNumericId());
            } else {
                throw new IllegalStateException("Unknown urn type " + urn);
            }
        }

        public Urn getUrn() {
            return new Urn(Urn.SOUNDCLOUD_SCHEME + ":" + kind + "s:" + id);
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
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
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

        public boolean isRemote() {
            return score != 0;
        }

        private Uri contentProviderUri() {
            final Urn urn = getUrn();
            if (urn.isTrack()) {
                return Content.TRACK.forId(urn.getNumericId());
            } else if (urn.isUser()) {
                return Content.USER.forId(urn.getNumericId());
            } else if (urn.isPlaylist()) {
                return Content.PLAYLIST.forId(urn.getNumericId());
            } else {
                throw new IllegalStateException("Can't convert to content Uri: " + urn);
            }
        }
    }
}
