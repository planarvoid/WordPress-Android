package com.soundcloud.android.search;

import static com.soundcloud.android.search.SearchOperations.TYPE_ALL;
import static com.soundcloud.android.search.SearchResultsFragment.EXTRA_PUBLISH_SEARCH_SUBMISSION_EVENT;
import static com.soundcloud.android.search.SearchResultsFragment.EXTRA_QUERY;
import static com.soundcloud.android.search.SearchResultsFragment.EXTRA_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.java.collections.PropertySet;
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

    private static final String SEARCH_QUERY = "query";
    private static final int SEARCH_TYPE = TYPE_ALL;

    private SearchResultsPresenter presenter;

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private SearchOperations searchOperations;
    @Mock private SearchResultsAdapter adapter;
    @Mock private SearchOperations.SearchPagingFunction searchPagingFunction;
    @Mock private MixedItemClickListener.Factory clickListenerFactory;
    @Mock private MixedItemClickListener clickListener;
    @Mock private TrackItemRenderer trackItemRenderer;
    @Mock private FeatureFlags featureFlags;
    @Mock private Navigator navigator;
    @Mock private SearchTracker searchTracker;

    @Captor private ArgumentCaptor<List<ListItem>> listArgumentCaptor;

    private TestEventBus eventBus = new TestEventBus();
    private SearchQuerySourceInfo searchQuerySourceInfo;

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.default_recyclerview_with_refresh, new Bundle());

    @Before
    public void setUp() throws Exception {
        setupFragmentArguments(true);
        final List<PropertySet> propertySets = Collections.singletonList(PropertySet.create().put(EntityProperty.URN, TRACK_URN));
        final SearchResult searchResult = SearchResult.fromPropertySets(propertySets, Optional.<Link>absent(), QUERY_URN);
        final Observable<SearchResult> searchResultObservable = Observable.just(searchResult);

        presenter = new SearchResultsPresenter(swipeRefreshAttacher, searchOperations, adapter,
                clickListenerFactory, eventBus, featureFlags, navigator, searchTracker);

        searchQuerySourceInfo = new SearchQuerySourceInfo(QUERY_URN, 0, Urn.forTrack(1));
        searchQuerySourceInfo.setQueryResults(Arrays.asList(Urn.forTrack(1), Urn.forTrack(3)));


        when(clickListenerFactory.create(any(Screen.class), any(SearchQuerySourceInfo.class))).thenReturn(clickListener);
        when(searchOperations.searchResult(anyString(), anyInt())).thenReturn(searchResultObservable);
        when(searchOperations.pagingFunction(anyInt())).thenReturn(searchPagingFunction);
        when(searchPagingFunction.getSearchQuerySourceInfo(anyInt(), any(Urn.class))).thenReturn(searchQuerySourceInfo);
        when(featureFlags.isEnabled(Flag.SOUNDCLOUD_GO)).thenReturn(false);
    }

    @Test
    public void itemClickDelegatesToClickListener() {
        final List<ListItem> listItems = setupAdapter();
        when(clickListenerFactory.create(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo)).thenReturn(clickListener);

        presenter.onBuildBinding(new Bundle());
        presenter.onItemClicked(fragmentRule.getView(), 0);

        verify(clickListener).onItemClick(listItems, fragmentRule.getView(), 0);
    }

    @Test
    public void mustTrackItemClick() {
        setupAdapter();

        presenter.onBuildBinding(new Bundle());
        presenter.onItemClicked(fragmentRule.getView(), 0);

        verify(searchTracker).trackSearchItemClick(anyInt(), any(Urn.class), any(SearchQuerySourceInfo.class));
    }

    @Test
    public void doesNotTrackNonFirstSearch() {
        setupFragmentArguments(false);
        presenter.onCreate(fragmentRule.getFragment(), new Bundle());

        verify(searchTracker, times(0)).trackSearchSubmission(anyInt(), any(Urn.class));
        verify(searchTracker, times(0)).trackResultsScreenEvent(anyInt());
    }

    @Test
    public void mustTrackSearchResults() {
        presenter.onCreate(fragmentRule.getFragment(), new Bundle());

        verify(searchTracker).setTrackingData(SEARCH_TYPE, QUERY_URN, false);
        verify(searchTracker).trackSearchSubmission(SEARCH_TYPE, QUERY_URN);
        verify(searchTracker).trackResultsScreenEvent(SEARCH_TYPE);
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

        final List<PropertySet> premiumItemsSource = Collections.emptyList();
        final Optional<Link> nextHref = Optional.absent();
        presenter.onPremiumContentViewAllClicked(context(), premiumItemsSource, nextHref);

        verify(navigator).openSearchPremiumContentResults(eq(context()), anyString(), anyInt(), eq(premiumItemsSource), eq(nextHref), eq(QUERY_URN));
    }

    @Test
    public void shouldOpenUpgradeSubscription() {
        presenter.onPremiumContentHelpClicked(context());

        verify(navigator).openUpgrade(context());
    }

    @Test
    public void itemClickBuildsPlayQueueWithPremiumTracks() {
        setupAdapterWithPremiumContent();
        when(clickListenerFactory.create(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo)).thenReturn(clickListener);

        presenter.onBuildBinding(new Bundle());
        presenter.onItemClicked(fragmentRule.getView(), 1);

        verify(clickListener).onItemClick(listArgumentCaptor.capture(), eq(fragmentRule.getView()), eq(1));

        final List<ListItem> playQueue = listArgumentCaptor.getValue();
        assertThat(playQueue.get(0).getUrn()).isEqualTo(SearchPremiumItem.PREMIUM_URN);
        assertThat(playQueue.get(1).getUrn()).isEqualTo(TRACK_URN);
    }

    @Test
    public void premiumItemClickBuildsPlayQueueWithPremiumTracks() {
        setupAdapterWithPremiumContent();
        when(clickListenerFactory.create(Screen.SEARCH_EVERYTHING, searchQuerySourceInfo)).thenReturn(clickListener);

        final ListItem premiumTrackItemOne = TrackItem.from(PropertySet.from(TrackProperty.URN.bind(PREMIUM_TRACK_URN_ONE)));
        final ListItem premiumTrackItemTwo = TrackItem.from(PropertySet.from(TrackProperty.URN.bind(PREMIUM_TRACK_URN_TWO)));
        final List<ListItem> premiumItems = Arrays.asList(premiumTrackItemOne, premiumTrackItemTwo);

        presenter.onBuildBinding(new Bundle());
        presenter.onPremiumItemClicked(fragmentRule.getView(), premiumItems);

        verify(clickListener).onItemClick(listArgumentCaptor.capture(), eq(fragmentRule.getView()), eq(0));

        final List<ListItem> playQueue = listArgumentCaptor.getValue();
        assertThat(playQueue.get(0).getUrn()).isEqualTo(PREMIUM_TRACK_URN_ONE);
        assertThat(playQueue.get(1).getUrn()).isEqualTo(TRACK_URN);
    }

    private List<ListItem> setupAdapter() {
        final TrackItem trackItem = TrackItem.from(PropertySet.from(TrackProperty.URN.bind(TRACK_URN)));
        final List<ListItem> listItems = Collections.singletonList((ListItem) trackItem);
        when(adapter.getItem(0)).thenReturn(trackItem);
        when(adapter.getItems()).thenReturn(listItems);
        when(searchPagingFunction.getSearchQuerySourceInfo(0, TRACK_URN)).thenReturn(searchQuerySourceInfo);
        return listItems;
    }

    private List<ListItem> setupAdapterWithPremiumContent() {
        PropertySet propertySet = PropertySet.create();
        propertySet.put(TrackProperty.URN, PREMIUM_TRACK_URN_ONE);

        final ListItem premiumItem = new SearchPremiumItem(Collections.singletonList(propertySet), Optional.<Link>absent(), 1);
        final TrackItem trackItem = TrackItem.from(PropertySet.from(TrackProperty.URN.bind(TRACK_URN)));
        final List<ListItem> listItems = Arrays.asList(premiumItem, trackItem);

        when(adapter.getItem(0)).thenReturn(premiumItem);
        when(adapter.getItem(1)).thenReturn(trackItem);
        when(adapter.getItems()).thenReturn(listItems);
        when(searchPagingFunction.getSearchQuerySourceInfo(0, TRACK_URN)).thenReturn(searchQuerySourceInfo);

        return listItems;
    }

    private void setupFragmentArguments(boolean publishSearchSubmissionEvent) {
        final Bundle arguments = new Bundle();
        arguments.putString(EXTRA_QUERY, SEARCH_QUERY);
        arguments.putInt(EXTRA_TYPE, SEARCH_TYPE);
        arguments.putBoolean(EXTRA_PUBLISH_SEARCH_SUBMISSION_EVENT, publishSearchSubmissionEvent);
        fragmentRule.setFragmentArguments(arguments);
    }
}
