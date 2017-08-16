package com.soundcloud.android.search.suggestions

import android.support.annotation.NonNull
import com.soundcloud.android.api.model.ApiPlaylist
import com.soundcloud.android.api.model.ApiTrack
import com.soundcloud.android.api.model.ApiUser
import com.soundcloud.android.testsupport.StorageIntegrationTest
import org.assertj.core.util.Lists
import org.junit.Before
import org.junit.Test
import java.util.*

class SearchSuggestionStorageTest : StorageIntegrationTest() {

    private lateinit var suggestionStorage: SearchSuggestionStorage

    private lateinit var likedTrack: ApiTrack
    private lateinit var ownedTrack: ApiTrack
    private lateinit var likedTrackSearchSuggestion: SearchSuggestion
    private lateinit var ownedTrackSearchSuggestion: SearchSuggestion
    private lateinit var likedPlaylist: ApiPlaylist
    private lateinit var likedPlaylistSearchSuggestion: SearchSuggestion
    private lateinit var ownedPlaylist: ApiPlaylist
    private lateinit var ownedPlaylistSearchSuggestion: SearchSuggestion
    private lateinit var followingUser: ApiUser
    private lateinit var loggedInUser: ApiUser
    private lateinit var creator: ApiUser
    private lateinit var followingUserSearchSuggestion: SearchSuggestion
    private lateinit var loggedInUserSearchSuggestion: SearchSuggestion
    private lateinit var likedTrackArtistUsernameSearchSuggestion: SearchSuggestion
    private lateinit var likedPlaylistArtistUsernameSearchSuggestion: SearchSuggestion

    @Before
    fun setUp() {
        suggestionStorage = SearchSuggestionStorage(propellerRx())

        followingUser = testFixtures().insertUser("Random account")
        loggedInUser = testFixtures().insertUser("Myself")
        creator = testFixtures().insertUser("Prolific artist")

        likedTrack = testFixtures().insertTrackWithTitle("A tune I enjoy", creator)
        testFixtures().insertLikedTrack(likedTrack, 100L)
        likedTrackSearchSuggestion = buildSearchSuggestionFromApiTrack(likedTrack)
        likedTrackArtistUsernameSearchSuggestion = buildSearchSuggestionFromApiTrackArtistUsername(likedTrack)

        ownedTrack = testFixtures().insertTrackWithTitle("Awesome song that I created", loggedInUser)
        testFixtures().insertTrackPost(ownedTrack, 100L)
        ownedTrackSearchSuggestion = buildSearchSuggestionFromApiTrack(ownedTrack, DatabaseSearchSuggestion.Kind.Post)

        likedPlaylist = testFixtures().insertPlaylistWithTitle("Liked playlist", creator)
        testFixtures().insertLikedPlaylist(Date (0L), likedPlaylist)
        likedPlaylistSearchSuggestion = buildSearchSuggestionFromApiPlaylist(likedPlaylist, DatabaseSearchSuggestion.Kind.Like)
        likedPlaylistArtistUsernameSearchSuggestion = buildSearchSuggestionFromApiPlaylistArtistUsername(likedPlaylist)

        ownedPlaylist = testFixtures().insertPlaylistWithTitle("Cool mix created by me", loggedInUser)
        testFixtures().insertPostedPlaylist(ownedPlaylist, 0L)
        ownedPlaylistSearchSuggestion = buildSearchSuggestionFromApiPlaylist(ownedPlaylist, DatabaseSearchSuggestion.Kind.Post)

        followingUserSearchSuggestion = buildSearchSuggestionFromApiUser(followingUser)
        loggedInUserSearchSuggestion = buildSearchSuggestionFromApiUser(loggedInUser)
        testFixtures().insertFollowing(followingUser.urn, 0L)
    }

    @Test
    fun returnsLikedTrackFromStorageMatchedOnFirstWord() {
        val suggestions = suggestionStorage.getSuggestions(likedTrack.title.substring(0, 3), loggedInUser.urn, 1)

        suggestions.test().assertValue(Lists.newArrayList(likedTrackSearchSuggestion))
    }

    @Test
    fun returnsLikedTrackFromStorageMatchedOnSecondWord() {
        val title = likedTrack.title
        val startIndex = title.indexOf(" ")
        val suggestions = suggestionStorage.getSuggestions(title.substring(startIndex + 1, startIndex + 3), loggedInUser.urn, 1)

        suggestions.test().assertValue(Lists.newArrayList(likedTrackSearchSuggestion))
    }

    @Test
    fun returnsMultipleMatchesForLikedTrackByMostRecentlyLiked() {
        val secondLikedTrack = testFixtures().insertTrackWithTitle(likedTrack.title, creator, likedTrack.createdAt.time - 1)
        testFixtures().insertLikedTrack(secondLikedTrack, 500L)
        val secondLikedTrackSearchSuggestion = buildSearchSuggestionFromApiTrack(secondLikedTrack)

        val suggestions = suggestionStorage.getSuggestions(likedTrack.title.substring(0, 3), loggedInUser.urn, 2)

        suggestions.test().assertValue(Lists.newArrayList(secondLikedTrackSearchSuggestion, likedTrackSearchSuggestion))
    }

