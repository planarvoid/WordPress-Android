package com.soundcloud.android.search.suggestions

import com.soundcloud.android.likes.LikesStorage
import com.soundcloud.android.model.Association
import com.soundcloud.android.model.Urn
import com.soundcloud.android.playlists.Playlist
import com.soundcloud.android.playlists.PlaylistRepository
import com.soundcloud.android.profile.Following
import com.soundcloud.android.profile.PostsStorage
import com.soundcloud.android.testsupport.StorageIntegrationTest
import com.soundcloud.android.testsupport.fixtures.ModelFixtures
import com.soundcloud.android.tracks.Track
import com.soundcloud.android.tracks.TrackRepository
import com.soundcloud.android.users.User
import com.soundcloud.android.users.UserAssociation
import com.soundcloud.android.users.UserAssociationStorage
import com.soundcloud.android.users.UserStorage
import com.soundcloud.java.optional.Optional
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.`when`
import java.util.Date

class LocalSearchSuggestionOperationsTest : StorageIntegrationTest() {

    private lateinit var suggestionOperationsLocal: LocalSearchSuggestionOperations

    @Mock private lateinit var likesStorage: LikesStorage
    @Mock private lateinit var postsStorage: PostsStorage
    @Mock private lateinit var playlistPostsStorage: com.soundcloud.android.playlists.PostsStorage
    @Mock private lateinit var trackRepository: TrackRepository
    @Mock private lateinit var playlistRepository: PlaylistRepository
    @Mock private lateinit var userAssociationStorage: UserAssociationStorage
    @Mock private lateinit var userStorage: UserStorage

    private lateinit var likedTrack: Track
    private lateinit var postedTrack: Track
    private lateinit var likedTrackSearchSuggestion: SearchSuggestion
    private lateinit var postedTrackSearchSuggestion: SearchSuggestion
    private lateinit var likedPlaylist: Playlist
    private lateinit var likedPlaylistSearchSuggestion: SearchSuggestion
    private lateinit var postedPlaylist: Playlist
    private lateinit var postedPlaylistSearchSuggestion: SearchSuggestion
    private lateinit var followingUser: User
    private lateinit var loggedInUser: User
    private lateinit var creator: User
    private lateinit var followingUserSearchSuggestion: SearchSuggestion
    private lateinit var loggedInUserSearchSuggestion: SearchSuggestion
    private lateinit var likedTrackArtistUsernameSearchSuggestion: SearchSuggestion
    private lateinit var likedPlaylistArtistUsernameSearchSuggestion: SearchSuggestion

    @Before
    fun setUp() {
        suggestionOperationsLocal = LocalSearchSuggestionOperations(likesStorage,
                                                                    postsStorage,
                                                                    playlistPostsStorage,
                                                                    trackRepository,
                                                                    playlistRepository,
                                                                    userAssociationStorage,
                                                                    userStorage)

        followingUser = ModelFixtures.userBuilder().username("Random account").signupDate(Optional.of(Date(500L))).build()
        loggedInUser = ModelFixtures.userBuilder().username("Myself").build()
        creator = ModelFixtures.userBuilder().username("Prolific artist").build()

        likedTrack = ModelFixtures.baseTrackBuilder().creatorName(creator.username()).title("A tune I enjoy").urn(Urn.forTrack(123)).createdAt(Date(100L)).build()
        likedTrackSearchSuggestion = buildSearchSuggestionFromTrack(likedTrack)
        likedTrackArtistUsernameSearchSuggestion = buildSearchSuggestionFromTrackCreatorName(likedTrack)

        postedTrack = ModelFixtures.baseTrackBuilder().creatorName(loggedInUser.username()).title("Awesome song that I created").urn(Urn.forTrack(456)).createdAt(Date(100L)).build()
        postedTrackSearchSuggestion = buildSearchSuggestionFromTrack(postedTrack, DatabaseSearchSuggestion.Kind.Post)

        likedPlaylist = ModelFixtures.playlistBuilder().creatorName(creator.username()).title("Liked playlist").createdAt(Date(0L)).build()
        likedPlaylistSearchSuggestion = buildSearchSuggestionFromPlaylist(likedPlaylist, DatabaseSearchSuggestion.Kind.Like)
        likedPlaylistArtistUsernameSearchSuggestion = buildSearchSuggestionFromPlaylistCreatorName(likedPlaylist)

        postedPlaylist = ModelFixtures.playlistBuilder().creatorName(loggedInUser.username()).title("Cool mix created by me").createdAt(Date(0L)).build()
        postedPlaylistSearchSuggestion = buildSearchSuggestionFromPlaylist(postedPlaylist, DatabaseSearchSuggestion.Kind.Post)

        followingUserSearchSuggestion = buildSearchSuggestionFromUser(followingUser)
        loggedInUserSearchSuggestion = buildSearchSuggestionFromUser(loggedInUser)
    }

