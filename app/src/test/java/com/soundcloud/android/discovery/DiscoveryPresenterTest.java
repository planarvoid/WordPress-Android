package com.soundcloud.android.discovery;

import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.search.PlaylistTagsPresenter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.view.EmptyView;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DiscoveryPresenterTest extends AndroidUnitTest {

    private static final Urn SEED_TRACK_URN = Urn.forTrack(1L);
    private static final Urn RECOMMENDATION_URN = Urn.forTrack(2L);
    private static final Urn RECOMMENDED_TRACK_URN = Urn.forTrack(3L);

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private View view;
    @Mock private Fragment fragment;
    @Mock private RecyclerView recyclerView;
    @Mock private EmptyView emptyView;
    @Mock private Resources resources;
    @Mock private DiscoveryOperations discoveryOperations;
    @Mock private DiscoveryAdapter adapter;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private Navigator navigator;
    @Mock private FeatureFlags featureFlags;

    private DiscoveryPresenter presenter;

    private TestSubscriber testSubscriber = new TestSubscriber();
    private Provider expandPlayerSubscriberProvider = providerOf(testSubscriber);

    @Mock RecommendationItem recommendationItemOne;
    @Mock RecommendationItem recommendationItemTwo;

    @Rule public ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        this.presenter = new DiscoveryPresenter(swipeRefreshAttacher, discoveryOperations,
                adapter, expandPlayerSubscriberProvider, playbackInitiator, navigator, featureFlags);

        when(view.findViewById(R.id.ak_recycler_view)).thenReturn(recyclerView);
        when(view.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(view.getContext()).thenReturn(context());
        when(view.getResources()).thenReturn(context().getResources());
        when(recyclerView.getAdapter()).thenReturn(adapter);
        when(recommendationItemOne.getSeedTrackUrn()).thenReturn(SEED_TRACK_URN);
        when(recommendationItemOne.getRecommendationUrn()).thenReturn(RECOMMENDATION_URN);
    }

    @Test
    public void activityMustImplementPlayListTagListener() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Host activity must be a " + PlaylistTagsPresenter.Listener.class);

        Activity activity = mock(Activity.class);
        presenter.onAttach(fragment, activity);
    }

    @Test
    public void selectPlayListTagShouldCallActivityListener() {
        Activity activity = mock(Activity.class, withSettings().extraInterfaces(PlaylistTagsPresenter.Listener.class));
        presenter.onAttach(fragment, activity);
        presenter.onTagSelected("#rock");

        verify((PlaylistTagsPresenter.Listener) activity).onTagSelected("#rock");
    }

    @Test
    public void clickOnViewAllShouldOpenRecommendations() {
        enableRecommendations();

        Context context = mock(Context.class);
        RecommendationItem recommendationItem = mock(RecommendationItem.class);
        when(recommendationItem.getSeedTrackLocalId()).thenReturn(1L);

        presenter.onRecommendationViewAllClicked(context, recommendationItem);

        verify(navigator).openRecommendation(context, 1L);
    }

    @Test
    public void clickOnRecommendationReasonPlaysSeedAndRecommendedTracks() {
        enableRecommendations();

        PublishSubject<List<DiscoveryItem>> discoveryItems = PublishSubject.create();
        when(discoveryOperations.discoveryItemsAndRecommendations()).thenReturn(discoveryItems);

        PublishSubject<List<Urn>> recommendedTracksForSeed = PublishSubject.create();
        when(discoveryOperations.recommendedTracksWithSeed(any(RecommendationItem.class))).thenReturn(recommendedTracksForSeed);

        discoveryItems.onNext(Arrays.<DiscoveryItem>asList(recommendationItemOne, recommendationItemTwo));
        recommendedTracksForSeed.onNext(Arrays.asList(SEED_TRACK_URN, RECOMMENDED_TRACK_URN));

        when(playbackInitiator.playTracks(eq(recommendedTracksForSeed), eq(SEED_TRACK_URN), eq(0), isA(PlaySessionSource.class)))
                .thenReturn(Observable.just(PlaybackResult.success()));

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        presenter.onRecommendationReasonClicked(recommendationItemOne);
    }

    @Test
    public void clickOnRecommendationArtworkPlaysRecommendedTracks() {
        enableRecommendations();

        PublishSubject<List<DiscoveryItem>> discoveryItems = PublishSubject.create();
        when(discoveryOperations.discoveryItemsAndRecommendations()).thenReturn(discoveryItems);

        PublishSubject<List<Urn>> recommendedTracks = PublishSubject.create();
        when(discoveryOperations.recommendedTracks()).thenReturn(recommendedTracks);

        discoveryItems.onNext(Arrays.<DiscoveryItem>asList(recommendationItemOne, recommendationItemTwo));
        recommendedTracks.onNext(Collections.singletonList(RECOMMENDED_TRACK_URN));

        when(playbackInitiator.playTracks(eq(recommendedTracks), eq(RECOMMENDATION_URN), eq(0), isA(PlaySessionSource.class)))
                .thenReturn(Observable.just(PlaybackResult.success()));

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        presenter.onRecommendationArtworkClicked(recommendationItemOne);
    }

    private void enableRecommendations() {
        when(featureFlags.isEnabled(Flag.FEATURE_DISCOVERY)).thenReturn(true);
        when(featureFlags.isEnabled(Flag.FEATURE_DISCOVERY_RECOMMENDATIONS)).thenReturn(true);
    }
}
