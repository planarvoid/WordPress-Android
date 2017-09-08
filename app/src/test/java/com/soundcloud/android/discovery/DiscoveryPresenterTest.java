package com.soundcloud.android.discovery;


import static com.soundcloud.android.helpers.NavigationTargetMatcher.matchesNavigationTarget;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.ReferringEventProvider;
import com.soundcloud.android.events.ReferringEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.RootActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.stream.StreamSwipeRefreshAttacher;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;
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
import java.util.Collections;
import java.util.List;

public class DiscoveryPresenterTest extends AndroidUnitTest {

    @Mock private Fragment fragment;
    @Mock private Bundle bundle;
    @Mock private DiscoveryAdapter adapter;
    @Mock private DiscoveryAdapter.Factory adapterFactory;
    @Mock private StreamSwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private Observer<Iterable<DiscoveryCardViewModel>> itemObserver;
    @Mock private DiscoveryOperations discoveryOperations;
    @Mock private Navigator navigator;
    @Mock private FeedbackController feedbackController;
    @Mock private EventTracker eventTracker;
    @Mock private ReferringEventProvider referringEventProvider;
    @Mock private SyncStateStorage syncStateStorage;
    @Captor private ArgumentCaptor<ScreenEvent> screenEventArgumentCaptor;
    @Captor private ArgumentCaptor<Iterable<DiscoveryCardViewModel>> discoveryCardsArgumentCaptor;

    private static final Screen SCREEN = Screen.DISCOVER;
    private DiscoveryPresenter presenter;
    private final FragmentActivity activity = activity();
    private final PublishSubject<Long> enterScreen = PublishSubject.create();
    private final ReferringEvent referringEvent = ReferringEvent.create("123", "previous_event");

    @Before
    public void setUp() {
        when(adapterFactory.create(any(DiscoveryPresenter.class))).thenReturn(adapter);
        presenter = new DiscoveryPresenter(swipeRefreshAttacher,
                                           adapterFactory,
                                           discoveryOperations,
                                           navigator,
                                           feedbackController,
                                           eventTracker,
                                           referringEventProvider,
                                           syncStateStorage);
        when(discoveryOperations.discoveryCards()).thenReturn(Single.just(new DiscoveryResult(Lists.newArrayList(DiscoveryFixtures.INSTANCE.getSingleContentSelectionCard()), Optional.absent())));
        when(discoveryOperations.refreshDiscoveryCards()).thenReturn(Single.just(new DiscoveryResult(Lists.newArrayList(DiscoveryFixtures.INSTANCE.getMultipleContentSelectionCard()),
                                                                                                     Optional.absent())));
        when(fragment.getActivity()).thenReturn(activity);
        when(referringEventProvider.getReferringEvent()).thenReturn(Optional.of(referringEvent));
    }

    @Test
    public void onCreateAddsSearchItemToAdapter() {

        CollectionBinding<List<DiscoveryCardViewModel>, DiscoveryCardViewModel> binding = presenter.onBuildBinding(bundle);
        binding.connect();
        binding.items().subscribe(itemObserver);

        verify(itemObserver).onNext(discoveryCardsArgumentCaptor.capture());

        assertThat(discoveryCardsArgumentCaptor.getValue().iterator().next()).isEqualTo(DiscoveryCardViewModel.SearchCard.INSTANCE);
    }

    @Test
    public void onCreateResetsSyncMisses() {
        when(adapter.selectionItemClick()).thenReturn(PublishSubject.create());
        initRootActivity();

        presenter.onCreate(fragment, null);

        verify(syncStateStorage).resetSyncMisses(Syncable.DISCOVERY_CARDS);
    }

    @Test
    public void onRefreshAddsSearchItemToAdapter() {
        CollectionBinding<List<DiscoveryCardViewModel>, DiscoveryCardViewModel> binding = presenter.onRefreshBinding();
        binding.connect();
        binding.items().subscribe(itemObserver);

        verify(itemObserver).onNext(discoveryCardsArgumentCaptor.capture());

        assertThat(discoveryCardsArgumentCaptor.getValue().iterator().next()).isEqualTo(DiscoveryCardViewModel.SearchCard.INSTANCE);
    }

    @Test
    public void navigatesToSearchWhenClicked() {
        presenter.onSearchClicked(activity);

        verify(navigator).navigateTo(argThat(matchesNavigationTarget(NavigationTarget.forSearchAutocomplete(Screen.DISCOVER))));
    }

    @Test
    public void handlesError() {
        final Exception exception = new RuntimeException();
        final EmptyView.Status result = presenter.handleError(exception);

        assertThat(result).isEqualTo(EmptyView.Status.ERROR);
    }