    @Test
    fun returnsLikedTrackFromStorageMatchedOnFirstWord() {
        configureStorageAndRepositoryResponses(likedTracks = listOf(likedTrack, postedTrack))

        val suggestions = suggestionOperationsLocal.getSuggestions(likedTrack.title().substring(0, 3).toUpperCase(), loggedInUser.urn(), 3)

        suggestions.test().assertValue(listOf(likedTrackSearchSuggestion))
    }

    @Test
    fun returnsLikedTrackFromStorageMatchedOnSecondWord() {
        configureStorageAndRepositoryResponses(likedTracks = listOf(likedTrack, postedTrack))

        val title = likedTrack.title()
        val startIndex = title.indexOf(" ")
        val suggestions = suggestionOperationsLocal.getSuggestions(title.substring(startIndex + 1, startIndex + 3).toUpperCase(), loggedInUser.urn(), 3)

        suggestions.test().assertValue(listOf(likedTrackSearchSuggestion))
    }

    @Test
    fun returnsMultipleMatchesForLikedTrackByMostRecentlyLiked() {
        val secondLikedTrack = ModelFixtures.baseTrackBuilder().creatorName(creator.username()).title(likedTrack.title()).urn(Urn.forTrack(789)).createdAt(Date(500L)).build()
        val secondLikedTrackSearchSuggestion = buildSearchSuggestionFromTrack(secondLikedTrack)
        configureStorageAndRepositoryResponses(likedTracks = listOf(likedTrack, postedTrack, secondLikedTrack))

        val suggestions = suggestionOperationsLocal.getSuggestions(likedTrack.title().substring(0, 3).toUpperCase(), loggedInUser.urn(), 3)

        suggestions.test().assertValue(listOf(secondLikedTrackSearchSuggestion, likedTrackSearchSuggestion))
    }

    @Test
    fun returnsPostedTrackFromStorageMatchedOnFirstWord() {
        configureStorageAndRepositoryResponses(postedTracks = listOf(likedTrack, postedTrack))

        val suggestions = suggestionOperationsLocal.getSuggestions(postedTrack.title().substring(0, 3).toUpperCase(), loggedInUser.urn(), 1)

        suggestions.test().assertValue(listOf(postedTrackSearchSuggestion))
    }

    @Test
    fun returnsPostedTrackLikeFromStorageMatchedOnSecondWord() {
        configureStorageAndRepositoryResponses(postedTracks = listOf(likedTrack, postedTrack))

        val title = postedTrack.title()
        val startIndex = title.indexOf(" ")
        val suggestions = suggestionOperationsLocal.getSuggestions(title.substring(startIndex + 1, startIndex + 3).toUpperCase(), loggedInUser.urn(), 1)

        suggestions.test().assertValue(listOf(postedTrackSearchSuggestion))
    }

    @Test
    fun returnsMultipleMatchesForPostedTrackOrderedByMostRecentlyPosted() {
        val recentlyPostedTrack = ModelFixtures.baseTrackBuilder().creatorName(loggedInUser.username()).title(postedTrack.title()).urn(Urn.forTrack(789)).createdAt(Date(500L)).build()
        val recentlyPostedTrackSearchSuggestion = buildSearchSuggestionFromTrack(recentlyPostedTrack, DatabaseSearchSuggestion.Kind.Post)

        configureStorageAndRepositoryResponses(postedTracks = listOf(likedTrack, postedTrack, recentlyPostedTrack))

        val suggestions = suggestionOperationsLocal.getSuggestions(postedTrack.title().substring(0, 3).toUpperCase(), loggedInUser.urn(), 2)

        suggestions.test().assertValue(listOf(recentlyPostedTrackSearchSuggestion, postedTrackSearchSuggestion))
    }

