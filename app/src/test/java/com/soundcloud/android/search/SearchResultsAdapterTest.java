package com.soundcloud.android.search;

import static com.soundcloud.android.search.SearchResultsAdapter.Kind.TYPE_HEADER;
import static com.soundcloud.android.search.SearchResultsAdapter.Kind.TYPE_PLAYLIST;
import static com.soundcloud.android.search.SearchResultsAdapter.Kind.TYPE_PREMIUM_CONTENT;
import static com.soundcloud.android.search.SearchResultsAdapter.Kind.TYPE_TRACK;
import static com.soundcloud.android.search.SearchResultsAdapter.Kind.TYPE_UPSELL;
import static com.soundcloud.android.search.SearchResultsAdapter.Kind.TYPE_USER;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.view.adapters.FollowableUserItemRenderer;
import com.soundcloud.android.view.adapters.PlaylistItemRenderer;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.support.v4.app.Fragment;
import android.view.ViewGroup;

import java.util.Collections;
import java.util.List;

public class SearchResultsAdapterTest extends AndroidUnitTest {

    private static final int SEARCH_RESULTS_COUNT = 100;

    @Mock private FeatureFlags featureFlags;
    @Mock private FollowableUserItemRenderer userRenderer;
    @Mock private TrackItemRenderer trackRenderer;
    @Mock private PlaylistItemRenderer playlistRenderer;
    @Mock private SearchPremiumContentRenderer premiumContentRenderer;
    @Mock private SearchUpsellRenderer searchUpsellRenderer;
    @Mock private ViewGroup itemView;
    @Mock private Fragment fragment;
    @Mock private SearchResultHeaderRenderer searchResultHeaderRenderer;

    @Captor private ArgumentCaptor<List<PlaylistItem>> playlistItemCaptor;
    private SearchResultsAdapter adapter;

    @Before
    public void setup() {
        adapter = new SearchResultsAdapter(trackRenderer,
                                           playlistRenderer,
                                           userRenderer,
                                           premiumContentRenderer,
                                           searchUpsellRenderer,
                                           searchResultHeaderRenderer);
    }

    @Test
    public void shouldDifferentiateItemViewTypesForUniversalSearchResult() {
        adapter.addItem(dummyUserItem());
        adapter.addItem(dummyTrackItem());
        adapter.addItem(dummyPlaylistItem());
        adapter.addItem(dummySearchPremiumItem());
        adapter.addItem(dummySearchUpsellItem());
        adapter.addItem(dummySearchHeaderItem());

        assertThat(adapter.getItemViewType(0)).isEqualTo(TYPE_USER.ordinal());
        assertThat(adapter.getItemViewType(1)).isEqualTo(TYPE_TRACK.ordinal());
        assertThat(adapter.getItemViewType(2)).isEqualTo(TYPE_PLAYLIST.ordinal());
        assertThat(adapter.getItemViewType(3)).isEqualTo(TYPE_PREMIUM_CONTENT.ordinal());
        assertThat(adapter.getItemViewType(4)).isEqualTo(TYPE_UPSELL.ordinal());
        assertThat(adapter.getItemViewType(5)).isEqualTo(TYPE_HEADER.ordinal());
    }

    @Test
    public void shouldDifferentiateItemViewTypesForDifferentResultTypes() {
        adapter.addItem(dummyUserItem());
        adapter.addItem(dummyTrackItem());
        adapter.addItem(dummyPlaylistItem());
        adapter.addItem(dummySearchPremiumItem());
        adapter.addItem(dummySearchUpsellItem());
        adapter.addItem(dummySearchHeaderItem());

        assertThat(adapter.getItemViewType(0)).isEqualTo(TYPE_USER.ordinal());
        assertThat(adapter.getItemViewType(1)).isEqualTo(TYPE_TRACK.ordinal());
        assertThat(adapter.getItemViewType(2)).isEqualTo(TYPE_PLAYLIST.ordinal());
        assertThat(adapter.getItemViewType(3)).isEqualTo(TYPE_PREMIUM_CONTENT.ordinal());
        assertThat(adapter.getItemViewType(4)).isEqualTo(TYPE_UPSELL.ordinal());
        assertThat(adapter.getItemViewType(5)).isEqualTo(TYPE_HEADER.ordinal());
    }

    @Test
    public void shouldFilterUpsellItemWhenGettingResultItems() {
        final UserItem userItem = dummyUserItem();
        final TrackItem trackItem = dummyTrackItem();
        final PlaylistItem playlistItem = dummyPlaylistItem();

        adapter.addItem(dummySearchUpsellItem());
        adapter.addItem(userItem);
        adapter.addItem(trackItem);
        adapter.addItem(playlistItem);

        assertThat(adapter.getResultItems()).containsExactly(userItem, trackItem, playlistItem);
    }

    @Test
    public void shouldFilterPremiumContentItemWhenGettingResultItems() {
        final UserItem userItem = dummyUserItem();
        final TrackItem trackItem = dummyTrackItem();
        final PlaylistItem playlistItem = dummyPlaylistItem();

        adapter.addItem(dummySearchPremiumItem());
        adapter.addItem(userItem);
        adapter.addItem(trackItem);
        adapter.addItem(playlistItem);

        assertThat(adapter.getResultItems()).containsExactly(userItem, trackItem, playlistItem);
    }

    private UserItem dummyUserItem() {
        return (UserItem) ModelFixtures.listItemFromSearchItem(new ApiUniversalSearchItem(ModelFixtures.create(ApiUser.class), null, null));
    }

    private TrackItem dummyTrackItem() {
        return (TrackItem) ModelFixtures.listItemFromSearchItem(new ApiUniversalSearchItem(null, null, ModelFixtures.create(ApiTrack.class)));
    }

    private PlaylistItem dummyPlaylistItem() {
        return (PlaylistItem) ModelFixtures.listItemFromSearchItem(new ApiUniversalSearchItem(null, ModelFixtures.create(ApiPlaylist.class), null));
    }

    private SearchPremiumItem dummySearchPremiumItem() {
        final TrackItem trackItem = ModelFixtures.trackItem(Urn.forTrack(123L));
        return new SearchPremiumItem(Collections.singletonList(trackItem),
                                     Optional.<Link>absent(),
                                     SEARCH_RESULTS_COUNT);
    }

    private UpsellSearchableItem dummySearchUpsellItem() {
        return new UpsellSearchableItem();
    }

    private SearchResultHeaderRenderer.SearchResultHeader dummySearchHeaderItem() {
        return SearchResultHeaderRenderer.SearchResultHeader.create(SearchType.TRACKS, SearchOperations.ContentType.PREMIUM, 0);
    }
}
