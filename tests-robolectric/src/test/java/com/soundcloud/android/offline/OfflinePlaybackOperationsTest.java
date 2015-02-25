package com.soundcloud.android.offline;


import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.likes.LikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.commands.LoadOfflineTrackUrnsCommand;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class OfflinePlaybackOperationsTest {

    @Mock private FeatureOperations featureOperations;
    @Mock private NetworkConnectionHelper connectionHelper;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private LikeOperations likeOperations;
    @Mock private LoadOfflineTrackUrnsCommand loadOfflineUrnsCommand;
    @Mock private PlaybackToastHelper toastHelper;

    private final List<Urn> trackUrns = Arrays.asList(Urn.forTrack(123L), Urn.forTrack(234L));
    private final Observable<List<Urn>> tracksObservable = Observable.just(trackUrns);
    private final PlaySessionSource playSessionSource = new PlaySessionSource(Screen.EXPLORE_AUDIO_GENRE);

    private OfflinePlaybackOperations operations;

    @Before
    public void setUp() throws Exception {
        operations = new OfflinePlaybackOperations(featureOperations, connectionHelper,
                playbackOperations, likeOperations, toastHelper, loadOfflineUrnsCommand, Schedulers.immediate());
    }

    @Test
    public void shouldCreateOfflinePlayQueueWhenFeatureEnabledAndOffline() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(connectionHelper.isNetworkConnected()).thenReturn(false);

        expect(operations.shouldCreateOfflinePlayQueue()).toBeTrue();
    }

    @Test
    public void shouldNotCreateOfflinePlayQueueWhenFeatureDisabled() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);

        expect(operations.shouldCreateOfflinePlayQueue()).toBeFalse();
    }

    @Test
    public void shouldPlayOfflineWhenFeatureEnabledAndTrackDownloaded() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        expect(operations.shouldPlayOffline(downloadedTrack())).toBeTrue();
    }

    @Test
    public void shouldNotPlayOfflineWhenFeatureEnabledAndTrackRemoved() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        expect(operations.shouldPlayOffline(removedTrack())).toBeFalse();
    }

    @Test
    public void shouldNotPlayOfflineWhenFeatureEnabledAndTrackNotDownloaded() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        expect(operations.shouldPlayOffline(notDownloadedTrack())).toBeFalse();
    }

    @Test
    public void shouldNotPlayOfflineWhenFeatureDisabled() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);

        expect(operations.shouldPlayOffline(downloadedTrack())).toBeFalse();
    }

    @Test
    public void shouldPlayAllLikedTracksWhenNoOfflinePlayQueueCreated() {
        when(likeOperations.likedTrackUrns()).thenReturn(tracksObservable);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        when(playbackOperations.playTracks(tracksObservable, trackUrns.get(0), 0, playSessionSource))
                .thenReturn(Observable.<List<Urn>>empty());

        operations.playLikes(trackUrns.get(0), 0, playSessionSource).subscribe();

        verify(playbackOperations).playTracks(tracksObservable, trackUrns.get(0), 0, playSessionSource);
    }

    @Test
    public void shouldPlayOfflineOnlyTracksWhenOfflinePlayQueueCreated() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(connectionHelper.isNetworkConnected()).thenReturn(false);
        when(loadOfflineUrnsCommand.toObservable()).thenReturn(tracksObservable);

        operations.playLikes(trackUrns.get(0), 0, playSessionSource).subscribe();

        verify(playbackOperations).playTracks(trackUrns, trackUrns.get(0), 0, playSessionSource);
    }

    @Test
    public void shouldDisplayToastWhenTrackIsNotAvailableOffline() {
        final TestObserver<List<Urn>> observer = new TestObserver<>();
        final Urn trackNotAvailableOffline = Urn.forTrack(888L);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(connectionHelper.isNetworkConnected()).thenReturn(false);
        when(loadOfflineUrnsCommand.toObservable()).thenReturn(tracksObservable);

        operations.playLikes(trackNotAvailableOffline, 0, playSessionSource).subscribe(observer);

        verify(toastHelper).showTrackUnavailableOfflineToast();
        expect(observer.getOnNextEvents()).toBeEmpty();
    }

    @Test
    public void shouldPlayOfflineOnlyTracksShuffledWhenOfflinePlayQueueCreated() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(connectionHelper.isNetworkConnected()).thenReturn(false);
        when(loadOfflineUrnsCommand.toObservable()).thenReturn(tracksObservable);
        when(playbackOperations.playTracksShuffled(tracksObservable, playSessionSource))
                .thenReturn(Observable.<List<Urn>>empty());

        operations.playTracksShuffled(playSessionSource).subscribe();

        verify(playbackOperations).playTracksShuffled(tracksObservable, playSessionSource);
    }

    @Test
    public void shouldPlayAllLikedTracksShuffledWhenNoOfflinePlayQueueCreated() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        when(likeOperations.likedTrackUrns()).thenReturn(tracksObservable);
        when(playbackOperations.playTracksShuffled(tracksObservable, playSessionSource))
                .thenReturn(Observable.<List<Urn>>empty());

        operations.playTracksShuffled(playSessionSource).subscribe();

        verify(playbackOperations).playTracksShuffled(tracksObservable, playSessionSource);
    }

    private PropertySet downloadedTrack() {
        return PropertySet.from(TrackProperty.OFFLINE_DOWNLOADED_AT.bind(new Date()));
    }

    private PropertySet removedTrack() {
        return PropertySet.from(
                TrackProperty.OFFLINE_DOWNLOADED_AT.bind(new Date()),
                TrackProperty.OFFLINE_REMOVED_AT.bind(new Date()));
    }

    private PropertySet notDownloadedTrack() {
        return PropertySet.create();
    }

}