    @Test
    fun returnsLikedPlaylistFromStorageMatchedOnFirstWord() {
        configureStorageAndRepositoryResponses(likedPlaylists = listOf(likedPlaylist, postedPlaylist))

        val suggestions = suggestionOperationsLocal.getSuggestions(likedPlaylist.title().substring(0, 3).toUpperCase(), loggedInUser.urn(), 2)

        suggestions.test().assertValue(listOf(likedPlaylistSearchSuggestion))
    }

    @Test
    fun returnsLikedPlaylistFromStorageMatchedOnSecondWord() {
        configureStorageAndRepositoryResponses(likedPlaylists = listOf(likedPlaylist, postedPlaylist))

        val title = likedPlaylist.title()
        val startIndex = title.indexOf(" ")
        val suggestions = suggestionOperationsLocal.getSuggestions(title.substring(startIndex + 1, startIndex + 3).toUpperCase(), loggedInUser.urn(), 2)

        suggestions.test().assertValue(listOf(likedPlaylistSearchSuggestion))
    }

    @Test
    fun returnsPostedPlaylistFromStorageMatchedOnFirstWord() {
        configureStorageAndRepositoryResponses(postedPlaylists = listOf(likedPlaylist, postedPlaylist))

        val suggestions = suggestionOperationsLocal.getSuggestions(postedPlaylist.title().substring(0, 3).toUpperCase(), loggedInUser.urn(), 1)

        suggestions.test().assertValue(listOf(postedPlaylistSearchSuggestion))
    }

    @Test
    fun returnsPostedPlaylistFromStorageMatchedOnSecondWord() {
        configureStorageAndRepositoryResponses(postedPlaylists = listOf(likedPlaylist, postedPlaylist))

        val title = postedPlaylist.title()
        val startIndex = title.indexOf(" ")
        val suggestions = suggestionOperationsLocal.getSuggestions(title.substring(startIndex + 1, startIndex + 3).toUpperCase(), loggedInUser.urn(), 1)

        suggestions.test().assertValue(listOf(postedPlaylistSearchSuggestion))
    }

    @Test
    fun returnsLikedTrackAndPlaylistFromStorageMatchedOnFirstWordOfArtistUsername() {
        configureStorageAndRepositoryResponses(likedTracks = listOf(likedTrack, postedTrack), likedPlaylists = listOf(likedPlaylist, postedPlaylist))

        val suggestions = suggestionOperationsLocal.getSuggestions(creator.username().substring(0, 3).toUpperCase(), loggedInUser.urn(), 2)

        suggestions.test().assertValue(listOf(likedTrackArtistUsernameSearchSuggestion, likedPlaylistArtistUsernameSearchSuggestion))
    }

    @Test
    fun returnsLikedTrackAndPlaylistFromStorageMatchedOnSecondWordOfArtistUsername() {
        configureStorageAndRepositoryResponses(likedTracks = listOf(likedTrack, postedTrack), likedPlaylists = listOf(likedPlaylist, postedPlaylist))

        val username = creator.username()
        val startIndex = username.indexOf(" ")
        val suggestions = suggestionOperationsLocal.getSuggestions(username.substring(startIndex + 1, startIndex + 3).toUpperCase(), loggedInUser.urn(), 2)

        suggestions.test().assertValue(listOf(likedTrackArtistUsernameSearchSuggestion, likedPlaylistArtistUsernameSearchSuggestion))
    }

    @Test
    fun removesDuplicateWhenUserSearchesByCreatorNameAndLikedTrackWithTitleContainingArtistName() {
        val likedTrackWithArtistName = ModelFixtures.baseTrackBuilder()
                .creatorName(creator.username())
                .title("Great song by " + creator.username())
                .urn(Urn.forTrack(789))
                .createdAt(Date(500L))
                .build()
        val likedTrackWithArtistNameSearchSuggestion = buildSearchSuggestionFromTrack(likedTrackWithArtistName)
        configureStorageAndRepositoryResponses(likedTracks = listOf(likedTrack, postedTrack, likedTrackWithArtistName))

        val suggestions = suggestionOperationsLocal.getSuggestions(creator.username().substring(0, 3).toUpperCase(), loggedInUser.urn(), 6)

        suggestions.test().assertValue(listOf(likedTrackWithArtistNameSearchSuggestion, likedTrackArtistUsernameSearchSuggestion))
    }

