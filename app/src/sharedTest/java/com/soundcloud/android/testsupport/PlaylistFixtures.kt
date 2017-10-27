package com.soundcloud.android.testsupport

import com.soundcloud.android.api.legacy.model.PlaylistStats
import com.soundcloud.android.api.model.ApiPlaylist
import com.soundcloud.android.api.model.Sharing
import com.soundcloud.android.model.Urn
import com.soundcloud.android.offline.OfflineProperties
import com.soundcloud.android.playlists.Playlist
import com.soundcloud.android.playlists.PlaylistItem
import com.soundcloud.java.optional.Optional
import com.soundcloud.java.strings.Strings
import java.util.Date

object PlaylistFixtures {

    private var runningId = 1L

    @JvmField val IMAGE_URL_TEMPLATE = Optional.of("https://i1.sndcdn.com/artworks-000151307749-v2r7oy-{size}.jpg")
    @JvmField val REPOSTS_COUNT = 4
    @JvmField val LIKES_COUNT = 5
    @JvmField val DURATION = 12345
    @JvmField var TAGS = listOf("tag1", "tag2", "tag3")
    @JvmField var TRACK_COUNT = 2
    @JvmField var SHARING = Sharing.PUBLIC
    @JvmField var GENRE = "clownstep"
    @JvmField var SET_TYPE = Strings.EMPTY
    @JvmField var RELEASE_DATE = Strings.EMPTY

    @JvmStatic
    fun apiPlaylist(): ApiPlaylist = apiPlaylistBuilder().build()

    @JvmStatic
    fun apiPlaylist(urn: Urn): ApiPlaylist = apiPlaylistBuilder(urn).build()

    @JvmStatic
    fun apiPlaylists(count: Int) = List(count, { apiPlaylist() })

    @JvmStatic
    fun nextPlaylistUrn(): Urn = Urn.forPlaylist(runningId++)

    @JvmStatic
    fun apiPlaylistBuilder(): ApiPlaylist.Builder = apiPlaylistBuilder(nextPlaylistUrn())

    @JvmStatic
    fun apiPlaylistBuilder(urn: Urn = nextPlaylistUrn()): ApiPlaylist.Builder {
        val title = "playlist " + urn.numericId
        return ApiPlaylist.builder()
                .urn(urn)
                .title(title)
                .imageUrlTemplate(IMAGE_URL_TEMPLATE)
                .user(UserFixtures.apiUser())
                .genre(GENRE)
                .tags(TAGS)
                .trackCount(TRACK_COUNT)
                .stats(PlaylistStats.create(REPOSTS_COUNT, LIKES_COUNT))
                .duration(DURATION.toLong())
                .sharing(SHARING)
                .permalinkUrl("https://soundcloud.com/marc/sets/mahsongs" + title)
                .createdAt(Date())
                .album(false)
                .setType(SET_TYPE)
                .releaseDate(RELEASE_DATE)
    }

    @JvmStatic
    fun playlistBuilder(): Playlist.Builder = playlistBuilder(PlaylistFixtures.apiPlaylist())

    @JvmStatic
    fun playlistBuilder(apiPlaylist: ApiPlaylist): Playlist.Builder = Playlist.from(apiPlaylist).toBuilder()

    @JvmStatic
    fun playlistBuilder(playlist: Playlist): Playlist.Builder = playlist.toBuilder()

    @JvmStatic
    fun playlist(): Playlist = PlaylistFixtures.playlistBuilder().trackCount(0).build()

    @JvmStatic
    fun playlist(urn: Urn): Playlist = PlaylistFixtures.playlistBuilder().urn(urn).build()

    @JvmStatic
    fun playlistItems(count: Int): List<PlaylistItem> = List(count, { playlistItem() })

    @JvmStatic
    fun playlistItems(apiPlaylists: List<ApiPlaylist>): List<PlaylistItem> = apiPlaylists.map { playlistItem(it) }

    @JvmStatic
    fun playlistItem(): PlaylistItem = playlistItem(playlist())

    @JvmStatic
    fun playlistItemBuilder(): PlaylistItem.Builder = playlistItemBuilder(playlist())

    @JvmStatic
    fun playlistItemBuilder(playlist: Playlist): PlaylistItem.Builder = PlaylistItem.builder(playlist, OfflineProperties())

    @JvmStatic
    fun playlistItem(apilaylist: ApiPlaylist): PlaylistItem = playlistItem(Playlist.from(apilaylist))

    @JvmStatic
    fun playlistItem(playlist: Playlist): PlaylistItem = playlistItemBuilder(playlist, OfflineProperties()).build()

    @JvmStatic
    fun playlistItemBuilder(playlist: Playlist, offlineProperties: OfflineProperties): PlaylistItem.Builder = PlaylistItem.builder(playlist, offlineProperties)

    @JvmStatic
    fun playlistItem(urn: Urn): PlaylistItem = playlistItem(PlaylistFixtures.playlist(urn))

}