    @Test
    fun returnsOwnedTrackFromStorageMatchedOnFirstWord() {
        val suggestions = suggestionStorage.getSuggestions(ownedTrack.title.substring(0, 3), loggedInUser.urn, 1)

        suggestions.test().assertValue(Lists.newArrayList(ownedTrackSearchSuggestion))
    }

    @Test
    fun returnsOwnedTrackLikeFromStorageMatchedOnSecondWord() {
        val title = ownedTrack.title
        val startIndex = title.indexOf(" ")
        val suggestions = suggestionStorage.getSuggestions(title.substring(startIndex + 1, startIndex + 3), loggedInUser.urn, 1)

        suggestions.test().assertValue(Lists.newArrayList(ownedTrackSearchSuggestion))
    }

    @Test
    fun returnsMultipleMatchesForOwnedTrackByMostRecentlyPosted() {
        val secondOwnedTrack = testFixtures().insertTrackWithTitle(ownedTrack.title, loggedInUser, ownedTrack.createdAt.time - 1)
        testFixtures().insertTrackPost(secondOwnedTrack, 500L)
        val secondOwnedTrackSearchSuggestion = buildSearchSuggestionFromApiTrack(secondOwnedTrack, DatabaseSearchSuggestion.Kind.Post)

        val suggestions = suggestionStorage.getSuggestions(ownedTrack.title.substring(0, 3), loggedInUser.urn, 2)

        suggestions.test().assertValue(Lists.newArrayList(secondOwnedTrackSearchSuggestion, ownedTrackSearchSuggestion))
    }

    @Test
    fun returnsLikedPlaylistFromStorageMatchedOnFirstWord() {
        val suggestions = suggestionStorage.getSuggestions(likedPlaylist.title.substring(0, 3), loggedInUser.urn, 1)

        suggestions.test().assertValue(Lists.newArrayList(likedPlaylistSearchSuggestion))
    }

    @Test
    fun returnsLikedPlaylistFromStorageMatchedOnSecondWord() {
        val title = likedPlaylist.title
        val startIndex = title.indexOf(" ")
        val suggestions = suggestionStorage.getSuggestions(title.substring(startIndex + 1, startIndex + 3), loggedInUser.urn, 1)

        suggestions.test().assertValue(Lists.newArrayList(likedPlaylistSearchSuggestion))
    }

    @Test
    fun returnsOwnedPlaylistFromStorageMatchedOnFirstWord() {
        val suggestions = suggestionStorage.getSuggestions(ownedPlaylist.title.substring(0, 3), loggedInUser.urn, 1)

        suggestions.test().assertValue(Lists.newArrayList(ownedPlaylistSearchSuggestion))
    }

    @Test
    fun returnsOwnedPlaylistFromStorageMatchedOnSecondWord() {
        val title = ownedPlaylist.title
        val startIndex = title.indexOf(" ")
        val suggestions = suggestionStorage.getSuggestions(title.substring(startIndex + 1, startIndex + 3), loggedInUser.urn, 1)

        suggestions.test().assertValue(Lists.newArrayList(ownedPlaylistSearchSuggestion))
    }

    @Test
    fun returnsLikedTrackAndPlaylistFromStorageMatchedOnFirstWordOfArtistUsername() {
        val suggestions = suggestionStorage.getSuggestions(creator.username.substring(0, 3), loggedInUser.urn, 2)

        suggestions.test().assertValue(Lists.newArrayList(likedTrackArtistUsernameSearchSuggestion,
                likedPlaylistArtistUsernameSearchSuggestion))
    }

    @Test
    fun returnsLikedTrackAndPlaylistFromStorageMatchedOnSecondWordOfArtistUsername() {
        val username = creator.username
        val startIndex = username.indexOf(" ")
        val suggestions = suggestionStorage.getSuggestions(username.substring(startIndex + 1, startIndex + 3), loggedInUser.urn, 2)

        suggestions.test().assertValue(Lists.newArrayList(likedTrackArtistUsernameSearchSuggestion,
                likedPlaylistArtistUsernameSearchSuggestion))
    }

    @Test
    fun removesDuplicateWhenUserSearchesByArtistNameAndLikedTrackWithTitleContainingArtistName() {
        val likedTrackWithArtistName = testFixtures().insertTrackWithTitle("Great song by " + creator.username, creator)
        testFixtures().insertLikedTrack(likedTrackWithArtistName, 500L)
        val likedTrackWithArtistNameSearchSuggestion = buildSearchSuggestionFromApiTrack(likedTrackWithArtistName)

        val suggestions = suggestionStorage.getSuggestions(creator.username.substring(0, 3), loggedInUser.urn, 6)

        suggestions.test().assertValue(Lists.newArrayList(likedTrackWithArtistNameSearchSuggestion,
                likedTrackArtistUsernameSearchSuggestion,
                likedPlaylistArtistUsernameSearchSuggestion))
    }

