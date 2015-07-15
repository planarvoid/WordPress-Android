package com.soundcloud.android.offline;

import static com.soundcloud.android.playback.PlaybackResult.ErrorReason.TRACK_UNAVAILABLE_OFFLINE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.likes.TrackLikeOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playlists.PlaylistOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.List;

public class OfflinePlaybackOperationsTest extends AndroidUnitTest {

    @Mock private TrackLikeOperations likeOperations;
    @Mock private PlaylistOperations playlistOperations;
    @Mock private FeatureOperations featureOperations;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private NetworkConnectionHelper connectionHelper;
    @Mock private TrackDownloadsStorage trackDownloadsStorage;
    @Captor private ArgumentCaptor<Observable<List<Urn>>> playedTracksCaptor;

    private final List<Urn> trackUrns = Arrays.asList(Urn.forTrack(123L), Urn.forTrack(234L));
    private final Observable<List<Urn>> tracksObservable = Observable.just(trackUrns);
    private final PlaySessionSource playSessionSource = new PlaySessionSource(Screen.EXPLORE_AUDIO_GENRE);
    private final Urn playlistUrn = Urn.forPlaylist(456L);

    private OfflinePlaybackOperations operations;

    @Before
    public void setUp() throws Exception {
        operations = new OfflinePlaybackOperations(featureOperations, connectionHelper,
                playbackOperations, likeOperations, playlistOperations, trackDownloadsStorage,
                Schedulers.immediate());
    }

    @Test
    public void shouldCreateOfflinePlayQueueWhenFeatureEnabledAndOffline() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(connectionHelper.isNetworkConnected()).thenReturn(false);

