package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import io.reactivex.Single;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.List;

class SearchSuggestionStorage {

    private static final String KIND = "kind";
    private static final String ID = "id";
    private static final String TYPE = "type";
    private static final String DISPLAY_TEXT = "display_text";
    private static final String IMAGE_URL = "image_url";
    private static final String KIND_LIKE = "like";
    private static final String KIND_FOLLOWING = "following";
    private static final String SQL_SOUNDS = "SELECT '" +
            KIND_LIKE + "' AS " + KIND + ", " +
            "Sounds._id AS " + ID + ", " +
            "Sounds._type AS " + TYPE + ", " +
            "title AS " + DISPLAY_TEXT + ", " +
            "artwork_url AS " + IMAGE_URL + " " +
            "FROM Likes " +
            "INNER JOIN Sounds ON Likes._id = Sounds._id " +
            "AND Likes._type = Sounds._type " +
            "WHERE " + DISPLAY_TEXT + " LIKE ? " +
            "OR " + DISPLAY_TEXT + " LIKE ?";
    private static final String SQL_LOGGED_IN_USER = "SELECT '" +
            KIND_FOLLOWING + "' AS " + KIND + ", " +
            "Users._id AS " + ID + ", " +
            "0 AS " + TYPE + ", " +
            "username AS " + DISPLAY_TEXT + ", " +
            "avatar_url AS " + IMAGE_URL + " " +
            "FROM Users " +
            "WHERE Users._id = ? " +
            "AND (" + DISPLAY_TEXT + " LIKE ? " +
            "OR " + DISPLAY_TEXT + " LIKE ?)";
    private static final String SQL_USERS = "SELECT '" +
            KIND_FOLLOWING + "' AS " + KIND + ", " +
            "Users._id AS " + ID + ", " +
            "0 AS " + TYPE + ", " +
            "username AS " + DISPLAY_TEXT + ", " +
            "avatar_url AS " + IMAGE_URL + " " +
            "FROM UserAssociations " +
            "INNER JOIN Users ON UserAssociations.target_id = Users._id " +
            "WHERE " + DISPLAY_TEXT + " LIKE ? " +
            "OR " + DISPLAY_TEXT + " LIKE ?";
    private static final String SQL = SQL_SOUNDS + " UNION " + SQL_USERS + " UNION " + SQL_LOGGED_IN_USER;
    private final PropellerRx propellerRx;

    @Inject
    public SearchSuggestionStorage(PropellerDatabase propeller) {
        this.propellerRx = new PropellerRx(propeller);
    }

    public Single<List<SearchSuggestion>> getSuggestions(String searchQuery, Urn loggedInUserUrn, int limit) {
        return RxJava.toV2Single(propellerRx.query(getQuery(), getWhere(searchQuery, loggedInUserUrn)).limit(limit).map(new SearchSuggestionMapper()).toList());
    }

    @NonNull
    private Object[] getWhere(String searchQuery, Urn loggedInUserUrn) {
        return new Object[]{
                searchQuery + "%", "% " + searchQuery + "%", // title
                searchQuery + "%", "% " + searchQuery + "%", // user associations
                loggedInUserUrn.getNumericId(), searchQuery + "%", "% " + searchQuery + "%" // logged in user
        };
    }

    @NonNull
    private String getQuery() {
        return SQL;
    }

    @NonNull
    private static Urn getUrn(CursorReader reader) {
        final long id = reader.getLong(ID);
        if (KIND_FOLLOWING.equals(reader.getString(KIND))) {
            return Urn.forUser(id);
        } else if (reader.getInt(TYPE) == Tables.Sounds.TYPE_TRACK) {
            return Urn.forTrack(id);
        } else {
            return Urn.forPlaylist(id);
        }
    }

    private static class SearchSuggestionMapper extends RxResultMapper<SearchSuggestion> {
        @Override
        public SearchSuggestion map(CursorReader cursorReader) {
            return DatabaseSearchSuggestion.create(getUrn(cursorReader), cursorReader.getString(DISPLAY_TEXT), Optional.fromNullable(cursorReader.getString(IMAGE_URL)));
        }
    }
}
