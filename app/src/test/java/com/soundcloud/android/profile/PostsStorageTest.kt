package com.soundcloud.android.profile

import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.accounts.AccountOperations
import com.soundcloud.android.api.model.ApiTrack
import com.soundcloud.android.api.model.ApiUser
import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.StorageIntegrationTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import java.util.Date

class PostsStorageTest : StorageIntegrationTest() {

    private lateinit var storage: PostsStorage
    private lateinit var user: ApiUser
    @Mock private lateinit var accountOperations: AccountOperations

    @Before
    fun setUp() {
        user = testFixtures().insertUser()

        storage = PostsStorage(propellerRxV2())

        whenever(accountOperations.loggedInUserUrn).thenReturn(user.urn)
    }

    @Test
    @Throws(Exception::class)
    fun returnsPostsInDescOrder() {
        val lastPost = createTrackPostForLastPostedAt(POSTED_DATE_2)
        val firstPost = createTrackPostForLastPostedAt(POSTED_DATE_1)

        storage.loadPostedTracksSortedByDateDesc().test().assertValue(listOf(lastPost, firstPost))
    }

    private fun createTrackAt(creationDate: Date): ApiTrack {
        return testFixtures().insertTrackWithCreationDate(user, creationDate)
    }

    private fun createTrackPostWithId(trackId: Long, postedAt: Date) {
        testFixtures().insertTrackPost(trackId, postedAt.time, false)
    }

    private fun createTrackPostForLastPostedAt(postedAt: Date): Pair<Urn, Date> {
        val track = createTrackAt(postedAt)
        createTrackPostWithId(track.urn.numericId, postedAt)
        return track.urn to postedAt
    }

    companion object {
        private val POSTED_DATE_1 = Date(100000)
        private val POSTED_DATE_2 = Date(200000)
    }

}