    @Test
    fun returnsUserAndLikedTrackAndPlaylistInCorrectOrderWhenCreatorIsFollowed() {
        configureStorageAndRepositoryResponses(likedTracks = listOf(likedTrack), likedPlaylists = listOf(likedPlaylist), followingUsers = listOf(creator))

        val suggestions = suggestionOperationsLocal.getSuggestions(creator.username().substring(0, 3).toUpperCase(), loggedInUser.urn(), 4)

        suggestions.test().assertValue(listOf(buildSearchSuggestionFromUser(creator),
                                              likedTrackArtistUsernameSearchSuggestion,
                                              likedPlaylistArtistUsernameSearchSuggestion))
    }

    @Test
    fun returnsFollowingUserFromStorageMatchedOnFirstWord() {
        configureStorageAndRepositoryResponses(followingUsers = listOf(followingUser))

        val suggestions = suggestionOperationsLocal.getSuggestions(followingUser.username().substring(0, 3).toUpperCase(), loggedInUser.urn(), 1)

        suggestions.test().assertValue(listOf(followingUserSearchSuggestion))
    }

    @Test
    fun returnsFollowingUserFromStorageMatchedOnSecondWord() {
        configureStorageAndRepositoryResponses(followingUsers = listOf(followingUser))

        val username = followingUser.username()
        val startIndex = username.indexOf(" ")
        val suggestions = suggestionOperationsLocal.getSuggestions(username.substring(startIndex + 1, startIndex + 3).toUpperCase(), loggedInUser.urn(), 2)

        suggestions.test().assertValue(listOf(followingUserSearchSuggestion))
    }

    @Test
    fun returnsMultipleMatchesForFollowingUserSortedByMostRecentlyFollowed() {
        val mostRecentFollowingUser = ModelFixtures.userBuilder()
                .username(followingUser.username())
                .signupDate(Optional.of(Date(501L)))
                .build()

        configureStorageAndRepositoryResponses(followingUsers = listOf(followingUser, mostRecentFollowingUser))

        val mostRecentFollowingUserSearchSuggestion = buildSearchSuggestionFromUser(mostRecentFollowingUser)

        val suggestions = suggestionOperationsLocal.getSuggestions(followingUser.username().substring(0, 3).toUpperCase(), loggedInUser.urn(), 2)

        suggestions.test().assertValue(listOf(mostRecentFollowingUserSearchSuggestion, followingUserSearchSuggestion))
    }

    @Test
    fun returnsAllTypesFromStorageInTheCorrectOrder() {
        configureStorageAndRepositoryResponses(likedTracks = listOf(likedTrack),
                                               likedPlaylists = listOf(likedPlaylist),
                                               postedTracks = listOf(postedTrack),
                                               postedPlaylists = listOf(postedPlaylist),
                                               loggedInUser = loggedInUser,
                                               followingUsers = listOf(followingUser))

        val suggestions = suggestionOperationsLocal.getSuggestions("", loggedInUser.urn(), 20)

        suggestions.test().assertValue(listOf(likedTrackSearchSuggestion,
                                              likedPlaylistSearchSuggestion,
                                              followingUserSearchSuggestion,
                                              loggedInUserSearchSuggestion,
                                              postedTrackSearchSuggestion,
                                              postedPlaylistSearchSuggestion))
    }

    @Test
    fun returnsLimitedItemsFromStorage() {
        configureStorageAndRepositoryResponses(likedTracks = listOf(likedTrack, postedTrack))

        val suggestions = suggestionOperationsLocal.getSuggestions("", loggedInUser.urn(), 1)

        suggestions.test().assertValue(listOf(likedTrackSearchSuggestion))
    }

    @Test
    fun returnsOnlyMatchedItemFromStorage() {
        configureStorageAndRepositoryResponses(followingUsers = listOf(followingUser))

        val suggestions = suggestionOperationsLocal.getSuggestions(followingUser.username(), loggedInUser.urn(), 3)

        suggestions.test().assertValue(listOf(followingUserSearchSuggestion))
    }

    @Test
    fun returnsLoggedInItemFromStorage() {
        configureStorageAndRepositoryResponses(loggedInUser = loggedInUser)

        val suggestions = suggestionOperationsLocal.getSuggestions(loggedInUser.username(), loggedInUser.urn(), 3)

        suggestions.test().assertValue(listOf(loggedInUserSearchSuggestion))
    }

