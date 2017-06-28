package com.soundcloud.android.discovery;


import static com.soundcloud.android.discovery.DiscoveryFixtures.MULTI_APP_LINK;
import static com.soundcloud.android.discovery.DiscoveryFixtures.MULTI_CONTENT_SELECTION_CARD;
import static com.soundcloud.android.discovery.DiscoveryFixtures.MULTI_SELECTION_ITEM;
import static com.soundcloud.android.discovery.DiscoveryFixtures.MULTI_WEB_LINK;
import static com.soundcloud.android.discovery.DiscoveryFixtures.SEARCH_ITEM;
import static com.soundcloud.android.discovery.DiscoveryFixtures.SINGLE_APP_LINK;
import static com.soundcloud.android.discovery.DiscoveryFixtures.SINGLE_CONTENT_SELECTION_CARD;
import static com.soundcloud.android.discovery.DiscoveryFixtures.SINGLE_SELECTION_ITEM;
import static com.soundcloud.android.discovery.DiscoveryFixtures.SINGLE_WEB_LINK;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.ReferringEventProvider;
import com.soundcloud.android.events.ReferringEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.RootActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.stream.StreamSwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;

public class DiscoveryPresenterTest extends AndroidUnitTest {

    @Mock private Fragment fragment;
    @Mock private Bundle bundle;
    @Mock private DiscoveryAdapter adapter;
    @Mock private DiscoveryAdapterFactory adapterFactory;
    @Mock private StreamSwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private Observer<Iterable<DiscoveryCard>> itemObserver;
    @Mock private DiscoveryOperations discoveryOperations;
    @Mock private Navigator navigator;
    @Mock private DiscoveryTrackingManager discoveryTrackingManager;
    @Mock private FeedbackController feedbackController;
    @Mock private EventTracker eventTracker;
    @Mock private ReferringEventProvider referringEventProvider;
    @Captor private ArgumentCaptor<ScreenEvent> screenEventArgumentCaptor;

    private static final Screen SCREEN = Screen.DISCOVER;
    private DiscoveryPresenter presenter;
    private final FragmentActivity activity = activity();
    private final PublishSubject<Long> enterScreen = PublishSubject.create();
    private final ReferringEvent referringEvent = ReferringEvent.create("123", "previous_event");

    @Before
    public void setUp() {
        when(adapterFactory.create(any(DiscoveryPresenter.class))).thenReturn(adapter);
        presenter = new DiscoveryPresenter(swipeRefreshAttacher, adapterFactory, discoveryOperations, navigator, discoveryTrackingManager, feedbackController, eventTracker, referringEventProvider);
        when(discoveryOperations.discoveryCards()).thenReturn(Single.just(DiscoveryResult.create(emptyList(), Optional.absent())));
        when(discoveryOperations.refreshDiscoveryCards()).thenReturn(Single.just(DiscoveryResult.create(emptyList(), Optional.absent())));
        when(fragment.getActivity()).thenReturn(activity);
        when(referringEventProvider.getReferringEvent()).thenReturn(Optional.of(referringEvent));
    }

    @Test
    public void onCreateAddsSearchItemToAdapter() {
        CollectionBinding<List<DiscoveryCard>, DiscoveryCard> binding = presenter.onBuildBinding(bundle);
        binding.connect();
        binding.items().subscribe(itemObserver);

        verify(itemObserver).onNext(singletonList(DiscoveryCard.forSearchItem()));
    }

    @Test
    public void onRefreshAddsSearchItemToAdapter() {
        CollectionBinding<List<DiscoveryCard>, DiscoveryCard> binding = presenter.onRefreshBinding();
        binding.connect();
        binding.items().subscribe(itemObserver);

        verify(itemObserver).onNext(singletonList(DiscoveryCard.forSearchItem()));
    }

    @Test
    public void navigatesToSearchWhenClicked() {
        presenter.onSearchClicked(activity);

        verify(navigator).navigateTo(activity, NavigationTarget.forSearchAutocomplete(Screen.DISCOVER));
    }

