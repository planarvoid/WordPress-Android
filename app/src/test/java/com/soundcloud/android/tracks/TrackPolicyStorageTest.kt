package com.soundcloud.android.tracks

import com.soundcloud.android.testsupport.StorageIntegrationTest
import com.soundcloud.java.collections.Sets
import org.junit.Before
import org.junit.Test
import java.util.Date

class TrackPolicyStorageTest : StorageIntegrationTest() {

    private lateinit var trackPolicyStorage: TrackPolicyStorage

    @Before
    fun setup() {
        trackPolicyStorage = TrackPolicyStorage(propellerRxV2())
    }

    @Test
    fun `returns urns for tracks with stale or missing policies`() {
        val track = testFixtures().insertTrack()
        val stalePolicyTrack = testFixtures().insertTrack()
        val missingPolicyTrack = testFixtures().insertTrack()

        testFixtures().updatePolicyTimestamp(stalePolicyTrack, Date(100))
        testFixtures().clearTrackPolicy(missingPolicyTrack)

        val trackUrns = Sets.newHashSet(track.urn, stalePolicyTrack.urn, missingPolicyTrack.urn)

        val testObserver = trackPolicyStorage.filterForStalePolicies(trackUrns, Date(200)).test();

        testObserver.assertValue(Sets.newHashSet(stalePolicyTrack.urn, missingPolicyTrack.urn))
    }
}