    private fun configureStorageAndRepositoryResponses(likedTracks: List<Track> = emptyList(),
                                                       postedTracks: List<Track> = emptyList(),
                                                       likedPlaylists: List<Playlist> = emptyList(),
                                                       postedPlaylists: List<Playlist> = emptyList(),
                                                       followingUsers: List<User> = emptyList(),
                                                       loggedInUser: User = this.loggedInUser) {
        configureLikedTracksResponse(likedTracks)
        configureLikedPlaylistsResponse(likedPlaylists)
        configureFollowingUsersResponse(followingUsers)
        configureLoggedInUserResponse(loggedInUser)
        configurePostedTracksResponse(postedTracks)
        configurePostedPlaylistsResponse(postedPlaylists)
    }

    private fun configureLikedTracksResponse(tracks: List<Track>) {
        `when`(likesStorage.loadTrackLikes()).thenReturn(Single.just(tracks.map { Association(it.urn(), it.createdAt()) }))
        `when`(trackRepository.fromUrns(tracks.map { it.urn() })).thenReturn(Single.just(tracks.map { it.urn() to it }.toMap()))
    }

    private fun configurePostedTracksResponse(tracks: List<Track>) {
        `when`(postsStorage.loadPostedTracksSortedByDateDesc()).thenReturn(Single.just(tracks.map { Association(it.urn(), it.createdAt()) }))
        `when`(trackRepository.fromUrns(tracks.map { it.urn() })).thenReturn(Single.just(tracks.map { it.urn() to it }.toMap()))
    }

    private fun configureFollowingUsersResponse(users: List<User>) {
        `when`(userAssociationStorage.loadFollowings()).thenReturn(Single.just(users.map { Following.from(it, UserAssociation.create(it.urn(), 0, 0, it.signupDate(), Optional.absent())) }))
    }

    private fun configureLoggedInUserResponse(loggedInUser: User) {
        `when`(userStorage.loadUsers(listOf(loggedInUser.urn()))).thenReturn(Single.just(listOf(loggedInUser)))
    }

    private fun configureLikedPlaylistsResponse(playlists: List<Playlist>) {
        `when`(likesStorage.loadPlaylistLikes()).thenReturn(Single.just(playlists.map { Association(it.urn(), it.createdAt()) }))
        `when`(playlistRepository.withUrns(playlists.map { it.urn() })).thenReturn(Single.just(playlists.map { it.urn() to it }.toMap()))
    }

    private fun configurePostedPlaylistsResponse(playlists: List<Playlist>) {
        `when`(playlistPostsStorage.loadPostedPlaylists(ArgumentMatchers.anyInt())).thenReturn(Single.just(playlists.map { Association(it.urn(), it.createdAt()) }))
        `when`(playlistRepository.withUrns(playlists.map { it.urn() })).thenReturn(Single.just(playlists.map { it.urn() to it }.toMap()))
    }

    private fun buildSearchSuggestionFromUser(user: User): SearchSuggestion {
        return DatabaseSearchSuggestion.create(user.urn(),
                                               user.username(),
                                               user.avatarUrl(),
                                               user.isPro,
                                               DatabaseSearchSuggestion.Kind.Following)
    }

    private fun buildSearchSuggestionFromPlaylistCreatorName(playlist: Playlist): DatabaseSearchSuggestion {
        return DatabaseSearchSuggestion.create(playlist.urn(),
                                               "${playlist.creatorName()} - ${playlist.title()}",
                                               playlist.imageUrlTemplate(),
                                               false,
                                               DatabaseSearchSuggestion.Kind.LikeByUsername)
    }

    private fun buildSearchSuggestionFromPlaylist(playlist: Playlist, kind: DatabaseSearchSuggestion.Kind): SearchSuggestion {
        return DatabaseSearchSuggestion.create(playlist.urn(),
                                               playlist.title(),
                                               playlist.imageUrlTemplate(),
                                               false,
                                               kind)
    }

    private fun buildSearchSuggestionFromTrackCreatorName(track: Track): SearchSuggestion {
        return DatabaseSearchSuggestion.create(track.urn(),
                                               "${track.creatorName()} - ${track.title()}",
                                               track.imageUrlTemplate(),
                                               false,
                                               DatabaseSearchSuggestion.Kind.LikeByUsername)
    }

    private fun buildSearchSuggestionFromTrack(track: Track, kind: DatabaseSearchSuggestion.Kind = DatabaseSearchSuggestion.Kind.Like): SearchSuggestion {
        return DatabaseSearchSuggestion.create(track.urn(),
                                               track.title(),
                                               track.imageUrlTemplate(),
                                               false,
                                               kind)
    }
}