    @Test
    public void navigatesAndTracksSingleSelectionItemClick() {
        final ArrayList<DiscoveryCardViewModel> cards = Lists.newArrayList(DiscoveryCardViewModel.SearchCard.INSTANCE,
                                                                           DiscoveryFixtures.INSTANCE.singleContentSelectionCardViewModel(),
                                                                           DiscoveryFixtures.INSTANCE.multiContentSelectionCardViewModel());
        when(adapter.getItems()).thenReturn(cards);
        final PublishSubject<SelectionItemViewModel> selectionItemPublishSubject = PublishSubject.create();
        when(adapter.selectionItemClick()).thenReturn(selectionItemPublishSubject);
        initRootActivity();

        presenter.onCreate(fragment, null);

        final SelectionItemTrackingInfo trackingInfo = mock(SelectionItemTrackingInfo.class);
        final UIEvent uiEvent = mock(UIEvent.class);
        when(trackingInfo.toUIEvent()).thenReturn(uiEvent);
        selectionItemPublishSubject.onNext(DiscoveryFixtures.INSTANCE.singleSelectionItemViewModel(trackingInfo));

        verify(eventTracker).trackClick(uiEvent);
        verify(navigator).navigateTo(argThat(matchesNavigationTarget(NavigationTarget.forNavigation(DiscoveryFixtures.INSTANCE.getSingleAppLink(),
                                                                                                    Optional.fromNullable(DiscoveryFixtures.INSTANCE.getSingleWebLink()), SCREEN,
                                                                                                    Optional.of(DiscoverySource.RECOMMENDATIONS)))));
    }

    @Test
    public void navigatesAndTracksMultiSelectionItemClick() {
        final ArrayList<DiscoveryCardViewModel> cards = Lists.newArrayList(DiscoveryCardViewModel.SearchCard.INSTANCE,
                                                                           DiscoveryFixtures.INSTANCE.singleContentSelectionCardViewModel(),
                                                                           DiscoveryFixtures.INSTANCE.multiContentSelectionCardViewModel());
        when(adapter.getItems()).thenReturn(cards);
        final PublishSubject<SelectionItemViewModel> selectionItemPublishSubject = PublishSubject.create();
        when(adapter.selectionItemClick()).thenReturn(selectionItemPublishSubject);
        initRootActivity();

        presenter.onCreate(fragment, null);

        final SelectionItemTrackingInfo trackingInfo = mock(SelectionItemTrackingInfo.class);
        final UIEvent uiEvent = mock(UIEvent.class);
        when(trackingInfo.toUIEvent()).thenReturn(uiEvent);
        selectionItemPublishSubject.onNext(DiscoveryFixtures.INSTANCE.multiSelectionItemViewModel(trackingInfo));

        verify(eventTracker).trackClick(uiEvent);
        verify(navigator).navigateTo(argThat(matchesNavigationTarget(NavigationTarget.forNavigation(DiscoveryFixtures.INSTANCE.getMultiAppLink(),
                                                                                                    Optional.fromNullable(DiscoveryFixtures.INSTANCE.getMultiWebLink()), SCREEN,
                                                                                                    Optional.of(DiscoverySource.RECOMMENDATIONS)))));
    }

    @Test
    public void sendsPageViewWithQueryUrnAfterResultReceived() {
        when(discoveryOperations.discoveryCards()).thenReturn(Single.just(new DiscoveryResult(Collections.emptyList(), Optional.absent())));
        when(adapter.selectionItemClick()).thenReturn(PublishSubject.create());
        initRootActivity();

        presenter.onCreate(fragment, null);

        enterScreen.onNext(123L);

        verifyZeroInteractions(eventTracker);

        when(discoveryOperations.discoveryCards()).thenReturn(Single.just(new DiscoveryResult(Lists.newArrayList(DiscoveryFixtures.INSTANCE.getSingleContentSelectionCard(),
                                                                                                                 DiscoveryFixtures.INSTANCE.getMultipleContentSelectionCard()), Optional.absent())));
        CollectionBinding<List<DiscoveryCardViewModel>, DiscoveryCardViewModel> binding = presenter.onBuildBinding(bundle);
        binding.connect();
        binding.items().subscribe(itemObserver);

        verify(eventTracker).trackScreen(screenEventArgumentCaptor.capture(), eq(Optional.of(referringEvent)));

        final ScreenEvent screenEvent = screenEventArgumentCaptor.getValue();
        assertThat(screenEvent.screen()).isEqualTo(Screen.DISCOVER.get());
        assertThat(screenEvent.queryUrn().get()).isEqualTo(DiscoveryFixtures.INSTANCE.getSingleContentSelectionCard().getParentQueryUrn());
    }

    private RootActivity initRootActivity() {
        final RootActivity rootActivity = mock(RootActivity.class);
        when(fragment.getActivity()).thenReturn(rootActivity);
        when(rootActivity.enterScreenTimestamp()).thenReturn(enterScreen);
        return rootActivity;
    }
}
