package com.soundcloud.android.likes;

import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarHelper;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.MidTierTrackEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadRequest;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.paywall.PaywallImpressionController;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.CollapsingScrollHelper;
import com.soundcloud.android.view.adapters.PagedTracksRecyclerItemAdapter;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TrackLikesPresenterTest extends AndroidUnitTest {

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.track_likes_fragment);

    private TrackLikesPresenter presenter;

    @Mock private TrackLikeOperations likeOperations;
    @Mock private OfflinePlaybackOperations playbackOperations;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private PagedTracksRecyclerItemAdapter adapter;
    @Mock private ActionBarHelper actionMenuController;
    @Mock private TrackLikesHeaderPresenter headerPresenter;
    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private Menu menu;
    @Mock private MenuInflater menuInflater;
    @Mock private MenuItem menuItem;
    @Mock private Navigator navigator;
    @Mock private FeatureOperations featureOperations;
    @Mock private CollapsingScrollHelper collapsingScrollHelper;
    @Mock private PaywallImpressionController paywallImpressionController;

    private PublishSubject<List<PropertySet>> likedTracksObservable = PublishSubject.create();
    private TestSubscriber testSubscriber = new TestSubscriber();
    private Provider expandPlayerSubscriberProvider = providerOf(testSubscriber);
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setup() {
        presenter = new TrackLikesPresenter(likeOperations, playbackOperations,
                offlineContentOperations, adapter, actionMenuController, headerPresenter, expandPlayerSubscriberProvider,
                eventBus, swipeRefreshAttacher, featureOperations, navigator, collapsingScrollHelper, paywallImpressionController);
        when(likeOperations.likedTracks()).thenReturn(likedTracksObservable);
        when(likeOperations.onTrackLiked()).thenReturn(Observable.<PropertySet>empty());
        when(likeOperations.onTrackUnliked()).thenReturn(Observable.<Urn>empty());
        when(offlineContentOperations.getOfflineContentOrOfflineLikesStatusChanges()).thenReturn(Observable.just(true));
    }

    @Test
    public void shouldOnSubscribeListObserversToHeaderPresenter() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
        verify(headerPresenter).onSubscribeListObservers(any(CollectionBinding.class));
    }

    @Test
    public void shouldRedirectOnDestroyViewToHeaderPresenter() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
        presenter.onDestroyView(fragmentRule.getFragment());
        verify(headerPresenter).onDestroyView(fragmentRule.getFragment());
    }

    @Test
    public void shouldAttachRecyclerViewToImpressionControllerInOnViewCreated() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
        verify(paywallImpressionController).attachRecyclerView(isA(RecyclerView.class));
    }

    @Test
    public void shouldDetachRecyclerViewToImpressionControllerInOnDestroyView() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
        presenter.onDestroyView(fragmentRule.getFragment());
        verify(paywallImpressionController).detachRecyclerView(isA(RecyclerView.class));
    }

    @Test
    public void shouldPlayLikedTracksOnListItemClick() {
        PlaybackResult playbackResult = setupPlaybackConditions(ModelFixtures.create(TrackItem.class));
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onItemClicked(mock(View.class), 0);

        testSubscriber.assertReceivedOnNext(Arrays.asList(playbackResult));
    }

    @Test
    public void shouldShowUpsellOnMidTierItemClick() {
        final TrackItem clickedTrack = TrackItem.from(TestPropertySets.midTierTrack());

        when(featureOperations.upsellMidTier()).thenReturn(true);
        when(adapter.getItem(0)).thenReturn(clickedTrack);
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onItemClicked(mock(View.class), 0);

        verify(navigator).openUpgrade(any(Context.class));
    }

    @Test
    public void shouldNotShowUpsellOnMidTierItemClickWhenUserCannotUpgrade() {
        final TrackItem clickedTrack = TrackItem.from(TestPropertySets.midTierTrack());
        setupPlaybackConditions(clickedTrack);

        when(adapter.getItem(0)).thenReturn(clickedTrack);
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onItemClicked(mock(View.class), 0);

        verifyZeroInteractions(navigator);
    }

    @Test
    public void shouldSendUpsellEventOnMidTierItemClick() {
        final TrackItem clickedTrack = TrackItem.from(TestPropertySets.midTierTrack());

        when(featureOperations.upsellMidTier()).thenReturn(true);
        when(adapter.getItem(0)).thenReturn(clickedTrack);
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onItemClicked(mock(View.class), 0);

        final MidTierTrackEvent trackingEvent = (MidTierTrackEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(trackingEvent.getKind()).isEqualTo(MidTierTrackEvent.KIND_CLICK);
        assertThat(trackingEvent.getTrackUrn()).isEqualTo(clickedTrack.getEntityUrn());
    }

    @Test
    public void shouldNotSendUpsellEventOnMidTierItemClickWhenUserCannotUpgrade() {
        final TrackItem clickedTrack = TrackItem.from(TestPropertySets.midTierTrack());
        setupPlaybackConditions(clickedTrack);

        when(adapter.getItem(0)).thenReturn(clickedTrack);
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onItemClicked(mock(View.class), 0);

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }


    @Test
    public void shouldNotPlayTracksOnListItemClickIfItemIsNull() {
        when(adapter.getItem(0)).thenReturn(null);
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onItemClicked(mock(View.class), 0);

        verifyZeroInteractions(playbackOperations);
    }

    @Test
    public void shouldUnsubscribeFromEventQueuesWhenViewsAreDestroyed() {
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
        presenter.onDestroyView(fragmentRule.getFragment());

        eventBus.verifyUnsubscribed();
    }

    @Test
    public void shouldUpdateAdapterWhenLikedTrackDownloaded() {
        final ApiTrack track = ModelFixtures.create(ApiTrack.class);
        final DownloadRequest downloadRequest = ModelFixtures.downloadRequestFromLikes(track.getUrn());
        final CurrentDownloadEvent downloadingEvent = CurrentDownloadEvent.downloading(downloadRequest);

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);
        reset(adapter);

        when(adapter.getItems()).thenReturn(Collections.singletonList(TrackItem.from(track)));
        eventBus.publish(EventQueue.CURRENT_DOWNLOAD, downloadingEvent);

        verify(adapter).notifyDataSetChanged();
    }

    @Test
    public void shouldUpdateAdapterOnOfflineLikesOrOfflineContentStateChange() {
        PublishSubject<Boolean> featureChange = PublishSubject.create();
        when(offlineContentOperations.getOfflineContentOrOfflineLikesStatusChanges()).thenReturn(featureChange);
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        featureChange.onNext(true);

        verify(adapter).notifyDataSetChanged();
    }

    @NotNull
    private PlaybackResult setupPlaybackConditions(TrackItem clickedTrack) {
        PlaybackResult playbackResult = PlaybackResult.success();
        when(adapter.getItem(0)).thenReturn(clickedTrack);
        when(playbackOperations.playLikes(eq(clickedTrack.getEntityUrn()), eq(0), isA(PlaySessionSource.class)))
                .thenReturn(Observable.just(playbackResult));
        return playbackResult;
    }

}
