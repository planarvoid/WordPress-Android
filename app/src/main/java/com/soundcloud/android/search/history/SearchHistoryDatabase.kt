package com.soundcloud.android.search.history

import com.soundcloud.android.ApplicationModule
import com.soundcloud.android.storage.SqlBriteDatabase
import io.reactivex.Scheduler

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class SearchHistoryDatabase
@Inject
constructor(databaseOpenHelper: SearchHistoryDatabaseOpenHelper, @Named(ApplicationModule.RX_HIGH_PRIORITY) scheduler: Scheduler) : SqlBriteDatabase(databaseOpenHelper, scheduler)
