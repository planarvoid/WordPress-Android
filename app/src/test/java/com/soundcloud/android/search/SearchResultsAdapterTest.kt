package com.soundcloud.android.search

import com.soundcloud.android.model.Urn
import com.soundcloud.android.playlists.PlaylistItem
import com.soundcloud.android.search.SearchResultsAdapter.Kind.TYPE_HEADER
import com.soundcloud.android.search.SearchResultsAdapter.Kind.TYPE_PLAYLIST
import com.soundcloud.android.search.SearchResultsAdapter.Kind.TYPE_PREMIUM_CONTENT
import com.soundcloud.android.search.SearchResultsAdapter.Kind.TYPE_TRACK
import com.soundcloud.android.search.SearchResultsAdapter.Kind.TYPE_UPSELL
import com.soundcloud.android.search.SearchResultsAdapter.Kind.TYPE_USER
import com.soundcloud.android.testsupport.PlaylistFixtures
import com.soundcloud.android.testsupport.TrackFixtures
import com.soundcloud.android.testsupport.UserFixtures
import com.soundcloud.android.testsupport.fixtures.ModelFixtures
import com.soundcloud.android.tracks.TrackItem
import com.soundcloud.android.users.UserItem
import com.soundcloud.java.optional.Optional
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SearchResultsAdapterTest {

    @Mock private lateinit var userRenderer: SearchUserItemRenderer
    @Mock private lateinit var trackRenderer: SearchTrackItemRenderer
    @Mock private lateinit var playlistRenderer: SearchPlaylistItemRenderer
    @Mock private lateinit var premiumContentRenderer: SearchPremiumContentRenderer
    @Mock private lateinit var searchUpsellRenderer: SearchUpsellRenderer
    @Mock private lateinit var searchResultHeaderRenderer: SearchResultHeaderRenderer

    private lateinit var adapter: SearchResultsAdapter

    @Before
    fun setup() {
        adapter = SearchResultsAdapter(trackRenderer,
                                       playlistRenderer,
                                       userRenderer,
                                       premiumContentRenderer,
                                       searchUpsellRenderer,
                                       searchResultHeaderRenderer)
    }

    @Test
    fun shouldDifferentiateItemViewTypesForUniversalSearchResult() {
        adapter.addItem(dummyUserItem())
        adapter.addItem(dummyTrackItem())
        adapter.addItem(dummyPlaylistItem())
        adapter.addItem(dummySearchPremiumItem())
        adapter.addItem(dummySearchUpsellItem())
        adapter.addItem(dummySearchHeaderItem())

        assertThat(adapter.getItemViewType(0)).isEqualTo(TYPE_USER.ordinal)
        assertThat(adapter.getItemViewType(1)).isEqualTo(TYPE_TRACK.ordinal)
        assertThat(adapter.getItemViewType(2)).isEqualTo(TYPE_PLAYLIST.ordinal)
        assertThat(adapter.getItemViewType(3)).isEqualTo(TYPE_PREMIUM_CONTENT.ordinal)
        assertThat(adapter.getItemViewType(4)).isEqualTo(TYPE_UPSELL.ordinal)
        assertThat(adapter.getItemViewType(5)).isEqualTo(TYPE_HEADER.ordinal)
    }

    @Test
    fun shouldDifferentiateItemViewTypesForDifferentResultTypes() {
        adapter.addItem(dummyUserItem())
        adapter.addItem(dummyTrackItem())
        adapter.addItem(dummyPlaylistItem())
        adapter.addItem(dummySearchPremiumItem())
        adapter.addItem(dummySearchUpsellItem())
        adapter.addItem(dummySearchHeaderItem())

        assertThat(adapter.getItemViewType(0)).isEqualTo(TYPE_USER.ordinal)
        assertThat(adapter.getItemViewType(1)).isEqualTo(TYPE_TRACK.ordinal)
        assertThat(adapter.getItemViewType(2)).isEqualTo(TYPE_PLAYLIST.ordinal)
        assertThat(adapter.getItemViewType(3)).isEqualTo(TYPE_PREMIUM_CONTENT.ordinal)
        assertThat(adapter.getItemViewType(4)).isEqualTo(TYPE_UPSELL.ordinal)
        assertThat(adapter.getItemViewType(5)).isEqualTo(TYPE_HEADER.ordinal)
    }

    @Test
    fun shouldFilterUpsellItemWhenGettingResultItems() {
        val userItem = dummyUserItem()
        val trackItem = dummyTrackItem()
        val playlistItem = dummyPlaylistItem()

        adapter.addItem(dummySearchUpsellItem())
        adapter.addItem(userItem)
        adapter.addItem(trackItem)
        adapter.addItem(playlistItem)

        assertThat(adapter.getResultItems()).containsExactly(userItem, trackItem, playlistItem)
    }

    @Test
    fun shouldFilterPremiumContentItemWhenGettingResultItems() {
        val userItem = dummyUserItem()
        val trackItem = dummyTrackItem()
        val playlistItem = dummyPlaylistItem()

        adapter.addItem(dummySearchPremiumItem())
        adapter.addItem(userItem)
        adapter.addItem(trackItem)
        adapter.addItem(playlistItem)

        assertThat(adapter.getResultItems()).containsExactly(userItem, trackItem, playlistItem)
    }

    private fun dummyUserItem(): SearchUserItem {
        val userItem = ModelFixtures.listItemFromSearchItem(ApiUniversalSearchItem(UserFixtures.apiUser(), null, null)) as UserItem
        return SearchUserItem(userItem, Optional.absent())
    }

    private fun dummyTrackItem(): SearchTrackItem {
        val trackItem = ModelFixtures.listItemFromSearchItem(ApiUniversalSearchItem(null, null, TrackFixtures.apiTrack())) as TrackItem
        return SearchTrackItem(trackItem, Optional.absent())
    }

    private fun dummyPlaylistItem(): SearchPlaylistItem {
        val playlistItem = ModelFixtures.listItemFromSearchItem(ApiUniversalSearchItem(null, PlaylistFixtures.apiPlaylist(), null)) as PlaylistItem
        return SearchPlaylistItem(playlistItem, Optional.absent())
    }

    private fun dummySearchPremiumItem(): SearchPremiumItem {
        val trackItem = TrackFixtures.trackItem(Urn.forTrack(123L))
        return SearchPremiumItem(listOf(trackItem),
                                 Optional.absent(),
                                 SEARCH_RESULTS_COUNT)
    }

    private fun dummySearchUpsellItem(): UpsellSearchableItem {
        return UpsellSearchableItem()
    }

    private fun dummySearchHeaderItem(): SearchResultHeaderRenderer.SearchResultHeader {
        return SearchResultHeaderRenderer.SearchResultHeader.create(SearchType.TRACKS, SearchOperations.ContentType.PREMIUM, 0)
    }

    companion object {

        private val SEARCH_RESULTS_COUNT = 100
    }
}
