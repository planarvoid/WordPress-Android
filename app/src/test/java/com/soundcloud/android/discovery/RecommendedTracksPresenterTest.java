package com.soundcloud.android.discovery;

import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.view.EmptyView;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.List;

public class RecommendedTracksPresenterTest extends AndroidUnitTest {

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private View view;
    @Mock private Fragment fragment;
    @Mock private RecyclerView recyclerView;
    @Mock private EmptyView emptyView;
    @Mock private Resources resources;
    @Mock private DiscoveryOperations discoveryOperations;
    @Mock private RecommendedTracksAdapter adapter;
    @Mock private PlaybackOperations playbackOperations;

    private RecommendedTracksPresenter presenter;

    private TestSubscriber testSubscriber = new TestSubscriber();
    private Provider expandPlayerSubscriberProvider = providerOf(testSubscriber);

    @Mock RecommendedTrackItem recommendedTrackItemOne;
    @Mock RecommendedTrackItem recommendedTrackItemTwo;

    @Before
    public void setUp() {
        this.presenter = new RecommendedTracksPresenter(swipeRefreshAttacher, discoveryOperations,
                adapter, expandPlayerSubscriberProvider, playbackOperations);

        when(view.findViewById(R.id.ak_recycler_view)).thenReturn(recyclerView);
        when(view.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(view.getContext()).thenReturn(context());
        when(view.getResources()).thenReturn(context().getResources());
        when(recyclerView.getAdapter()).thenReturn(adapter);

        when(recommendedTrackItemOne.getEntityUrn()).thenReturn(Urn.forTrack(1L));
        when(recommendedTrackItemTwo.getEntityUrn()).thenReturn(Urn.forTrack(2L));
    }

    @Test
    public void clickOnTrackPlaysItAndEnqueueRecommendedTracks() {
        final List<RecommendedTrackItem> trackItems = Arrays.asList(recommendedTrackItemOne, recommendedTrackItemTwo);
        PublishSubject<List<RecommendedTrackItem>> recommendedTracksForSeed = PublishSubject.create();
        when(discoveryOperations.recommendedTracksForSeed(anyLong())).thenReturn(recommendedTracksForSeed);

        final List<Urn> playQueue = Arrays.asList(recommendedTrackItemOne.getEntityUrn(), recommendedTrackItemTwo.getEntityUrn());
        PublishSubject<List<Urn>> recommendedTracks = PublishSubject.create();
        when(discoveryOperations.recommendedTracks()).thenReturn(recommendedTracks);

        Urn entityUrn = recommendedTrackItemOne.getEntityUrn();
        when(playbackOperations.playTracks(eq(recommendedTracks), eq(entityUrn), eq(0), isA(PlaySessionSource.class)))
                .thenReturn(Observable.just(PlaybackResult.success()));

        recommendedTracksForSeed.onNext(trackItems);
        recommendedTracks.onNext(playQueue);

        presenter.onBuildBinding(mock(Bundle.class));
        presenter.onRecommendedTrackClicked(recommendedTrackItemOne);
        verify(discoveryOperations).recommendedTracksForSeed(anyLong());
        verify(discoveryOperations).recommendedTracks();
        verify(playbackOperations).playTracks(eq(recommendedTracks), eq(entityUrn), eq(0), isA(PlaySessionSource.class));
    }
}