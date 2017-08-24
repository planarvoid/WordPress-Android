package com.soundcloud.android.users

import com.soundcloud.android.model.Urn
import com.soundcloud.android.storage.Tables
import com.soundcloud.android.utils.OpenForTesting
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

    fun urnForPermalink(permalink: String): Maybe<Urn> {
        require(!permalink.startsWith("/")) { "Permalink must not start with a '/' and must not be a url. Found $permalink" }
        return propeller.queryResult(buildPermalinkQuery(permalink))
                .filter { queryResult -> !queryResult.isEmpty }
                .map { queryResult -> queryResult.first { cursorReader -> Urn.forUser(cursorReader.getLong(Tables.UsersView.ID)) } }
                .firstElement()
    }

    private fun buildPermalinkQuery(identifier: String): Query {
        return Query.from(Tables.UsersView.TABLE)
                .select(Tables.UsersView.ID)
                .whereIn(Tables.UsersView.PERMALINK, identifier)
                .limit(1)
    }

    private fun buildUserQuery(userUrn: Urn): Query {
        return buildUsersQuery(listOf(userUrn))
    }

    private fun buildUsersQuery(userUrns: List<Urn>): Query {
        return Query.from(Tables.UsersView.TABLE)
                .whereIn(Tables.UsersView.ID, userUrns.map { it.numericId })
    }
}
