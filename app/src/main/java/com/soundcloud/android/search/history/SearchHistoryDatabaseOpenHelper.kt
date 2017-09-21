package com.soundcloud.android.search.history

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

import javax.inject.Inject

internal class SearchHistoryDatabaseOpenHelper
@Inject
constructor(context: Context) : SQLiteOpenHelper(context, "search_history.db", null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SearchHistoryDbModel.CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    companion object {
        private val DATABASE_VERSION = 1
    }
}
