package com.soundcloud.android.suggestedcreators

import com.soundcloud.android.model.Urn
import com.soundcloud.android.storage.DefaultCleanupHelper
import com.soundcloud.android.storage.Tables
import com.soundcloud.propeller.PropellerDatabase
import com.soundcloud.propeller.query.Query
import javax.inject.Inject

class SuggestedCreatorsCleanupHelper
@Inject constructor(private val propeller: PropellerDatabase) : DefaultCleanupHelper() {

    override fun usersToKeep(): Set<Urn> {
        return propeller.query(Query.from(Tables.SuggestedCreators.TABLE).select(Tables.SuggestedCreators.SEED_USER_ID, Tables.SuggestedCreators.SUGGESTED_USER_ID))
                .flatMap { listOf(Urn.forUser(it.getLong(Tables.SuggestedCreators.SEED_USER_ID)), Urn.forUser(it.getLong(Tables.SuggestedCreators.SUGGESTED_USER_ID))) }
                .toSet()
    }

}
