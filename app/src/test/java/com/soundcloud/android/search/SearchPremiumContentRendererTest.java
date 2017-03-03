package com.soundcloud.android.search;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.view.adapters.PlaylistItemRenderer;
import com.soundcloud.android.view.adapters.UserItemRenderer;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SearchPremiumContentRendererTest extends AndroidUnitTest {

    private SearchPremiumContentRenderer renderer;

    @Mock private TrackItemRenderer trackRenderer;
    @Mock private PlaylistItemRenderer playlistRenderer;
    @Mock private UserItemRenderer userRenderer;
    @Mock private FeatureOperations featureOperations;
    @Mock private FeatureFlags flags;

    private View premiumItemView;
    private View trackItemView;
    private View playListItemView;
    private View userItemView;
    private View viewAllResultsView;
    private CondensedNumberFormatter numberFormatter;

    @Before
    public void setUp() {
        final FrameLayout frameLayout = new FrameLayout(context());
        premiumItemView = LayoutInflater.from(context()).inflate(R.layout.search_premium_item, frameLayout, false);
        trackItemView = LayoutInflater.from(context()).inflate(R.layout.track_list_item, frameLayout, false);
        playListItemView = LayoutInflater.from(context()).inflate(R.layout.playlist_list_item, frameLayout, false);
        userItemView = LayoutInflater.from(context()).inflate(R.layout.user_list_item, frameLayout, false);
        viewAllResultsView = premiumItemView.findViewById(R.id.view_all_container);

        when(trackRenderer.createItemView(any(ViewGroup.class))).thenReturn(trackItemView);
        when(playlistRenderer.createItemView(any(ViewGroup.class))).thenReturn(playListItemView);
        when(userRenderer.createItemView(any(ViewGroup.class))).thenReturn(userItemView);

        numberFormatter = CondensedNumberFormatter.create(Locale.getDefault(), resources());
        renderer = new SearchPremiumContentRenderer(trackRenderer,
                                                    playlistRenderer,
                                                    userRenderer,
                                                    resources(),
                                                    numberFormatter,
                                                    featureOperations);
    }

    @Test
    public void shouldHideViewsAfterCreation() {
        renderer.createItemView(new FrameLayout(context()));

        assertThat(trackItemView.getVisibility()).isEqualTo(GONE);
        assertThat(playListItemView.getVisibility()).isEqualTo(GONE);
        assertThat(userItemView.getVisibility()).isEqualTo(GONE);

        verify(trackRenderer).createItemView(any(ViewGroup.class));
        verify(playlistRenderer).createItemView(any(ViewGroup.class));
        verify(userRenderer).createItemView(any(ViewGroup.class));
    }

    @Test
    public void shouldBindItemViewTrack() {
        renderer.createItemView(new FrameLayout(context()));
        renderer.bindItemView(0, premiumItemView, buildSearchPremiumItem(Urn.forTrack(123L)));

        assertThat(trackItemView.getVisibility()).isEqualTo(VISIBLE);
        assertThat(trackItemView.hasOnClickListeners()).isTrue();
        assertThat(playListItemView.getVisibility()).isEqualTo(GONE);
        assertThat(userItemView.getVisibility()).isEqualTo(GONE);
        verify(trackRenderer).bindItemView(eq(0), eq(trackItemView), anyList());
    }

    @Test
    public void shouldBindItemViewPlaylist() {
        renderer.createItemView(new FrameLayout(context()));
        renderer.bindItemView(0, premiumItemView, buildSearchPremiumItem(Urn.forPlaylist(123L)));

        assertThat(playListItemView.getVisibility()).isEqualTo(VISIBLE);
        assertThat(playListItemView.hasOnClickListeners()).isTrue();
        assertThat(trackItemView.getVisibility()).isEqualTo(GONE);
        assertThat(userItemView.getVisibility()).isEqualTo(GONE);
        TextView resultsCountTextView = (TextView) premiumItemView.findViewById(R.id.results_count);
        assertThat(resultsCountTextView.getText()).contains(numberFormatter.format(1));
        verify(playlistRenderer).bindItemView(eq(0), eq(playListItemView), anyList());
    }

    @Test
    public void showResultWithLotsOfItems() {
        renderer.createItemView(new FrameLayout(context()));
        renderer.bindItemView(0, premiumItemView, buildSearchPremiumItemWithManyResults(10000));

        TextView resultsCountTextView = (TextView) premiumItemView.findViewById(R.id.results_count);
        assertThat(resultsCountTextView.getText()).contains(numberFormatter.format(10000));
    }

    @Test
    public void shouldBindItemViewUser() {
        renderer.createItemView(new FrameLayout(context()));
        renderer.bindItemView(0, premiumItemView, buildSearchPremiumItem(Urn.forUser(123L)));

        assertThat(userItemView.getVisibility()).isEqualTo(VISIBLE);
        assertThat(userItemView.hasOnClickListeners()).isTrue();
        assertThat(playListItemView.getVisibility()).isEqualTo(GONE);
        assertThat(trackItemView.getVisibility()).isEqualTo(GONE);
        verify(userRenderer).bindItemView(eq(0), eq(userItemView), anyList());
    }

    @Test
    public void shouldShowUpsellIconWhenNotHighTierUser() {
        final View helpItemView = premiumItemView.findViewById(R.id.help);
        when(featureOperations.upsellHighTier()).thenReturn(true);

        renderer.createItemView(new FrameLayout(context()));
        renderer.bindItemView(0, premiumItemView, buildSearchPremiumItem(Urn.forTrack(123L), Urn.forTrack(234L)));

        assertThat(helpItemView.getVisibility()).isEqualTo(VISIBLE);
        assertThat(helpItemView.hasOnClickListeners()).isTrue();
    }

    @Test
    public void shouldShowViewAllButtonWhenHavingMoreThanOneHighTierResult() {
        when(featureOperations.upsellHighTier()).thenReturn(true);

        renderer.createItemView(new FrameLayout(context()));
        renderer.bindItemView(0, premiumItemView, buildSearchPremiumItem(Urn.forTrack(123L), Urn.forTrack(234L)));

        assertThat(viewAllResultsView.getVisibility()).isEqualTo(VISIBLE);
        assertThat(viewAllResultsView.hasOnClickListeners()).isTrue();
    }

    @Test
    public void shouldNotShowViewAllButtonWhenHavingOneHighTierResult() {
        when(featureOperations.upsellHighTier()).thenReturn(true);

        renderer.createItemView(new FrameLayout(context()));
        renderer.bindItemView(0, premiumItemView, buildSearchPremiumItem(Urn.forTrack(123L)));

        assertThat(viewAllResultsView.getVisibility()).isEqualTo(GONE);
        assertThat(viewAllResultsView.hasOnClickListeners()).isFalse();
    }

    @Test
    public void shouldNotShowUpsellIconWhenHighTierUser() {
        final View helpItemView = premiumItemView.findViewById(R.id.help);
        when(featureOperations.upsellHighTier()).thenReturn(false);

        renderer.createItemView(new FrameLayout(context()));
        renderer.bindItemView(0, premiumItemView, buildSearchPremiumItem(Urn.forTrack(123L), Urn.forTrack(234L)));

        assertThat(helpItemView.getVisibility()).isEqualTo(GONE);
        assertThat(helpItemView.hasOnClickListeners()).isFalse();
    }

    private List<SearchPremiumItem> buildSearchPremiumItemWithManyResults(int itemCount) {
        final List<ListItem> searchableItems = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            searchableItems.add(ModelFixtures.trackItem(Urn.forTrack(i)));
        }
        return Collections.singletonList(new SearchPremiumItem(searchableItems, Optional.absent(), itemCount));
    }

    private List<SearchPremiumItem> buildSearchPremiumItem(Urn... urns) {
        final List<ListItem> searchableItems = new ArrayList<>(urns.length);
        for (Urn urn : urns) {
            if (urn.isTrack()) {
                searchableItems.add(ModelFixtures.trackItem(urn));
            } else if (urn.isPlaylist()) {
                searchableItems.add(ModelFixtures.playlistItem(urn));
            } else if (urn.isUser()) {
                searchableItems.add(ModelFixtures.create(UserItem.class).copyWithUrn(urn));
            }
        }
        return Collections.singletonList(new SearchPremiumItem(searchableItems, Optional.absent(), urns.length));
    }
}
