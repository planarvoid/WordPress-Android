package com.soundcloud.android.users

import android.content.ContentValues
import com.soundcloud.android.model.Urn
import com.soundcloud.android.storage.Tables
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.propeller.ChangeResult
import com.soundcloud.propeller.ContentValuesBuilder
import com.soundcloud.propeller.query.Filter.filter
import com.soundcloud.propeller.query.Query
import com.soundcloud.propeller.rx.PropellerRxV2
import io.reactivex.Maybe
import io.reactivex.Single
import javax.inject.Inject

@OpenForTesting
class UserStorage
@Inject
constructor(private val propeller: PropellerRxV2) {

    fun loadUser(urn: Urn): Maybe<User> {
        return propeller.queryResult(buildUserQuery(urn))
                .filter { queryResult -> !queryResult.isEmpty }
                .map { queryResult -> queryResult.first(User::fromCursorReader) }
                .firstElement()
    }

    fun loadUsers(urns: List<Urn>): Single<List<User>> {
        return propeller.queryResult(buildUsersQuery(urns))
                .map { queryResult -> queryResult.toList(User::fromCursorReader) }
                .firstOrError()
    }

    fun loadUserMap(urns: List<Urn>): Single<Map<Urn, User>> {
        return loadUsers(urns).map { users -> users.associateBy { it.urn() } }
    }

    fun urnForPermalink(permalink: String): Maybe<Urn> {
        require(!permalink.startsWith("/")) { "Permalink must not start with a '/' and must not be a url. Found $permalink" }
        return propeller.queryResult(buildPermalinkQuery(permalink))
                .filter { queryResult -> !queryResult.isEmpty }
                .map { queryResult -> queryResult.first { cursorReader -> Urn.forUser(cursorReader.getLong(Tables.Users._ID)) } }
                .firstElement()
    }

    fun updateFollowersCount(userUrn: Urn, followersCount: Int): Single<ChangeResult> {
        return propeller.update(Tables.Users.TABLE, buildContentValuesForFollowersCount(followersCount), filter().whereEq(Tables.Users._ID, userUrn.numericId)).firstOrError()
    }

    private fun buildPermalinkQuery(identifier: String): Query {
        return Query.from(Tables.Users.TABLE)
                .select(Tables.Users._ID)
                .whereIn(Tables.Users.PERMALINK, identifier)
                .limit(1)
    }

    private fun buildUserQuery(userUrn: Urn): Query = buildUsersQuery(listOf(userUrn))

    private fun buildUsersQuery(userUrns: List<Urn>): Query {
        return Query.from(Tables.Users.TABLE)
                .whereIn(Tables.Users._ID, userUrns.map { it.numericId })
    }

    private fun buildContentValuesForFollowersCount(updatedFollowersCount: Int): ContentValues {
        return ContentValuesBuilder
                .values()
                .put(Tables.Users.FOLLOWERS_COUNT, updatedFollowersCount)
                .get()
    }
}
