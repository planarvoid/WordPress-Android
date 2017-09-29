package com.soundcloud.android.offline

import com.soundcloud.android.ApplicationModule
import com.soundcloud.android.storage.SqlBriteDatabase
import io.reactivex.Scheduler

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class OfflineDatabase
@Inject
constructor(databaseOpenHelper: OfflineDatabaseOpenHelper, @Named(ApplicationModule.RX_HIGH_PRIORITY) scheduler: Scheduler) : SqlBriteDatabase(databaseOpenHelper, scheduler)
