package com.soundcloud.android.search;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.analytics.performance.MetricKey;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.SearchPlayRelatedTracksConfig;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.payments.UpsellContext;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.TrackFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;

import android.os.Bundle;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SearchResultsPresenterTest extends AndroidUnitTest {

    private static final Urn PREMIUM_TRACK_URN_ONE = Urn.forTrack(1L);
    private static final Urn PREMIUM_TRACK_URN_TWO = Urn.forTrack(2L);
    private static final Urn TRACK_URN = Urn.forTrack(3L);
    private static final Urn QUERY_URN = new Urn("soundcloud:search:123");

    private static final String API_QUERY = "api_query";
    private static final String USER_QUERY = "user_query";
    private static final SearchType SEARCH_TAB = SearchType.ALL;
    private static final int RESULT_COUNT = 5;

    private SearchResultsPresenter presenter;

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private SearchOperations searchOperations;
    @Mock private SearchResultsAdapter adapter;
    @Mock private SearchOperations.SearchPagingFunction searchPagingFunction;
    @Mock private MixedItemClickListener.Factory clickListenerFactory;
    @Mock private MixedItemClickListener clickListener;
    @Mock private TrackItemRenderer trackItemRenderer;
    @Mock private NavigationExecutor navigationExecutor;
    @Mock private SearchTracker searchTracker;
    @Mock private ScreenProvider screenProvider;
    @Mock private SearchPlayQueueFilter searchPlayQueueFilter;
    @Mock private SearchPlayRelatedTracksConfig playRelatedTracksConfig;
    @Mock private FeatureOperations featureOperations;
    @Mock private PerformanceMetricsEngine performanceMetricsEngine;

    @Captor private ArgumentCaptor<List<ListItem>> listArgumentCaptor;
    @Captor private ArgumentCaptor<PerformanceMetric> performanceMetricArgumentCaptor;

    private TestEventBus eventBus = new TestEventBus();
    private Bundle bundle;

    private SearchQuerySourceInfo searchQuerySourceInfo;
    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.default_recyclerview_with_refresh,
                                                                    new Bundle());

    @Before
    public void setUp() throws Exception {
        setupFragmentArguments(true, SEARCH_TAB, false);
        final List<ListItem> trackItems = Collections.singletonList(TrackFixtures.trackItem(TRACK_URN));
        final SearchResult searchResult = SearchResult.fromSearchableItems(trackItems,
                                                                           Optional.absent(),
                                                                           Optional.of(QUERY_URN), RESULT_COUNT);
        final Observable<SearchResult> searchResultObservable = Observable.just(searchResult);

        presenter = new SearchResultsPresenter(swipeRefreshAttacher,
                                               searchOperations,
                                               adapter,
                                               clickListenerFactory,
                                               eventBus,
                                               navigationExecutor,
                                               searchTracker,
                                               screenProvider,
                                               searchPlayQueueFilter,
                                               featureOperations,
                                               performanceMetricsEngine);

        searchQuerySourceInfo = new SearchQuerySourceInfo(QUERY_URN, 0, Urn.forTrack(1), API_QUERY);
        searchQuerySourceInfo.setQueryResults(Arrays.asList(Urn.forTrack(1), Urn.forTrack(3)));

        when(clickListenerFactory.create(any(Screen.class),
                                         any(SearchQuerySourceInfo.class))).thenReturn(clickListener);
        when(searchOperations.searchResult(anyString(),
                                           any(Optional.class),
                                           any(SearchType.class),
                                           any(SearchOperations.ContentType.class))).thenReturn(searchResultObservable);
        when(searchOperations.pagingFunction(any(SearchType.class))).thenReturn(searchPagingFunction);
        when(searchPagingFunction.getSearchQuerySourceInfo(anyInt(), any(Urn.class), any(String.class))).thenReturn(searchQuerySourceInfo);
        when(screenProvider.getLastScreen()).thenReturn(Screen.SEARCH_MAIN);

        bundle = new Bundle();
        bundle.putParcelable(SearchResultsFragment.EXTRA_ARGS, SearchFragmentArgs.create(SEARCH_TAB, API_QUERY, USER_QUERY, Optional.absent(), Optional.absent(), false, false));
    }

    @Test
    public void itemClickDelegatesToClickListener() {
        final List<ListItem> listItems = setupAdapter();
        final int position = 0;

        when(clickListenerFactory.create(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo)).thenReturn(clickListener);
        when(searchPlayQueueFilter.correctQueue(listItems, position)).thenReturn(listItems);
        when(searchPlayQueueFilter.correctPosition(position)).thenReturn(position);

        presenter.onBuildBinding(bundle);
        presenter.onItemClicked(fragmentRule.getView(), position);

        verify(searchPlayQueueFilter).correctQueue(listItems, position);
        verify(searchPlayQueueFilter).correctPosition(position);
        verify(clickListener).onItemClick(listItems, fragmentRule.getActivity(), position);
    }

    @Test
    public void mustTrackItemClick() {
        setupAdapter();
        presenter.onBuildBinding(bundle);
        presenter.onItemClicked(fragmentRule.getView(), 0);

        verify(searchTracker).trackSearchItemClick(any(SearchType.class),
                                                   any(Urn.class),
                                                   any(SearchQuerySourceInfo.class));
    }

    @Test
    public void doesNotTrackNonFirstSearch() {
        setupFragmentArguments(false, SEARCH_TAB, false);
        presenter.onCreate(fragmentRule.getFragment(), new Bundle());

        verify(searchTracker, never()).trackSearchFormulationEnd(eq(Screen.SEARCH_MAIN), any(String.class), any(Optional.class), any(Optional.class));
        verify(searchTracker, never()).trackResultsScreenEvent(any(SearchType.class), any(String.class), eq(SearchOperations.ContentType.NORMAL));
    }

    @Test
    public void mustTrackSearchResults() {
        presenter.onCreate(fragmentRule.getFragment(), new Bundle());

        verify(searchTracker).setTrackingData(SEARCH_TAB, QUERY_URN, false);
        verify(searchTracker).trackSearchFormulationEnd(Screen.SEARCH_MAIN, USER_QUERY, Optional.absent(), Optional.absent());
        verify(searchTracker).trackResultsScreenEvent(SEARCH_TAB, API_QUERY, SearchOperations.ContentType.NORMAL);
    }

    @Test
    public void unsubscribesFromEventBusOnDestroy() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
        presenter.onDestroy(fragmentRule.getFragment());

        eventBus.verifyUnsubscribed();
    }

    @Test
    public void resetsSearchTrackerOnDestroy() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
        presenter.onDestroy(fragmentRule.getFragment());

        verify(searchTracker).reset();
    }

    @Test
    public void shouldOpenHighTierSearchResults() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        final List<Urn> premiumItemsSource = Collections.emptyList();
        final Optional<Link> nextHref = Optional.absent();
        presenter.onPremiumContentViewAllClicked(context(), premiumItemsSource, nextHref);

        verify(navigationExecutor).openSearchPremiumContentResults(eq(context()),
                                                                   anyString(),
                                                                   any(SearchType.class),
                                                                   eq(premiumItemsSource),
                                                                   eq(nextHref),
                                                                   eq(QUERY_URN));
    }

    @Test
    public void shouldOpenUpgradeSubscription() {
        presenter.onPremiumContentHelpClicked(context());

        verify(navigationExecutor).openUpgrade(context(), UpsellContext.PREMIUM_CONTENT);
    }

    @Test
    public void itemClickBuildsPlayQueueWithPremiumTracks() {
        setupAdapterWithPremiumContent();
        when(clickListenerFactory.create(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo)).thenReturn(clickListener);

        presenter.onBuildBinding(bundle);
        presenter.onItemClicked(fragmentRule.getView(), 1);

        verify(clickListener).onItemClick(listArgumentCaptor.capture(), eq(fragmentRule.getActivity()), eq(1));

        final List<ListItem> playQueue = listArgumentCaptor.getValue();
        assertThat(playQueue.get(0).getUrn()).isEqualTo(SearchPremiumItem.PREMIUM_URN);
        assertThat(playQueue.get(1).getUrn()).isEqualTo(TRACK_URN);
    }

    @Test
    public void premiumItemClickBuildsPlayQueueWithPremiumTracks() {
        setupAdapterWithPremiumContent();
        when(clickListenerFactory.create(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo)).thenReturn(clickListener);

        final ListItem premiumTrackItemOne = TrackFixtures.trackItem(PREMIUM_TRACK_URN_ONE);
        final ListItem premiumTrackItemTwo = TrackFixtures.trackItem(PREMIUM_TRACK_URN_TWO);
        final List<ListItem> premiumItems = Arrays.asList(premiumTrackItemOne, premiumTrackItemTwo);

        presenter.onBuildBinding(bundle);
        presenter.onPremiumItemClicked(fragmentRule.getView(), premiumItems);

        verify(clickListener).onItemClick(listArgumentCaptor.capture(), eq(fragmentRule.getActivity()), eq(0));

        final List<ListItem> playQueue = listArgumentCaptor.getValue();
        assertThat(playQueue.get(0).getUrn()).isEqualTo(PREMIUM_TRACK_URN_ONE);
        assertThat(playQueue.get(1).getUrn()).isEqualTo(TRACK_URN);
    }

    @Test
    public void shouldEndMeasuringSearchPerformanceOnAllSearchType() {
        setupFragmentArguments(false, SearchType.ALL, false);

        presenter.onCreate(fragmentRule.getFragment(), bundle);

        verify(performanceMetricsEngine).endMeasuring(performanceMetricArgumentCaptor.capture());
        PerformanceMetric metric = performanceMetricArgumentCaptor.getValue();
        assertThat(metric.metricType()).isEqualTo(MetricType.PERFORM_SEARCH);
        assertThat(metric.metricParams().toBundle().getString(MetricKey.SCREEN.toString())).isEqualTo(Screen.SEARCH_MAIN.toString());
    }

    @Test
    public void shouldNotEndMeasuringSearchPerformanceOnOtherSearchType() {
        setupFragmentArguments(false, SearchType.TRACKS, false);

        presenter.onCreate(fragmentRule.getFragment(), bundle);

        verify(performanceMetricsEngine, never()).endMeasuring(any(PerformanceMetric.class));
    }

    @Test
    public void setsAdapterUpsellListener() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
        presenter.onDestroyView(fragmentRule.getFragment());

        verify(adapter).setUpsellListener(presenter);
    }

    @Test
    public void shouldOpenUpgradeSubscriptionOnUpsellClick() {
        setupFragmentArguments(false, SearchType.TRACKS, true);
        presenter.onUpsellClicked(context());

        verify(navigationExecutor).openUpgrade(context(), UpsellContext.PREMIUM_CONTENT);
    }

    @Test
    public void shouldNotContainUpsellItemIfHighTierUser() {
        setupFragmentArguments(false, SearchType.TRACKS, true);
        setupAdapter();
        when(featureOperations.upsellHighTier()).thenReturn(false);

        final CollectionBinding<SearchResult, ListItem> collectionBinding = presenter.onBuildBinding(new Bundle());
        final ListItem firstListItem = collectionBinding.adapter().getItem(0);

        assertThat(firstListItem).isInstanceOf(TrackItem.class);
    }

    @Test
    public void shouldTrackPremiumResultsUpsellImpression() {
        setupFragmentArguments(false, SearchType.TRACKS, true);
        setupAdapter();
        when(featureOperations.upsellHighTier()).thenReturn(true);

        presenter.onCreate(fragmentRule.getFragment(), new Bundle());

        verify(searchTracker).trackPremiumResultsUpsellImpression();
    }

    @Test
    public void shouldNotTrackPremiumResultsUpsellImpressionIfNotHighTierUpsell() {
        setupFragmentArguments(false, SearchType.TRACKS, true);
        setupAdapter();
        when(featureOperations.upsellHighTier()).thenReturn(false);

        presenter.onCreate(fragmentRule.getFragment(), new Bundle());

        verify(searchTracker, never()).trackPremiumResultsUpsellImpression();
    }

    private List<ListItem> setupAdapter() {
        final TrackItem trackItem = TrackFixtures.trackItem(TRACK_URN);
        final List<ListItem> listItems = Collections.singletonList(trackItem);
        when(adapter.getItem(0)).thenReturn(trackItem);
        when(adapter.getItems()).thenReturn(listItems);
        when(adapter.getResultItems()).thenReturn(listItems);
        when(searchPagingFunction.getSearchQuerySourceInfo(eq(0), eq(TRACK_URN), any(String.class))).thenReturn(searchQuerySourceInfo);
        return listItems;
    }

    private void setupAdapterWithPremiumContent() {
        presenter = new SearchResultsPresenter(swipeRefreshAttacher,
                                               searchOperations,
                                               adapter,
                                               clickListenerFactory,
                                               eventBus,
                                               navigationExecutor,
                                               searchTracker,
                                               screenProvider,
                                               new SearchPlayQueueFilter(playRelatedTracksConfig),
                                               featureOperations,
                                               performanceMetricsEngine);


        final ListItem premiumItem = new SearchPremiumItem(Collections.singletonList(TrackFixtures.trackItem(PREMIUM_TRACK_URN_ONE)),
                                                           Optional.absent(),
                                                           1);
        final TrackItem trackItem = TrackFixtures.trackItem(TRACK_URN);
        final List<ListItem> listItems = Arrays.asList(premiumItem, trackItem);

        when(adapter.getItem(0)).thenReturn(premiumItem);
        when(adapter.getItem(1)).thenReturn(trackItem);
        when(adapter.getItems()).thenReturn(listItems);
        when(adapter.getResultItems()).thenReturn(Collections.singletonList(trackItem));
        when(searchPagingFunction.getSearchQuerySourceInfo(eq(0), eq(TRACK_URN), any(String.class))).thenReturn(searchQuerySourceInfo);
        when(playRelatedTracksConfig.isEnabled()).thenReturn(false);
    }

    private void setupFragmentArguments(boolean publishSearchSubmissionEvent, SearchType searchType, boolean isPremium) {
        final Bundle arguments = new Bundle();
        arguments.putParcelable(SearchResultsFragment.EXTRA_ARGS,
                                SearchFragmentArgs.create(searchType, API_QUERY, USER_QUERY, Optional.absent(), Optional.absent(), publishSearchSubmissionEvent, isPremium));
        fragmentRule.setFragmentArguments(arguments);
    }
}