    @Test
    fun returnsUserAndLikedTrackAndPlaylistInCorrectOrderWhenCreatorIsFollowed() {
        testFixtures().insertFollowing(creator.urn)
        val suggestions = suggestionStorage.getSuggestions(creator.username.substring(0, 3), loggedInUser.urn, 4)

        suggestions.test().assertValue(Lists.newArrayList(buildSearchSuggestionFromApiUser(creator),
                likedTrackArtistUsernameSearchSuggestion,
                likedPlaylistArtistUsernameSearchSuggestion))
    }

    @Test
    fun returnsFollowingUserFromStorageMatchedOnFirstWord() {
        val suggestions = suggestionStorage.getSuggestions(followingUser.username.substring(0, 3), loggedInUser.urn, 1)

        suggestions.test().assertValue(Lists.newArrayList(followingUserSearchSuggestion))
    }

    @Test
    fun returnsFollowingUserFromStorageMatchedOnSecondWord() {
        val username = followingUser.username
        val startIndex = username.indexOf(" ")
        val suggestions = suggestionStorage.getSuggestions(username.substring(startIndex + 1, startIndex + 3), loggedInUser.urn, 2)

        suggestions.test().assertValue(Lists.newArrayList(followingUserSearchSuggestion))
    }

    @Test
    fun returnsMultipleMatchesForFollowingUserSortedByMostRecentlyFollowed() {
        val secondFollowingUser = testFixtures().insertUser(followingUser.username, followingUser.createdAt.get().time - 1)
        testFixtures().insertFollowing(secondFollowingUser.urn, 500L)
        val secondFollowingUserSearchSuggestion = buildSearchSuggestionFromApiUser(secondFollowingUser)

        val suggestions = suggestionStorage.getSuggestions(followingUser.username.substring(0, 3), loggedInUser.urn, 2)

        suggestions.test().assertValue(Lists.newArrayList(secondFollowingUserSearchSuggestion,
                followingUserSearchSuggestion))
    }

    @Test
    fun returnsAllTypesFromStorageInTheCorrectOrder() {
        val suggestions = suggestionStorage.getSuggestions("", loggedInUser.urn, 20)

        suggestions.test().assertValue(Lists.newArrayList(likedTrackSearchSuggestion,
                likedPlaylistSearchSuggestion,
                followingUserSearchSuggestion,
                loggedInUserSearchSuggestion,
                ownedTrackSearchSuggestion,
                ownedPlaylistSearchSuggestion))
    }

    @Test
    fun returnsLimitedItemsFromStorage() {
        val suggestions = suggestionStorage.getSuggestions("", loggedInUser.urn, 1)

        suggestions.test().assertValue(Lists.newArrayList(likedTrackSearchSuggestion))
    }

    @Test
    fun returnsOnlyMatchedItemFromStorage() {
        val suggestions = suggestionStorage.getSuggestions(followingUser.username, loggedInUser.urn, 3)

        suggestions.test().assertValue(Lists.newArrayList(followingUserSearchSuggestion))
    }

    @Test
    fun returnsLoggedInItemFromStorage() {
        val suggestions = suggestionStorage.getSuggestions(loggedInUser.username, loggedInUser.urn, 3)

        suggestions.test().assertValue(Lists.newArrayList(loggedInUserSearchSuggestion))
    }

    @NonNull
    private fun buildSearchSuggestionFromApiTrack(apiTrack: ApiTrack, kind: DatabaseSearchSuggestion.Kind = DatabaseSearchSuggestion.Kind.Like): SearchSuggestion {
        return DatabaseSearchSuggestion.create(apiTrack.urn, apiTrack.title, apiTrack.imageUrlTemplate, kind)
    }

    @NonNull
    private fun buildSearchSuggestionFromApiTrackArtistUsername(apiTrack: ApiTrack): SearchSuggestion {
        return DatabaseSearchSuggestion.create(apiTrack.urn, "${apiTrack.userName} - ${apiTrack.title}", apiTrack.imageUrlTemplate, DatabaseSearchSuggestion.Kind.LikeByUsername)
    }

    @NonNull
    private fun buildSearchSuggestionFromApiPlaylist(apiPlaylist: ApiPlaylist, kind: DatabaseSearchSuggestion.Kind): SearchSuggestion {
        return DatabaseSearchSuggestion.create(apiPlaylist.urn, apiPlaylist.title, apiPlaylist.imageUrlTemplate, kind)
    }

    @NonNull
    private fun buildSearchSuggestionFromApiPlaylistArtistUsername(apiPlaylist: ApiPlaylist): SearchSuggestion {
        return DatabaseSearchSuggestion.create(apiPlaylist.urn, "${apiPlaylist.username} - ${apiPlaylist.title}", apiPlaylist.imageUrlTemplate, DatabaseSearchSuggestion.Kind.LikeByUsername)
    }

    @NonNull
    private fun buildSearchSuggestionFromApiUser(apiUser: ApiUser): SearchSuggestion {
        return DatabaseSearchSuggestion.create(apiUser.urn, apiUser.username, apiUser.imageUrlTemplate, DatabaseSearchSuggestion.Kind.Following)
    }
}
