package com.soundcloud.android.search.suggestions

import com.soundcloud.android.likes.LikesStorage
import com.soundcloud.android.model.Association
import com.soundcloud.android.model.Urn
import com.soundcloud.android.model.UrnHolder
import com.soundcloud.android.playlists.Playlist
import com.soundcloud.android.playlists.PlaylistRepository
import com.soundcloud.android.profile.PostsStorage
import com.soundcloud.android.tracks.Track
import com.soundcloud.android.tracks.TrackRepository
import com.soundcloud.android.users.UserAssociationStorage
import com.soundcloud.android.users.UserStorage
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.android.utils.enrichItemsWithProperties
import com.soundcloud.java.collections.Lists
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function8
import java.util.Date
import javax.inject.Inject

@OpenForTesting
class LocalSearchSuggestionOperations
@Inject
constructor(private val likesStorage: LikesStorage,
            private val postsStorage: PostsStorage,
            private val playlistPostsStorage: com.soundcloud.android.playlists.PostsStorage,
            private val trackRepository: TrackRepository,
            private val playlistRepository: PlaylistRepository,
            private val userAssociationStorage: UserAssociationStorage,
            private val userStorage: UserStorage) {

    fun getSuggestions(searchQuery: String, loggedInUserUrn: Urn, limit: Int): Single<List<SearchSuggestion>> {
        return getInitialSuggestions(searchQuery, loggedInUserUrn, limit)
                .map { this.removeDuplicates(it) }
                .map { it -> it.subList(0, minOf(it.size, limit)) }
    }

    private fun getInitialSuggestions(searchQuery: String, loggedInUserUrn: Urn, limit: Int): Single<List<DatabaseSearchSuggestion>> {
        return Single.zip(getLikedTracksWithTitleLike(likesStorage.loadTrackLikes(), searchQuery),
                          getLikedPlaylistsWithTitleLike(likesStorage.loadPlaylistLikes(), searchQuery),
                          getFollowingUsersWithUsernameLike(searchQuery),
                          getLoggedInUser(searchQuery, loggedInUserUrn),
                          getPostedTracksWithTitleLike(searchQuery),
                          getPostedPlaylistsWithTitleLike(searchQuery, limit),
                          getLikedTracksWithCreatorLike(likesStorage.loadTrackLikes(), searchQuery),
                          getLikedPlaylistsWithCreatorLike(likesStorage.loadPlaylistLikes(), searchQuery),
                          Function8 { t1, t2, t3, t4, t5, t6, t7, t8 -> t1.plus(t2).plus(t3).plus(t4).plus(t5).plus(t6).plus(t7).plus(t8) })
    }

    private fun getFollowingUsersWithUsernameLike(searchQuery: String): Single<List<DatabaseSearchSuggestion>> {
        return userAssociationStorage
                .loadFollowings()
                .map { followings ->
                    followings
                            .filter { it.user().username().contains(searchQuery, ignoreCase = true) }
                            .sortedByDescending { it.userAssociation().addedAt().or(Date(0L)) }
                            .map {
                                DatabaseSearchSuggestion.create(it.user().urn(),
                                                                it.user().username(),
                                                                it.user().avatarUrl(),
                                                                it.user().isPro,
                                                                DatabaseSearchSuggestion.Kind.Following)
                            }
                }
    }

    private fun getLoggedInUser(searchQuery: String, urn: Urn): Single<List<DatabaseSearchSuggestion>> {
        return userStorage.
                loadUsers(listOf(urn))
                .map { users ->
                    users
                            .filter { it.username().contains(searchQuery, ignoreCase = true) }
                            .map {
                                DatabaseSearchSuggestion.create(it.urn(),
                                                                it.username(),
                                                                it.avatarUrl(),
                                                                it.isPro,
                                                                DatabaseSearchSuggestion.Kind.Following)
                            }
                }
    }

    private fun getPostedTracksWithTitleLike(searchQuery: String): Single<List<DatabaseSearchSuggestion>> {
        return getFilteredTrackSuggestions(postsStorage.loadPostedTracksSortedByDateDesc(),
                                           searchQuery,
                                           { it.title().contains(searchQuery, ignoreCase = true) },
                                           { track, kind -> trackMapperByTitle(track, kind) },
                                           DatabaseSearchSuggestion.Kind.Post)
    }

    private fun getLikedTracksWithTitleLike(likedTracks: Single<List<Association>>, searchQuery: String): Single<List<DatabaseSearchSuggestion>> {
        return getFilteredTrackSuggestions(likedTracks,
                                           searchQuery,
                                           { it.title().contains(searchQuery, ignoreCase = true) },
                                           { track, kind -> trackMapperByTitle(track, kind) },
                                           DatabaseSearchSuggestion.Kind.Like)
    }

    private fun getLikedTracksWithCreatorLike(likedTracks: Single<List<Association>>, searchQuery: String): Single<List<DatabaseSearchSuggestion>> {
        return getFilteredTrackSuggestions(likedTracks,
                                           searchQuery,
                                           { it.creatorName().contains(searchQuery, ignoreCase = true) },
                                           { track, kind -> trackMapperByCreatorName(track, kind) },
                                           DatabaseSearchSuggestion.Kind.LikeByUsername)
    }

    private fun getPostedPlaylistsWithTitleLike(searchQuery: String, limit: Int): Single<List<DatabaseSearchSuggestion>> {
        return getFilteredPlaylistSuggestions(playlistPostsStorage.loadPostedPlaylists(limit),
                                              searchQuery,
                                              { it.title().contains(searchQuery, ignoreCase = true) },
                                              { playlist, kind -> playlistMapperByTitle(playlist, kind) },
                                              DatabaseSearchSuggestion.Kind.Post)
    }

    private fun getLikedPlaylistsWithTitleLike(likedPlaylists: Single<List<Association>>, searchQuery: String): Single<List<DatabaseSearchSuggestion>> {
        return getFilteredPlaylistSuggestions(likedPlaylists,
                                              searchQuery,
                                              { it.title().contains(searchQuery, ignoreCase = true) },
                                              { playlist, kind -> playlistMapperByTitle(playlist, kind) },
                                              DatabaseSearchSuggestion.Kind.Like)
    }

    private fun getLikedPlaylistsWithCreatorLike(likedPlaylists: Single<List<Association>>, searchQuery: String): Single<List<DatabaseSearchSuggestion>> {
        return getFilteredPlaylistSuggestions(likedPlaylists,
                                              searchQuery,
                                              { it.creatorName().contains(searchQuery, ignoreCase = true) },
                                              { playlist, kind -> playlistMapperByCreatorName(playlist, kind) },
                                              DatabaseSearchSuggestion.Kind.LikeByUsername)
    }

    private fun getFilteredTrackSuggestions(input: Single<List<Association>>,
                                            searchQuery: String,
                                            filter: (Track) -> Boolean,
                                            mapper: (Track, DatabaseSearchSuggestion.Kind) -> DatabaseSearchSuggestion,
                                            kind: DatabaseSearchSuggestion.Kind): Single<List<DatabaseSearchSuggestion>> {
        return input
                .flatMap { source ->
                    enrichItemsWithProperties(source,
                                              trackRepository.fromUrns(Lists.transform(source, UrnHolder::urn)),
                                              BiFunction { track: Track, association: Association -> track to association })
                }
                .map { list -> filterSortAndConvertTracks(list, searchQuery, filter, mapper, kind) }
    }

    private fun filterSortAndConvertTracks(input: List<Pair<Track, Association>>,
                                           searchQuery: String,
                                           filter: (Track) -> Boolean,
                                           mapper: (Track, DatabaseSearchSuggestion.Kind) -> DatabaseSearchSuggestion,
                                           kind: DatabaseSearchSuggestion.Kind): List<DatabaseSearchSuggestion> {
        return input
                .filter { filter(it.first) }
                .sortedByDescending { it.second.createdAt }
                .map { (track) -> mapper(track, kind) }
    }

    private fun trackMapperByTitle(track: Track, kind: DatabaseSearchSuggestion.Kind): DatabaseSearchSuggestion {
        return DatabaseSearchSuggestion.create(track.urn(),
                                               track.title(),
                                               track.imageUrlTemplate(),
                                               track.creatorIsPro(),
                                               kind)
    }

    private fun trackMapperByCreatorName(track: Track, kind: DatabaseSearchSuggestion.Kind): DatabaseSearchSuggestion {
        return DatabaseSearchSuggestion.create(track.urn(),
                                               "${track.creatorName()} - ${track.title()}",
                                               track.imageUrlTemplate(),
                                               track.creatorIsPro(),
                                               kind)
    }

    private fun getFilteredPlaylistSuggestions(input: Single<List<Association>>,
                                               searchQuery: String,
                                               filter: (Playlist) -> Boolean,
                                               mapper: (Playlist, DatabaseSearchSuggestion.Kind) -> DatabaseSearchSuggestion,
                                               kind: DatabaseSearchSuggestion.Kind): Single<List<DatabaseSearchSuggestion>> {
        return input
                .flatMap { source ->
                    enrichItemsWithProperties(source,
                                              playlistRepository.withUrns(Lists.transform(source, UrnHolder::urn)),
                                              BiFunction { playlist: Playlist, association: Association -> playlist to association })
                }
                .map { list -> filterSortAndConvertPlaylists(list, searchQuery, filter, mapper, kind) }
    }

    private fun filterSortAndConvertPlaylists(input: List<Pair<Playlist, Association>>,
                                              searchQuery: String,
                                              filter: (Playlist) -> Boolean,
                                              mapper: (Playlist, DatabaseSearchSuggestion.Kind) -> DatabaseSearchSuggestion,
                                              kind: DatabaseSearchSuggestion.Kind): List<DatabaseSearchSuggestion> {
        return input
                .filter { filter(it.first) }
                .sortedByDescending { it.second.createdAt }
                .map { (playlist) -> mapper(playlist, kind) }
    }

    private fun playlistMapperByTitle(playlist: Playlist, kind: DatabaseSearchSuggestion.Kind): DatabaseSearchSuggestion {
        return DatabaseSearchSuggestion.create(playlist.urn(),
                                               playlist.title(),
                                               playlist.imageUrlTemplate(),
                                               playlist.creatorIsPro(),
                                               kind)
    }

    private fun playlistMapperByCreatorName(playlist: Playlist, kind: DatabaseSearchSuggestion.Kind): DatabaseSearchSuggestion {
        return DatabaseSearchSuggestion.create(playlist.urn(),
                                               "${playlist.creatorName()} - ${playlist.title()}",
                                               playlist.imageUrlTemplate(),
                                               playlist.creatorIsPro(),
                                               kind)
    }

    private fun removeDuplicates(items: List<DatabaseSearchSuggestion>): List<SearchSuggestion> {
        val duplicatesToRemove = items.groupBy { it.urn }.filter { it.value.size > 1 }.values.flatten().filter { DatabaseSearchSuggestion.Kind.LikeByUsername == it.kind() }
        return items.filter { !duplicatesToRemove.contains(it) }
    }
}
