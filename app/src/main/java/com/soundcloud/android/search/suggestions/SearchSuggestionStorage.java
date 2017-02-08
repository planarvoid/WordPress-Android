package com.soundcloud.android.search.suggestions;

import static com.soundcloud.propeller.query.Query.from;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.storage.Tables.SearchSuggestions;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.List;

class SearchSuggestionStorage {

    private final PropellerRx propellerRx;

    @Inject
    public SearchSuggestionStorage(PropellerDatabase propeller) {
        this.propellerRx = new PropellerRx(propeller);
    }

    public Observable<List<SearchSuggestion>> getSuggestions(String searchQuery, int limit) {
        return propellerRx.query(getQuery(searchQuery, limit)).map(new SearchSuggestionMapper()).toList();
    }

    @NonNull
    private Query getQuery(String searchQuery, int limit) {
        return from(SearchSuggestions.TABLE)
                .select(SearchSuggestions.KIND,
                        SearchSuggestions._ID,
                        SearchSuggestions._TYPE,
                        SearchSuggestions.DISPLAY_TEXT,
                        SearchSuggestions.IMAGE_URL)
                .where(SearchSuggestions.DISPLAY_TEXT + " LIKE '" + searchQuery + "%' or "
                               + SearchSuggestions.DISPLAY_TEXT + " LIKE '% " + searchQuery + "%'")
                .limit(limit);
    }

    @NonNull
    private static Urn getUrn(CursorReader reader) {
        final long id = reader.getLong(SearchSuggestions._ID);
        if (SearchSuggestions.KIND_FOLLOWING.equals(reader.getString(SearchSuggestions.KIND))) {
            return Urn.forUser(id);
        } else if (reader.getInt(SearchSuggestions._TYPE) == Tables.Sounds.TYPE_TRACK) {
            return Urn.forTrack(id);
        } else {
            return Urn.forPlaylist(id);
        }
    }

    private static class SearchSuggestionMapper extends RxResultMapper<SearchSuggestion> {
        @Override
        public SearchSuggestion map(CursorReader cursorReader) {
            return DatabaseSearchSuggestion.create(getUrn(cursorReader), cursorReader.getString(SearchSuggestions.DISPLAY_TEXT), Optional.fromNullable(cursorReader.getString(SearchSuggestions.IMAGE_URL)));
        }
    }
}
