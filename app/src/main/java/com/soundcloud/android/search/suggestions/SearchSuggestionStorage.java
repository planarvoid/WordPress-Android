package com.soundcloud.android.search.suggestions;

import static com.soundcloud.java.collections.Iterables.filter;
import static com.soundcloud.java.collections.Lists.newArrayList;

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
    private static final String CREATION_DATE = "creation_date";
    private static final String TITLE = "title";
    private static final String KIND_LIKE = "like";
    private static final String KIND_FOLLOWING = "following";
    private static final String KIND_POST = "post";
    private static final String KIND_LIKE_USERNAME = "like_username";
    private static final String SQL_LIKED_SOUNDS = "SELECT " +
            "0 AS result_set, " +
            "'" + KIND_LIKE + "' AS " + KIND + ", " +
            "Sounds._id AS " + ID + ", " +
            "Sounds._type AS " + TYPE + ", " +
            "'' AS " + TITLE + ", " +
            "title AS " + DISPLAY_TEXT + ", " +
            "artwork_url AS " + IMAGE_URL + ", " +
            "Likes.created_at AS " + CREATION_DATE + " " +
            "FROM Likes " +
            "INNER JOIN Sounds ON Likes._id = Sounds._id " +
            "AND Likes._type = Sounds._type " +
            "WHERE " + DISPLAY_TEXT + " LIKE ? " +
            "OR " + DISPLAY_TEXT + " LIKE ?";
    private static final String SQL_USERS = "SELECT " +
            "1 AS result_set, " +
            "'" + KIND_FOLLOWING + "' AS " + KIND + ", " +
            "Users._id AS " + ID + ", " +
            "0 AS " + TYPE + ", " +
            "'' AS " + TITLE + ", " +
            "username AS " + DISPLAY_TEXT + ", " +
            "avatar_url AS " + IMAGE_URL + ", " +
            "UserAssociations.created_at AS " + CREATION_DATE + " " +
            "FROM UserAssociations " +
            "INNER JOIN Users ON UserAssociations.target_id = Users._id " +
            "WHERE " + DISPLAY_TEXT + " LIKE ? " +
            "OR " + DISPLAY_TEXT + " LIKE ?";
    private static final String SQL_LOGGED_IN_USER = "SELECT " +
            "2 AS result_set, " +
            "'" + KIND_FOLLOWING + "' AS " + KIND + ", " +
            "Users._id AS " + ID + ", " +
            "0 AS " + TYPE + ", " +
            "'' AS " + TITLE + ", " +
            "username AS " + DISPLAY_TEXT + ", " +
            "avatar_url AS " + IMAGE_URL + ", " +
            "0 AS " + CREATION_DATE + " " +
            "FROM Users " +
            "WHERE Users._id = ? " +
            "AND (" + DISPLAY_TEXT + " LIKE ? " +
            "OR " + DISPLAY_TEXT + " LIKE ?)";
    private static final String SQL_POSTED_SOUNDS = "SELECT " +
            "3 AS result_set, " +
            "'" + KIND_POST + "' AS " + KIND + ", " +
            "Sounds._id AS " + ID + ", " +
            "Sounds._type AS " + TYPE + ", " +
            "'' AS " + TITLE + ", " +
            "title AS " + DISPLAY_TEXT + ", " +
            "artwork_url AS " + IMAGE_URL + ", " +
            "Posts.created_at AS " + CREATION_DATE + " " +
            "FROM Posts " +
            "INNER JOIN Sounds ON Posts.target_id = Sounds._id " +
            "AND Posts.target_type = Sounds._type " +
            "WHERE Posts.type = '" + Tables.Posts.TYPE_POST + "' " +
            "AND (" + DISPLAY_TEXT + " LIKE ? " +
            "OR " + DISPLAY_TEXT + " LIKE ?)";
    private static final String SQL_LIKED_SOUNDS_BY_USERNAME = "SELECT " +
            "4 AS result_set, " +
            "'" + KIND_LIKE_USERNAME + "' AS " + KIND + ", " +
            "Sounds._id AS " + ID + ", " +
            "Sounds._type AS " + TYPE + ", " +
            "title AS " + TITLE + ", " +
            "(username || ' - ' || title) AS " + DISPLAY_TEXT + ", " +
            "artwork_url AS " + IMAGE_URL + ", " +
            "Likes.created_at AS " + CREATION_DATE + " " +
            "FROM Likes " +
            "INNER JOIN Sounds ON Likes._id = Sounds._id " +
            "AND Likes._type = Sounds._type " +
            "INNER JOIN Users ON Sounds.user_id = Users._id " +
            "WHERE Users.username LIKE ? " +
            "OR Users.username LIKE ?";
    private static final String SQL = SQL_LIKED_SOUNDS +
            " UNION " + SQL_USERS +
            " UNION " + SQL_LOGGED_IN_USER +
            " UNION " + SQL_POSTED_SOUNDS +
            " UNION " + SQL_LIKED_SOUNDS_BY_USERNAME +
            " ORDER BY result_set ASC, " + CREATION_DATE + " DESC";
    private final PropellerRx propellerRx;

    @Inject
    SearchSuggestionStorage(PropellerDatabase propeller) {
        this.propellerRx = new PropellerRx(propeller);
    }

    Single<List<SearchSuggestion>> getSuggestions(String searchQuery, Urn loggedInUserUrn, int limit) {
        return RxJava.toV2Single(propellerRx.query(getQuery(), getWhere(searchQuery, loggedInUserUrn))
                                            .limit(limit)
                                            .map(new DatabaseSearchSuggestionMapper())
                                            .toList()
                                            .map(this::removeDuplicates));
    }

    @NonNull
    private Object[] getWhere(String searchQuery, Urn loggedInUserUrn) {
        return new Object[]{
                searchQuery + "%", "% " + searchQuery + "%", // liked sounds by title
                searchQuery + "%", "% " + searchQuery + "%", // users that are being followed
                loggedInUserUrn.getNumericId(), searchQuery + "%", "% " + searchQuery + "%", // logged in user
                searchQuery + "%", "% " + searchQuery + "%", // posted sounds by title
                searchQuery + "%", "% " + searchQuery + "%" // liked sounds by username
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

    private static class DatabaseSearchSuggestionMapper extends RxResultMapper<DatabaseSearchSuggestion> {
        @Override
        public DatabaseSearchSuggestion map(CursorReader cursorReader) {
            final Optional<String> title = KIND_LIKE_USERNAME.equals(cursorReader.getString(KIND)) ? Optional.of(cursorReader.getString(TITLE)) : Optional.absent();
            return DatabaseSearchSuggestion.create(getUrn(cursorReader),
                                                   cursorReader.getString(DISPLAY_TEXT),
                                                   Optional.fromNullable(cursorReader.getString(IMAGE_URL)),
                                                   title);
        }
    }

    // It's possible that we might have some duplicates, e.g. if the user is searching by artist name and has liked a track or playlist whose title contains the artist name, then there would be two
    // suggestions per entity: one for "track/playlist name" and one for "artist name - track/playlist name". We only want to keep the former.
    private List<SearchSuggestion> removeDuplicates(List<DatabaseSearchSuggestion> items) {
        List<DatabaseSearchSuggestion> result = newArrayList(items);
        final List<DatabaseSearchSuggestion> tracksOrPlaylistsRecommendedBasedOnArtistName = newArrayList(filter(items, item -> item.title().isPresent()));
        for (DatabaseSearchSuggestion item : tracksOrPlaylistsRecommendedBasedOnArtistName) {
            final String title = item.title().get();
            for (SearchSuggestion otherSuggestion : items) {
                if (otherSuggestion.getQuery().equals(title)) {
                    result.remove(item);
                    break;
                }
            }
        }
        return newArrayList(result);
    }
}
