package com.soundcloud.android.collection.recentlyplayed

import com.soundcloud.android.collection.playhistory.PlayHistoryRecord
import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.StorageIntegrationTest
import org.junit.Before
import org.junit.Test

class RecentlyPlayedStorageTest : StorageIntegrationTest() {

    private lateinit var storage: RecentlyPlayedStorage

    @Before
    @Throws(Exception::class)
    fun setUp() {
        storage = RecentlyPlayedStorage(propeller(), propellerRxV2())
    }

    @Test
    fun shouldLoadRecentlyPlayedWithoutDuplicationsAndSortedByTimestamp() {

        testFixtures().insertRecentlyPlayed(100, Urn.forPlaylist(1))
        testFixtures().insertRecentlyPlayed(200, Urn.forPlaylist(1))
        testFixtures().insertRecentlyPlayed(300, Urn.forArtistStation(1))
        testFixtures().insertRecentlyPlayed(400, Urn.forArtistStation(1))
        testFixtures().insertRecentlyPlayed(500, Urn.forUser(1))

        storage.loadRecentlyPlayed(100)
                .test()
                .assertValue(listOf(
                        PlayHistoryRecord.forRecentlyPlayed(500, Urn.forUser(1)),
                        PlayHistoryRecord.forRecentlyPlayed(400, Urn.forArtistStation(1)),
                        PlayHistoryRecord.forRecentlyPlayed(200, Urn.forPlaylist(1))
                ))
    }
}
