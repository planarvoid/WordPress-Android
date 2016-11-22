package com.soundcloud.android.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.os.Bundle;

import java.util.Collections;
import java.util.List;

public class SearchPremiumResultsPresenterTest extends AndroidUnitTest {

    private static final Urn PREMIUM_TRACK_URN_ONE = Urn.forTrack(1L);
    private static final Urn PREMIUM_TRACK_URN_TWO = Urn.forTrack(2L);
    private static final Urn TRACK_URN = Urn.forTrack(3L);
    private static final Urn QUERY_URN = Urn.forUser(3L);

    private SearchPremiumResultsPresenter presenter;

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private SearchOperations searchOperations;
    @Mock private SearchResultsAdapter adapter;
    @Mock private SearchOperations.SearchPagingFunction searchPagingFunction;
    @Mock private MixedItemClickListener.Factory clickListenerFactory;
    @Mock private MixedItemClickListener clickListener;
    @Mock private FeatureOperations featureOperations;
    @Mock private Navigator navigator;
    @Mock private SearchTracker searchTracker;

    private TestEventBus eventBus = new TestEventBus();

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.default_recyclerview_with_refresh,
                                                                    new Bundle());

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        final List<PropertySet> propertySets =
                Collections.singletonList(PropertySet.create()
                                                     .put(EntityProperty.URN, PREMIUM_TRACK_URN_ONE)
                                                     .put(EntityProperty.URN, PREMIUM_TRACK_URN_TWO));
        final SearchResult searchResult = SearchResult.fromPropertySets(propertySets,
                                                                        Optional.<Link>absent(),
                                                                        QUERY_URN);
        final Observable<SearchResult> searchResultObservable = Observable.just(searchResult);

        presenter = new SearchPremiumResultsPresenter(swipeRefreshAttacher,
                                                      searchOperations,
                                                      adapter,
                                                      clickListenerFactory,
                                                      featureOperations,
                                                      navigator,
                                                      eventBus,
                                                      searchTracker);

        when(clickListenerFactory.create(any(Screen.class),
                                         any(SearchQuerySourceInfo.class))).thenReturn(clickListener);
        when(searchOperations.searchPremiumResultFrom(any(List.class), any(Optional.class), any(Urn.class))).thenReturn(
                searchResultObservable);
        when(searchOperations.searchPremiumResult(anyString(),
                                                  any(SearchType.class))).thenReturn(searchResultObservable);
        when(searchOperations.pagingFunction(any(SearchType.class))).thenReturn(searchPagingFunction);
    }

    @Test
    public void itemClickDelegatesToClickListener() {
        final List<ListItem> listItems = setupAdapter();
        when(clickListenerFactory.create(Screen.SEARCH_PREMIUM_CONTENT, null)).thenReturn(clickListener);

        presenter.onBuildBinding(new Bundle());
        presenter.onItemClicked(fragmentRule.getView(), 0);

        verify(clickListener).onItemClick(listItems, fragmentRule.getActivity(), 0);
    }

    @Test
    public void unsubscribesFromEventBusOnDestroy() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
        presenter.onDestroyView(fragmentRule.getFragment());

        eventBus.verifyUnsubscribed();
    }

    @Test
    public void setsAdapterUpsellListener() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
        presenter.onDestroyView(fragmentRule.getFragment());

        verify(adapter).setUpsellListener(presenter);
    }

    @Test
    public void shouldOpenUpgradeSubscription() {
        presenter.onUpsellClicked(context());

        verify(navigator).openUpgrade(context());
    }

    @Test
    public void shouldNotContainUpsellItemIfHighTierUser() {
        setupAdapter();
        when(featureOperations.upsellHighTier()).thenReturn(false);

        final CollectionBinding<SearchResult, ListItem> collectionBinding = presenter.onBuildBinding(new Bundle());
        final ListItem firstListItem = collectionBinding.adapter().getItem(0);

        assertThat(firstListItem).isInstanceOf(TrackItem.class);
    }

    @Test
    public void shouldTrackPremiumResultsUpsellImpression() {
        when(featureOperations.upsellHighTier()).thenReturn(true);

        presenter.onCreate(fragmentRule.getFragment(), new Bundle());

        verify(searchTracker).trackPremiumResultsUpsellImpression();
    }

    @Test
    public void shouldNotTrackPremiumResultsUpsellImpressionIfNotHighTierUpsell() {
        when(featureOperations.upsellHighTier()).thenReturn(false);

        presenter.onCreate(fragmentRule.getFragment(), new Bundle());

        verifyZeroInteractions(searchTracker);
    }

    private List<ListItem> setupAdapter() {
        final TrackItem trackItem = TrackItem.from(PropertySet.from(TrackProperty.URN.bind(TRACK_URN)));
        final List<ListItem> listItems = Collections.singletonList((ListItem) trackItem);
        when(adapter.getItem(0)).thenReturn(trackItem);
        when(adapter.getItems()).thenReturn(listItems);
        when(adapter.getResultItems()).thenReturn(listItems);
        return listItems;
    }
}
