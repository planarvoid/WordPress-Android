package com.soundcloud.android.posts

import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.accounts.AccountOperations
import com.soundcloud.android.api.model.ApiPlaylist
import com.soundcloud.android.api.model.ApiTrack
import com.soundcloud.android.api.model.ApiUser
import com.soundcloud.android.model.Association
import com.soundcloud.android.model.Urn
import com.soundcloud.android.playlists.Playlist
import com.soundcloud.android.playlists.PlaylistAssociation
import com.soundcloud.android.playlists.RemovePlaylistCommand
import com.soundcloud.android.storage.Table
import com.soundcloud.android.storage.TableColumns
import com.soundcloud.android.storage.Tables
import com.soundcloud.android.testsupport.PlaylistFixtures
import com.soundcloud.android.testsupport.StorageIntegrationTest
import com.soundcloud.android.testsupport.TrackFixtures
import com.soundcloud.android.testsupport.fixtures.ModelFixtures
import com.soundcloud.android.utils.TestDateProvider
import com.soundcloud.propeller.query.Query.from
import com.soundcloud.propeller.test.assertions.QueryAssertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import java.util.Arrays
import java.util.Date

@Suppress("IllegalIdentifier")
class PostsStorageTest : StorageIntegrationTest() {

    private lateinit var storage: PostsStorage
    private lateinit var user: ApiUser

    @Mock private lateinit var accountOperations: AccountOperations
    @Mock private lateinit var removePlaylistCommand: RemovePlaylistCommand

    @Before
    fun setUp() {
        user = testFixtures().insertUser()

        storage = PostsStorage(propellerRxV2(), TestDateProvider(), removePlaylistCommand)

        whenever(accountOperations.loggedInUserUrn).thenReturn(user.urn)
    }

    @Test
    @Throws(Exception::class)
    fun `loadPostedTracksSortedByDateDesc loads posted tracks in descending order`() {
        val lastPost = createTrackPostForLastPostedAt(POSTED_DATE_2)
        val firstPost = createTrackPostForLastPostedAt(POSTED_DATE_1)

        storage.loadPostedTracksSortedByDateDesc().test().assertValue(listOf(lastPost, firstPost))
    }

    @Test
    @Throws(Exception::class)
    fun `loadPostedPlaylists loads all posted playlists`() {
        val playlist1 = createPlaylistPostAt(POSTED_DATE_1)
        val playlist2 = createPlaylistPostAt(POSTED_DATE_2)

        storage.loadPostedPlaylists(10, java.lang.Long.MAX_VALUE).test()
                .assertValue(Arrays.asList(playlist2.association, playlist1.association))
    }

    @Test
    @Throws(Exception::class)
    fun `loadPostedPlaylists loads a limited number of posted playists`() {
        createPlaylistPostAt(POSTED_DATE_1)
        val playlist2 = createPlaylistPostAt(POSTED_DATE_2)

        storage.loadPostedPlaylists(1, java.lang.Long.MAX_VALUE).test()
                .assertValue(listOf(playlist2.association))
    }

    @Test
    @Throws(Exception::class)
    fun `loadPostedPlaylists only returns playlists posted after the given date`() {
        val playlist1 = createPlaylistAssociation(POSTED_DATE_1, POSTED_DATE_1)
        createPlaylistAssociation(POSTED_DATE_3, POSTED_DATE_1)

        storage.loadPostedPlaylists(2, POSTED_DATE_2.time).test()
                .assertValue(listOf(playlist1.association))
    }

    @Test
    @Throws(Exception::class)
    fun `loadPostedPlaylists only loads playlists that are in the CollectionItems table`() {
        val playlist1 = createPlaylistPostAt(POSTED_DATE_1)
        val playlist2 = createPlaylistPostAt(POSTED_DATE_2)
        createPlaylistAt(POSTED_DATE_3) // deleted

        storage.loadPostedPlaylists(10, java.lang.Long.MAX_VALUE).test()
                .assertValue(Arrays.asList(playlist2.association, playlist1.association))
    }

    @Test
    @Throws(Exception::class)
    fun `loadPostedPlaylists only loads returns playlists`() {
        val playlist1 = createPlaylistPostAt(POSTED_DATE_1)
        val playlist2 = createPlaylistPostAt(POSTED_DATE_2)

        createTrackPostWithId(playlist2.playlist.urn().numericId)

        storage.loadPostedPlaylists(10, java.lang.Long.MAX_VALUE).test()
                .assertValue(Arrays.asList(playlist2.association, playlist1.association))
    }

