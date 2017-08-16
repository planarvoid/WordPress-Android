package com.soundcloud.android.search.suggestions

import com.soundcloud.android.model.Urn
import com.soundcloud.android.rx.RxJava
import com.soundcloud.android.storage.Tables
import com.soundcloud.java.optional.Optional
import com.soundcloud.propeller.CursorReader
import com.soundcloud.propeller.rx.PropellerRx
import com.soundcloud.propeller.rx.RxResultMapper
import io.reactivex.Single
import javax.inject.Inject


open class SearchSuggestionStorage
@Inject
constructor(private val propeller: PropellerRx) {

    private val KIND = "kind"
    private val ID = "id"
    private val TYPE = "type"
    private val DISPLAY_TEXT = "display_text"
    private val IMAGE_URL = "image_url"
    private val CREATION_DATE = "creation_date"

    private val KIND_LIKE = DatabaseSearchSuggestion.Kind.Like.name
    private val KIND_FOLLOWING = DatabaseSearchSuggestion.Kind.Following.name
    private val KIND_POST = DatabaseSearchSuggestion.Kind.Post.name
    private val KIND_LIKE_USERNAME = DatabaseSearchSuggestion.Kind.LikeByUsername.name

    private val SQL_LIKED_SOUNDS = "SELECT " +
            "0 AS result_set, " +
            "'$KIND_LIKE' AS $KIND, " +
            "Sounds._id AS $ID, " +
            "Sounds._type AS $TYPE, " +
            "title AS $DISPLAY_TEXT, " +
            "artwork_url AS $IMAGE_URL, " +
            "Likes.created_at AS $CREATION_DATE " +
            "FROM Likes " +
            "INNER JOIN Sounds ON Likes._id = Sounds._id " +
            "AND Likes._type = Sounds._type " +
            "WHERE $DISPLAY_TEXT LIKE ? " +
            "OR $DISPLAY_TEXT LIKE ?"
    private val SQL_USERS = "SELECT " +
            "1 AS result_set, " +
            "'$KIND_FOLLOWING' AS $KIND, " +
            "Users._id AS $ID, " +
            "0 AS $TYPE , " +
            "username AS $DISPLAY_TEXT, " +
            "avatar_url AS $IMAGE_URL, " +
            "UserAssociations.created_at AS $CREATION_DATE " +
            "FROM UserAssociations " +
            "INNER JOIN Users ON UserAssociations.target_id = Users._id " +
            "WHERE $DISPLAY_TEXT LIKE ? " +
            "OR $DISPLAY_TEXT LIKE ?"
    private val SQL_LOGGED_IN_USER = "SELECT " +
            "2 AS result_set, " +
            "'$KIND_FOLLOWING' AS $KIND, " +
            "Users._id AS $ID, " +
            "0 AS $TYPE, " +
            "username AS $DISPLAY_TEXT, " +
            "avatar_url AS $IMAGE_URL, " +
            "0 AS $CREATION_DATE " +
            "FROM Users " +
            "WHERE Users._id = ? " +
            "AND ($DISPLAY_TEXT LIKE ? " +
            "OR $DISPLAY_TEXT LIKE ?)"
    private val SQL_POSTED_SOUNDS = "SELECT " +
            "3 AS result_set, " +
            "'$KIND_POST' AS $KIND, " +
            "Sounds._id AS $ID, " +
            "Sounds._type AS $TYPE, " +
            "title AS $DISPLAY_TEXT, " +
            "artwork_url AS $IMAGE_URL, " +
            "Posts.created_at AS $CREATION_DATE " +
            "FROM Posts " +
            "INNER JOIN Sounds ON Posts.target_id = Sounds._id " +
            "AND Posts.target_type = Sounds._type " +
            "WHERE Posts.type = '${Tables.Posts.TYPE_POST}' " +
            "AND ($DISPLAY_TEXT LIKE ? " +
            "OR $DISPLAY_TEXT LIKE ?)"
    private val SQL_LIKED_SOUNDS_BY_USERNAME = "SELECT " +
            "4 AS result_set, " +
            "'$KIND_LIKE_USERNAME' AS $KIND, " +
            "Sounds._id AS $ID, " +
            "Sounds._type AS $TYPE, " +
            "(username || ' - ' || title) AS $DISPLAY_TEXT, " +
            "artwork_url AS $IMAGE_URL, " +
            "Likes.created_at AS $CREATION_DATE " +
            "FROM Likes " +
            "INNER JOIN Sounds ON Likes._id = Sounds._id " +
            "AND Likes._type = Sounds._type " +
            "INNER JOIN Users ON Sounds.user_id = Users._id " +
            "WHERE Users.username LIKE ? " +
            "OR Users.username LIKE ?"
    private val SQL = "$SQL_LIKED_SOUNDS " +
            "UNION $SQL_USERS " +
            "UNION $SQL_LOGGED_IN_USER " +
            "UNION $SQL_POSTED_SOUNDS " +
            "UNION $SQL_LIKED_SOUNDS_BY_USERNAME " +
            "ORDER BY result_set ASC, $CREATION_DATE DESC"


    open fun getSuggestions(searchQuery: String, loggedInUserUrn: Urn, limit: Int): Single<List<SearchSuggestion>> {
        return RxJava.toV2Single(propeller.query(SQL, *getWhere(searchQuery, loggedInUserUrn))
                .limit(limit)
                .map(DatabaseSearchSuggestionMapper())
                .toList()
                .map { this.removeDuplicates(it) })
    }

    private fun getWhere(searchQuery: String, loggedInUserUrn: Urn): Array<Any> {
        return arrayOf(
                "$searchQuery%", "% $searchQuery%", // liked sounds by title
                "$searchQuery%", "% $searchQuery%", // users that are being followed
                loggedInUserUrn.numericId, "$searchQuery%", "% $searchQuery%", // logged in user
                "$searchQuery%", "% $searchQuery%", // posted sounds by title
                "$searchQuery%", "% $searchQuery%" // liked sounds by username
        )
    }

    private fun getUrn(reader: CursorReader): Urn {
        val id = reader.getLong(ID)
        return when {
            KIND_FOLLOWING == reader.getString(KIND) -> Urn.forUser(id)
            reader.getInt(TYPE) == Tables.Sounds.TYPE_TRACK -> Urn.forTrack(id)
            else -> Urn.forPlaylist(id)
        }
    }

    private fun removeDuplicates(items: List<DatabaseSearchSuggestion>): List<SearchSuggestion> {
        val duplicatesToRemove = items.groupBy { it.urn }.filter { it.value.size > 1 }.values.flatten().filter { DatabaseSearchSuggestion.Kind.LikeByUsername == it.kind() }
        return items.filter { !duplicatesToRemove.contains(it) }
    }

    private inner class DatabaseSearchSuggestionMapper : RxResultMapper<DatabaseSearchSuggestion>() {
        override fun map(cursorReader: CursorReader): DatabaseSearchSuggestion {
            return DatabaseSearchSuggestion.create(getUrn(cursorReader),
                    cursorReader.getString(DISPLAY_TEXT),
                    Optional.fromNullable(cursorReader.getString(IMAGE_URL)),
                    DatabaseSearchSuggestion.Kind.valueOf(cursorReader.getString(KIND)))
        }
    }
}
