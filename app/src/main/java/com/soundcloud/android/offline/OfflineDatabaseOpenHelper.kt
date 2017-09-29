package com.soundcloud.android.offline

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

import javax.inject.Inject

class OfflineDatabaseOpenHelper
@Inject
constructor(context: Context) : SQLiteOpenHelper(context, "offline.db", null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(TrackDownloadsModel.CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    companion object {
        private val DATABASE_VERSION = 1
    }
}