    @Test
    fun `markPlaylistPendingRemoval removes associated entry from Activities View`() {
        val playlist = PlaylistFixtures.apiPlaylist()
        val apiActivityItem = ModelFixtures.apiPlaylistRepostActivity(playlist)
        testFixtures().insertPlaylistRepostActivity(apiActivityItem)

        storage.markPlaylistPendingRemoval(playlist.urn).subscribe()

        val query = from(Table.Activities)
                .whereEq(TableColumns.Activities.SOUND_ID, playlist.id)
                .whereEq(TableColumns.Activities.SOUND_TYPE, com.soundcloud.android.storage.Tables.Sounds.TYPE_PLAYLIST)

        QueryAssertions.assertThat(select(query)).isEmpty
    }

    @Test
    fun `markPlaylistPendingRemoval removes associated entry from SoundStream View`() {
        val playlist = PlaylistFixtures.apiPlaylist()
        testFixtures().insertStreamPlaylistPost(playlist.id, 123L)

        storage.markPlaylistPendingRemoval(playlist.urn).subscribe()

        val query = from(Table.SoundStream)
                .whereEq(TableColumns.SoundStream.SOUND_ID, playlist.id)
                .whereEq(TableColumns.SoundStream.SOUND_TYPE, com.soundcloud.android.storage.Tables.Sounds.TYPE_PLAYLIST)

        QueryAssertions.assertThat(select(query)).isEmpty
    }

    @Test
    fun `markPlaylistPendingRemoval removes associated entry from Posts table`() {
        val playlist1 = createPlaylistPostAt(POSTED_DATE_1)
        val playlist2 = createPlaylistPostAt(POSTED_DATE_2)

        storage.markPlaylistPendingRemoval(playlist1.targetUrn).subscribe()

        storage.loadPostedPlaylists(10, java.lang.Long.MAX_VALUE).test()
                .assertValue(Arrays.asList(playlist2.association))
    }

    @Test
    fun `markPlaylistPendingRemoval removes associated entry from Sounds table`() {
        val playlist = PlaylistFixtures.apiPlaylist()
        testFixtures().insertStreamPlaylistPost(playlist.id, 123L)

        storage.markPlaylistPendingRemoval(playlist.urn).subscribe()

        val query = from(Tables.Sounds.TABLE)
                .whereEq(Tables.Sounds._ID, playlist.id)
                .whereEq(Tables.Sounds._TYPE, com.soundcloud.android.storage.Tables.Sounds.TYPE_PLAYLIST)

        QueryAssertions.assertThat(select(query)).isEmpty
    }

    @Test(expected = IllegalArgumentException::class)
    fun `markPlaylistPendingRemoval throws an exception if urn is not a playlist urn`() {
        storage.markPlaylistPendingRemoval(Urn.forTrack(123L)).subscribe()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `removePlaylist throws an exception if urn is not a playlist urn`() {
        storage.removePlaylist(Urn.forTrack(123L)).subscribe()
    }

    private fun createPlaylistAssociation(postedAt: Date, playlistCreatedAt: Date): PlaylistAssociation {
        val apiPlaylist = createPlaylistAt(playlistCreatedAt)
        val playlist = PlaylistFixtures.playlistBuilder(apiPlaylist)
                .isLikedByCurrentUser(false)
                .isRepostedByCurrentUser(false)

        insertPlaylistPost(apiPlaylist.urn.numericId, postedAt)
        return createPlaylistAssociation(playlist, postedAt)
    }

    private fun createPlaylistPostAt(playlistPostedAt: Date): PlaylistAssociation = createPlaylistAssociation(playlistPostedAt, playlistPostedAt)

    private fun createPlaylistAssociation(builder: Playlist.Builder, createdAt: Date): PlaylistAssociation {
        val playlist = builder.isLikedByCurrentUser(false).build()
        return PlaylistAssociation.create(playlist, Association(playlist.urn(), createdAt))
    }

    private fun createPlaylistAt(creationDate: Date): ApiPlaylist = testFixtures().insertPlaylistWithCreatedAt(creationDate)

    private fun insertPlaylistPost(playlistId: Long, postedAt: Date) {
        testFixtures().insertPlaylistPost(playlistId, postedAt.time, false)
    }

    private fun createTrackPostWithId(trackId: Long) {
        val apiTrack = TrackFixtures.apiTrack(Urn.forTrack(trackId))
        testFixtures().insertTrack(apiTrack)
        testFixtures().insertTrackPost(apiTrack.id, apiTrack.createdAt.time, false)
    }

    private fun createTrackAt(creationDate: Date): ApiTrack = testFixtures().insertTrackWithCreationDate(user, creationDate)

    private fun createTrackPostWithId(trackId: Long, postedAt: Date) {
        testFixtures().insertTrackPost(trackId, postedAt.time, false)
    }

    private fun createTrackPostForLastPostedAt(postedAt: Date): Association {
        val track = createTrackAt(postedAt)
        createTrackPostWithId(track.urn.numericId, postedAt)
        return Association(track.urn, postedAt)
    }

    companion object {
        private val POSTED_DATE_1 = Date(100000)
        private val POSTED_DATE_2 = Date(200000)
        private val POSTED_DATE_3 = Date(300000)
    }

}