    @Test
    public void handlesError() {
        final Exception exception = new RuntimeException();
        final EmptyView.Status result = presenter.handleError(exception);

        assertThat(result).isEqualTo(EmptyView.Status.ERROR);
    }

    @Test
    public void navigatesAndTracksSingleSelectionItemClick() {
        final ArrayList<DiscoveryCard> cards = Lists.newArrayList(SEARCH_ITEM, SINGLE_CONTENT_SELECTION_CARD, MULTI_CONTENT_SELECTION_CARD);
        when(adapter.getItems()).thenReturn(cards);
        final PublishSubject<SelectionItem> selectionItemPublishSubject = PublishSubject.create();
        when(adapter.selectionItemClick()).thenReturn(selectionItemPublishSubject);
        final RootActivity rootActivity = initRootActivity();

        presenter.onCreate(fragment, null);

        selectionItemPublishSubject.onNext(SINGLE_SELECTION_ITEM);

        verify(discoveryTrackingManager).trackSelectionItemClick(SINGLE_SELECTION_ITEM, cards);
        verify(navigator).navigateTo(same(rootActivity), eq(NavigationTarget.forNavigation(SINGLE_APP_LINK.get(), SINGLE_WEB_LINK, SCREEN, Optional.of(DiscoverySource.RECOMMENDATIONS))));
    }

    @Test
    public void navigatesAndTracksMultiSelectionItemClick() {
        final ArrayList<DiscoveryCard> cards = Lists.newArrayList(SEARCH_ITEM, SINGLE_CONTENT_SELECTION_CARD, MULTI_CONTENT_SELECTION_CARD);
        when(adapter.getItems()).thenReturn(cards);
        final PublishSubject<SelectionItem> selectionItemPublishSubject = PublishSubject.create();
        when(adapter.selectionItemClick()).thenReturn(selectionItemPublishSubject);
        final RootActivity rootActivity = initRootActivity();

        presenter.onCreate(fragment, null);

        selectionItemPublishSubject.onNext(MULTI_SELECTION_ITEM);

        verify(discoveryTrackingManager).trackSelectionItemClick(MULTI_SELECTION_ITEM, cards);
        verify(navigator).navigateTo(same(rootActivity), eq(NavigationTarget.forNavigation(MULTI_APP_LINK.get(), MULTI_WEB_LINK, SCREEN, Optional.of(DiscoverySource.RECOMMENDATIONS))));
    }

    @Test
    public void sendsPageViewWithQueryUrnAfterResultReceived() {
        when(adapter.selectionItemClick()).thenReturn(PublishSubject.create());
        initRootActivity();

        presenter.onCreate(fragment, null);

        enterScreen.onNext(123L);

        verifyZeroInteractions(eventTracker);

        when(discoveryOperations.discoveryCards()).thenReturn(Single.just(DiscoveryResult.create(Lists.newArrayList(DiscoveryFixtures.SINGLE_CONTENT_SELECTION_CARD,
                                                                                                                    DiscoveryFixtures.MULTI_CONTENT_SELECTION_CARD), Optional.absent())));
        CollectionBinding<List<DiscoveryCard>, DiscoveryCard> binding = presenter.onBuildBinding(bundle);
        binding.connect();
        binding.items().subscribe(itemObserver);

        verify(eventTracker).trackScreen(screenEventArgumentCaptor.capture(), eq(Optional.of(referringEvent)));

        final ScreenEvent screenEvent = screenEventArgumentCaptor.getValue();
        assertThat(screenEvent.screen()).isEqualTo(Screen.DISCOVER.get());
        assertThat(screenEvent.queryUrn().get()).isEqualTo(DiscoveryFixtures.SINGLE_CONTENT_SELECTION_CARD.parentQueryUrn().get());
    }

    private RootActivity initRootActivity() {
        final RootActivity rootActivity = mock(RootActivity.class);
        when(fragment.getActivity()).thenReturn(rootActivity);
        when(rootActivity.enterScreenTimestamp()).thenReturn(enterScreen);
        return rootActivity;
    }
}
