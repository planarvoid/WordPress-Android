package com.soundcloud.android.users

import android.database.SQLException
import com.soundcloud.android.ApplicationModule
import com.soundcloud.android.storage.SqlBriteDatabase
import com.soundcloud.android.utils.OpenForTesting
import io.reactivex.Scheduler
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@OpenForTesting
@Singleton
class FollowingDatabase
@Inject
internal constructor(databaseOpenHelper: FollowingOpenHelper, @Named(ApplicationModule.RX_HIGH_PRIORITY) scheduler: Scheduler) : SqlBriteDatabase(databaseOpenHelper, scheduler) {

    @Throws(SQLException::class)
    internal fun cleanUp() {
        runInTransaction {
            briteDatabase.writableDatabase.execSQL(FollowingModel.DELETEALL)
        }
    }
}
