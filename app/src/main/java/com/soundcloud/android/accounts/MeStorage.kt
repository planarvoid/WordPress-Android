package com.soundcloud.android.accounts

import com.soundcloud.android.commands.StoreUsersCommand
import com.soundcloud.android.storage.Tables
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.propeller.PropellerDatabase
import javax.inject.Inject

@OpenForTesting
class MeStorage
@Inject
constructor(private val propeller: PropellerDatabase) {
    fun store(me: Me) {
        val contentValues = StoreUsersCommand.buildUserContentValues(me.user)
        contentValues.put(Tables.Users.PRIMARY_EMAIL_CONFIRMED.name(), me.isPrimaryEmailConfirmed)
        propeller.insert(Tables.Users.TABLE, contentValues)
    }
}
