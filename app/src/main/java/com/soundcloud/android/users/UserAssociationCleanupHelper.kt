package com.soundcloud.android.users

import com.soundcloud.android.model.Urn
import com.soundcloud.android.storage.DefaultCleanupHelper
import com.soundcloud.android.storage.Tables
import com.soundcloud.propeller.PropellerDatabase
import com.soundcloud.propeller.query.Query
import javax.inject.Inject

class UserAssociationCleanupHelper
@Inject constructor(private val propeller: PropellerDatabase) : DefaultCleanupHelper() {
    override fun getUsersToKeep(): MutableSet<Urn> {
        return propeller.query(Query.from(Tables.UserAssociations.TABLE).select(Tables.UserAssociations.TARGET_ID))
                .map { Urn.forUser(it.getLong(Tables.UserAssociations.TARGET_ID)) }
                .toMutableSet()
    }
}

