package com.soundcloud.android.search;

import static com.soundcloud.android.search.SearchResultsAdapter.TYPE_PLAYLIST;
import static com.soundcloud.android.search.SearchResultsAdapter.TYPE_PREMIUM_CONTENT;
import static com.soundcloud.android.search.SearchResultsAdapter.TYPE_TRACK;
import static com.soundcloud.android.search.SearchResultsAdapter.TYPE_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.view.adapters.FollowableUserItemRenderer;
import com.soundcloud.android.view.adapters.PlaylistItemRenderer;
import com.soundcloud.java.collections.PropertySet;
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
    @Mock private ViewGroup itemView;
    @Mock private Fragment fragment;

    @Captor private ArgumentCaptor<List<PlaylistItem>> playlistItemCaptor;

    private SearchResultsAdapter adapter;

    @Before
    public void setup() {
        when(featureFlags.isEnabled(Flag.SEARCH_RESULTS_HIGH_TIER)).thenReturn(false);
        adapter = new SearchResultsAdapter(trackRenderer, playlistRenderer, userRenderer, premiumContentRenderer);
    }

    @Test
    public void shouldDifferentiateItemViewTypesForUniversalSearchResult() {
        adapter.addItem(dummyUserItem());
        adapter.addItem(dummyTrackItem());
        adapter.addItem(dummyPlaylistItem());
        adapter.addItem(dummySearchPremiumItem());

        assertThat(adapter.getItemViewType(0)).isEqualTo(TYPE_USER);
        assertThat(adapter.getItemViewType(1)).isEqualTo(TYPE_TRACK);
        assertThat(adapter.getItemViewType(2)).isEqualTo(TYPE_PLAYLIST);
        assertThat(adapter.getItemViewType(3)).isEqualTo(TYPE_PREMIUM_CONTENT);
    }

    @Test
    public void shouldDifferentiateItemViewTypesForDifferentResultTypes() {
        adapter.addItem(dummyUserItem());
        adapter.addItem(dummyTrackItem());
        adapter.addItem(dummyPlaylistItem());
        adapter.addItem(dummySearchPremiumItem());

        assertThat(adapter.getItemViewType(0)).isEqualTo(TYPE_USER);
        assertThat(adapter.getItemViewType(1)).isEqualTo(TYPE_TRACK);
        assertThat(adapter.getItemViewType(2)).isEqualTo(TYPE_PLAYLIST);
        assertThat(adapter.getItemViewType(3)).isEqualTo(TYPE_PREMIUM_CONTENT);
    }

    private UserItem dummyUserItem() {
        return UserItem.from(ApiUniversalSearchItem.forUser(ModelFixtures.create(ApiUser.class)).toPropertySet());
    }

    private TrackItem dummyTrackItem() {
        return TrackItem.from(ApiUniversalSearchItem.forTrack(ModelFixtures.create(ApiTrack.class)).toPropertySet());
    }

    private PlaylistItem dummyPlaylistItem() {
        return PlaylistItem.from(ApiUniversalSearchItem.forPlaylist(ModelFixtures.create(ApiPlaylist.class)).toPropertySet());
    }

    private SearchPremiumItem dummySearchPremiumItem() {
        return new SearchPremiumItem(Collections.<PropertySet>emptyList(), Optional.<Link>absent(), SEARCH_RESULTS_COUNT);
    }
}
