package com.soundcloud.android.users

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

import javax.inject.Inject

internal class FollowingOpenHelper @Inject
constructor(context: Context) : SQLiteOpenHelper(context, "following.db", null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(FollowingModel.CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    companion object {
        private val DATABASE_VERSION = 1
    }
}