        assertThat(operations.shouldCreateOfflinePlayQueue()).isTrue();
    }

    @Test
    public void shouldNotCreateOfflinePlayQueueWhenFeatureDisabled() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);

        assertThat(operations.shouldCreateOfflinePlayQueue()).isFalse();
    }

    @Test
    public void shouldPlayOfflineWhenFeatureEnabledAndTrackDownloaded() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        assertThat(operations.shouldPlayOffline(downloadedTrack())).isTrue();
    }

    @Test
    public void shouldNotPlayOfflineWhenFeatureEnabledAndTrackRemoved() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        assertThat(operations.shouldPlayOffline(removedTrack())).isFalse();
    }

    @Test
    public void shouldNotPlayOfflineWhenFeatureEnabledAndTrackNotDownloaded() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        assertThat(operations.shouldPlayOffline(notDownloadedTrack())).isFalse();
    }

    @Test
    public void shouldNotPlayOfflineWhenFeatureDisabled() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);

        assertThat(operations.shouldPlayOffline(downloadedTrack())).isFalse();
    }

    @Test
    public void shouldPlayAllLikedTracksWhenNoOfflinePlayQueueCreated() {
        when(likeOperations.likedTrackUrns()).thenReturn(tracksObservable);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        when(playbackOperations.playTracks(tracksObservable, trackUrns.get(0), 0, playSessionSource))
                .thenReturn(Observable.<PlaybackResult>empty());

        operations.playLikes(trackUrns.get(0), 0, playSessionSource).subscribe();

        verify(playbackOperations).playTracks(tracksObservable, trackUrns.get(0), 0, playSessionSource);
    }

    @Test
    public void shouldPlayOfflineOnlyLikesWhenOfflinePlayQueueCreated() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(connectionHelper.isNetworkConnected()).thenReturn(false);
        when(trackDownloadsStorage.likesUrns()).thenReturn(tracksObservable);

        operations.playLikes(trackUrns.get(0), 0, playSessionSource).subscribe();

        verify(playbackOperations).playTracks(trackUrns, trackUrns.get(0), 0, playSessionSource);
    }

    @Test
    public void shouldThrowExceptionWhenLikedTrackIsNotAvailableOffline() {
        final TestObserver<PlaybackResult> observer = new TestObserver<>();
        final Urn trackNotAvailableOffline = Urn.forTrack(888L);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(connectionHelper.isNetworkConnected()).thenReturn(false);
        when(trackDownloadsStorage.likesUrns()).thenReturn(tracksObservable);

        operations.playLikes(trackNotAvailableOffline, 0, playSessionSource).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0).isSuccess()).isFalse();
        assertThat(observer.getOnNextEvents().get(0).getErrorReason()).isEqualTo(TRACK_UNAVAILABLE_OFFLINE);
    }

    @Test
    public void shouldPlayOfflineLikesOnlyTracksShuffledWhenOfflinePlayQueueCreated() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(connectionHelper.isNetworkConnected()).thenReturn(false);
        when(trackDownloadsStorage.likesUrns()).thenReturn(tracksObservable);
        when(playbackOperations.playTracksShuffled(playedTracksCaptor.capture(), refEq(playSessionSource)))
                .thenReturn(Observable.<PlaybackResult>empty());

        operations.playLikedTracksShuffled(playSessionSource).subscribe();

        assertThat(playedTracksCaptor.getValue().toBlocking().single()).isEqualTo(tracksObservable.toBlocking().single());
    }

    @Test
    public void shouldPlayAllLikedTracksShuffledWhenNoOfflinePlayQueueCreated() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        when(likeOperations.likedTrackUrns()).thenReturn(tracksObservable);
        when(playbackOperations.playTracksShuffled(playedTracksCaptor.capture(), refEq(playSessionSource)))
                .thenReturn(Observable.<PlaybackResult>empty());

        operations.playLikedTracksShuffled(playSessionSource).subscribe();

        assertThat(playedTracksCaptor.getValue().toBlocking().single()).isEqualTo(tracksObservable.toBlocking().single());
    }

    @Test
    public void shouldPlayOfflinePlaylistTracksShuffledWhenOfflinePlayQueueCreated() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(connectionHelper.isNetworkConnected()).thenReturn(false);
        when(trackDownloadsStorage.playlistTrackUrns(playlistUrn)).thenReturn(tracksObservable);
        when(playbackOperations.playTracksShuffled(playedTracksCaptor.capture(), refEq(playSessionSource)))
                .thenReturn(Observable.<PlaybackResult>empty());

        operations.playPlaylistShuffled(playlistUrn, playSessionSource).subscribe();

        assertThat(playedTracksCaptor.getValue().toBlocking().single()).isEqualTo(tracksObservable.toBlocking().single());
    }

    @Test
    public void shouldPlayAllPlaylistTracksShuffledWhenNoOfflinePlayQueueCreated() {
        when(playlistOperations.trackUrnsForPlayback(playlistUrn)).thenReturn(tracksObservable);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        when(playbackOperations.playTracksShuffled(playedTracksCaptor.capture(), refEq(playSessionSource)))
                .thenReturn(Observable.<PlaybackResult>empty());

        operations.playPlaylistShuffled(playlistUrn, playSessionSource).subscribe();

        assertThat(playedTracksCaptor.getValue().toBlocking().single()).isEqualTo(tracksObservable.toBlocking().single());
    }

    @Test
    public void shouldPlayAllPlaylistTracksWhenNoOfflinePlayQueueCreated() {
        when(playlistOperations.trackUrnsForPlayback(playlistUrn)).thenReturn(tracksObservable);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        when(playbackOperations.playTracks(tracksObservable, trackUrns.get(0), 0, playSessionSource))
                .thenReturn(Observable.<PlaybackResult>empty());

        operations.playPlaylist(playlistUrn, trackUrns.get(0), 0, playSessionSource).subscribe();

        verify(playbackOperations).playTracks(tracksObservable, trackUrns.get(0), 0, playSessionSource);
    }

    @Test
    public void shouldPlayOfflineTracksFromAPlaylistWhenOfflinePlayQueueCreated() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(connectionHelper.isNetworkConnected()).thenReturn(false);
        when(trackDownloadsStorage.playlistTrackUrns(playlistUrn)).thenReturn(tracksObservable);

        operations.playPlaylist(playlistUrn, trackUrns.get(0), 0, playSessionSource).subscribe();

        verify(playbackOperations).playTracks(trackUrns, trackUrns.get(0), 0, playSessionSource);
    }

    @Test
    public void shouldThrowExceptionWhenPlaylistTrackIsNotAvailableOffline() {
        final TestObserver<PlaybackResult> observer = new TestObserver<>();
        final Urn trackNotAvailableOffline = Urn.forTrack(888L);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(connectionHelper.isNetworkConnected()).thenReturn(false);
        when(trackDownloadsStorage.playlistTrackUrns(playlistUrn)).thenReturn(tracksObservable);

        operations.playPlaylist(playlistUrn, trackNotAvailableOffline, 0, playSessionSource).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0).isSuccess()).isFalse();
        assertThat(observer.getOnNextEvents().get(0).getErrorReason()).isEqualTo(TRACK_UNAVAILABLE_OFFLINE);
    }

    private PropertySet downloadedTrack() {
        return PropertySet.from(OfflineProperty.OFFLINE_STATE.bind(OfflineState.DOWNLOADED));
    }

    private PropertySet removedTrack() {
        return PropertySet.from(OfflineProperty.OFFLINE_STATE.bind(OfflineState.NO_OFFLINE));
    }

    private PropertySet notDownloadedTrack() {
        return PropertySet.create();
    }

}
