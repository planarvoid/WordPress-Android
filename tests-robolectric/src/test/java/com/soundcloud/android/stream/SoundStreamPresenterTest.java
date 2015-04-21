package com.soundcloud.android.stream;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.PullToRefreshWrapper;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.RxTestHelper;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ListView;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SoundStreamPresenterTest {

    private SoundStreamPresenter presenter;

    @Mock private SoundStreamOperations streamOperations;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private SoundStreamAdapter adapter;
    @Mock private ImageOperations imageOperations;
    @Mock private PullToRefreshWrapper pullToRefreshWrapper;

    @Mock private Fragment fragment;
    @Mock private View view;
    @Mock private ListView listView;
    @Mock private EmptyView emptyView;

    private TestSubscriber testSubscriber = new TestSubscriber();
    private Provider expandPlayerSubscriberProvider = providerOf(testSubscriber);

    @Before
    public void setUp() throws Exception {
        presenter = new SoundStreamPresenter(streamOperations, playbackOperations, adapter, imageOperations,
                pullToRefreshWrapper, expandPlayerSubscriberProvider);
        when(streamOperations.existingStreamItems()).thenReturn(Observable.<List<PropertySet>>empty());
        when(streamOperations.pager()).thenReturn(RxTestHelper.<List<PropertySet>>pagerWithSinglePage());
        when(view.findViewById(android.R.id.list)).thenReturn(listView);
        when(view.findViewById(android.R.id.empty)).thenReturn(emptyView);
    }

    @Test
    public void setsItemClickHandlerOnList() {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        verify(listView).setOnItemClickListener(presenter);
    }

    @Test
    public void playsTracksOnTrackItemClick() {
        final TrackItem clickedTrack = ModelFixtures.create(TrackItem.class);
        final List<Urn> streamTrackUrns = Arrays.asList(clickedTrack.getEntityUrn(), Urn.forTrack(634L));
        final Observable<List<Urn>> streamTracks = Observable.just(streamTrackUrns);

        when(adapter.getItem(0)).thenReturn(clickedTrack);
        when(streamOperations.trackUrnsForPlayback()).thenReturn(streamTracks);
        when(playbackOperations.playTracks(eq(streamTracks), eq(clickedTrack.getEntityUrn()), eq(0), isA(PlaySessionSource.class)))
                .thenReturn(streamTracks);

        presenter.onItemClick(listView, view, 0, 0);

        testSubscriber.assertReceivedOnNext(Arrays.asList(streamTrackUrns));
    }

    @Test
    public void opensPlaylistScreenOnPlaylistItemClick() {
        final PlaylistItem playlistItem = ModelFixtures.create(PlaylistItem.class);
        when(adapter.getItem(0)).thenReturn(playlistItem);
        when(view.getContext()).thenReturn(Robolectric.application);

        presenter.onItemClick(listView, view, 0, 0);

        final Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(Actions.PLAYLIST);
        expect(intent.getParcelableExtra(PlaylistDetailActivity.EXTRA_URN)).toEqual(playlistItem.getEntityUrn());
    }

    @Test
    public void configuresEmptyStateImage() {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(emptyView).setImage(R.drawable.empty_stream);
    }

    @Test
    public void configuresEmptyStateForOnboardingFailure() {
        presenter.onCreate(fragment, null);
        presenter.setOnboardingSuccess(false);

        presenter.onViewCreated(fragment, view, null);

        verify(emptyView).setMessageText(R.string.error_onboarding_fail);
    }

    @Test
    public void configuresEmptyStateForOnboardingSuccess() {
        presenter.onCreate(fragment, null);
        presenter.setOnboardingSuccess(true);

        presenter.onViewCreated(fragment, view, null);

        verify(emptyView).setMessageText(R.string.list_empty_stream_message);
        verify(emptyView).setActionText(R.string.list_empty_stream_action);
    }

    @Test
    public void updatesLastSeenOnResume() {
        presenter.onCreate(fragment, null);
        presenter.onResume(fragment);

        verify(streamOperations).updateLastSeen();
    }

}
