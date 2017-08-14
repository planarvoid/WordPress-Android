package com.soundcloud.android.collection.recentlyplayed

import android.content.ContentValues
import com.soundcloud.android.collection.CollectionDatabase
import com.soundcloud.android.collection.CollectionDatabaseOpenHelper
import com.soundcloud.android.collection.DbModel.RecentlyPlayed
import com.soundcloud.android.collection.playhistory.PlayHistoryRecord
import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.AndroidUnitTest
import io.reactivex.schedulers.Schedulers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class RecentlyPlayedStorageTest : AndroidUnitTest() {

    private lateinit var storage: RecentlyPlayedStorage
    private lateinit var dbHelper: CollectionDatabaseOpenHelper

    val userUrn = Urn.forUser(1)
    val playlistUrn = Urn.forPlaylist(1)
    val artistStationUrn = Urn.forArtistStation(1)

    val playlistPlayHistoryRecord = PlayHistoryRecord.forRecentlyPlayed(100, playlistUrn)
    val artistStationPlayHistoryRecord = PlayHistoryRecord.forRecentlyPlayed(200, artistStationUrn)
    val userPlayHistoryRecord = PlayHistoryRecord.forRecentlyPlayed(300, userUrn)

    @Before
    fun setUp() {
        dbHelper = CollectionDatabaseOpenHelper(RuntimeEnvironment.application)
        val collectionDatabase = CollectionDatabase(dbHelper, Schedulers.trampoline())
        storage = RecentlyPlayedStorage(collectionDatabase)
    }

    @Test
    fun shouldLoadRecentlyPlayedWithoutDuplicationsAndSortedByTimestamp() {
        insertRecentlyPlayed(50, playlistUrn)
        insertRecentlyPlayed(100, playlistUrn)
        insertRecentlyPlayed(100, artistStationUrn)
        insertRecentlyPlayed(200, artistStationUrn)
        insertRecentlyPlayed(300, userUrn)

        storage.loadRecentlyPlayed(100)
                .test()
                .assertValue(listOf(
                        userPlayHistoryRecord,
                        artistStationPlayHistoryRecord,
                        playlistPlayHistoryRecord
                ))
    }

    @Test
    fun shouldProperlyInsertItems() {
        storage.insertRecentlyPlayed(listOf(playlistPlayHistoryRecord, userPlayHistoryRecord, artistStationPlayHistoryRecord))

        val rowsCount = dbRowsCount();
        assertThat(rowsCount).isEqualTo(3)
    }

    @Test
    fun shouldProperlyDeleteItems() {
        insertRecentlyPlayed(100, playlistUrn)
        insertRecentlyPlayed(200, artistStationUrn)

        storage.removeRecentlyPlayed(listOf(playlistPlayHistoryRecord))

        val rows = storage.loadAll()
        assertThat(rows.size).isEqualTo(1)
        assertThat(rows.first().context_id()).isEqualTo(artistStationPlayHistoryRecord.contextUrn().numericId)
        assertThat(rows.first().context_type()).isEqualTo(artistStationPlayHistoryRecord.contextType.toLong())
    }

    @Test
    fun shouldProperlyMarkItemAsSynced() {
        insertRecentlyPlayed(500, userUrn, false)

        storage.markAsSynced(listOf(PlayHistoryRecord.forRecentlyPlayed(500, userUrn)))

        val isSynced = storage.loadAll().first().synced()
        assertThat(isSynced).isTrue()
    }

    @Test
    fun hasPendingContextShouldReturnTrueWhenThereAreUnSyncedItems() {
        insertRecentlyPlayed(100, playlistUrn)
        insertRecentlyPlayed(200, artistStationUrn)
        insertRecentlyPlayed(300, userUrn, false)

        val hasPendingContextToSync = storage.hasPendingContextsToSync()

        assertThat(hasPendingContextToSync).isTrue()
    }

    @Test
    fun hasPendingContextShouldReturnFalseWhenThereNoAreUnSyncedItems() {
        insertRecentlyPlayed(100, playlistUrn)
        insertRecentlyPlayed(200, artistStationUrn)
        insertRecentlyPlayed(300, userUrn)

        val hasPendingContextToSync = storage.hasPendingContextsToSync()

        assertThat(hasPendingContextToSync).isFalse()
    }

    @Test
    fun shouldProperlyClearTheDatabase() {
        insertRecentlyPlayed(100, playlistUrn)
        insertRecentlyPlayed(200, userUrn)

        storage.clear()

        val rowsCount = dbRowsCount()
        assertThat(rowsCount).isEqualTo(0)
    }

    @Test
    fun shouldTrimTableToLimit() {
        insertRecentlyPlayed(100, playlistUrn)
        insertRecentlyPlayed(200, artistStationUrn)
        insertRecentlyPlayed(300, userUrn)

        storage.trim(1)

        val rowsCount = dbRowsCount()
        assertThat(rowsCount).isEqualTo(1)
    }

    private fun dbRowsCount() = storage.loadAll().size

    private fun insertRecentlyPlayed(timestamp: Long, urn: Urn, synced: Boolean = true) {
        val record = PlayHistoryRecord.create(timestamp, Urn.NOT_SET, urn)
        val cv = ContentValues().apply {
            put(RecentlyPlayed.TIMESTAMP, record.timestamp())
            put(RecentlyPlayed.CONTEXT_TYPE, record.contextType)
            put(RecentlyPlayed.CONTEXT_ID, record.contextUrn().numericId)
            put(RecentlyPlayed.SYNCED, if (synced) 1 else 0)
        }
        dbHelper.writableDatabase.insert(RecentlyPlayed.TABLE_NAME, RecentlyPlayed.SYNCED, cv)
    }

}
