package com.soundcloud.android.playlists;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Observer;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistOperationsTest {

    private PlaylistOperations operations;

    @Mock private Observer<List<Urn>> urnListObserver;
    @Mock private Observer<PlaylistInfo> playlistInfoObserver;
    @Mock private SyncInitiator syncInitiator;
    @Mock private LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrns;
    @Mock private LoadPlaylistCommand loadPlaylistCommand;
    @Mock private LoadPlaylistTracksCommand loadPlaylistTracksCommand;

    private ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
    final PropertySet track1 = ModelFixtures.create(ApiTrack.class).toPropertySet();
    final PropertySet track2 = ModelFixtures.create(ApiTrack.class).toPropertySet();

    @Before
    public void setUp() throws Exception {
        operations = new PlaylistOperations(Schedulers.immediate(), syncInitiator, loadPlaylistCommand, loadPlaylistTrackUrns, loadPlaylistTracksCommand);
    }

    @Test
    public void trackUrnsForPlaybackReturnsTrackUrnsFromCommand() throws Exception {
        final List<Urn> urnList = Arrays.asList(Urn.forTrack(123L), Urn.forTrack(456L));
        when(loadPlaylistTrackUrns.toObservable()).thenReturn(Observable.just(urnList));

        operations.trackUrnsForPlayback(Urn.forPlaylist(123L)).subscribe(urnListObserver);

        verify(urnListObserver).onNext(urnList);
        verify(urnListObserver).onCompleted();
    }

    @Test
    public void loadsPlaylistWithTracksFromStorage() throws Exception {
        when(loadPlaylistTracksCommand.toObservable()).thenReturn(Observable.<List<PropertySet>>just(Lists.newArrayList(track1, track2)));
        when(loadPlaylistCommand.toObservable()).thenReturn(Observable.just(playlist.toPropertySet()));

        operations.playlistInfo(playlist.getUrn()).subscribe(playlistInfoObserver);

        verify(playlistInfoObserver).onNext(new PlaylistInfo(playlist.toPropertySet(), Arrays.asList(track1, track2)));
        verify(playlistInfoObserver).onCompleted();
    }

    @Test
    public void updatedPlaylistSyncsThenLoadsFromStorage() throws Exception {
        when(syncInitiator.syncPlaylist(playlist.getUrn())).thenReturn(Observable.just(true));
        when(loadPlaylistTracksCommand.toObservable()).thenReturn(Observable.<List<PropertySet>>just(Lists.newArrayList(track1, track2)));
        when(loadPlaylistCommand.toObservable()).thenReturn(Observable.just(playlist.toPropertySet()));

        operations.updatedPlaylistInfo(playlist.getUrn()).subscribe(playlistInfoObserver);

        InOrder inOrder = Mockito.inOrder(syncInitiator, playlistInfoObserver);
        inOrder.verify(syncInitiator).syncPlaylist(playlist.getUrn());
        inOrder.verify(playlistInfoObserver).onNext(new PlaylistInfo(playlist.toPropertySet(), Arrays.asList(track1, track2)));
        inOrder.verify(playlistInfoObserver).onCompleted();
    }

    @Test
    public void loadsPlaylistAndSyncsBeforeEmittingIfPlaylistMetaDataMissing() throws Exception {
        when(syncInitiator.syncPlaylist(playlist.getUrn())).thenReturn(Observable.just(true));
        when(loadPlaylistTracksCommand.toObservable()).thenReturn(Observable.<List<PropertySet>>just(Lists.newArrayList(track1, track2)));
        when(loadPlaylistCommand.toObservable()).thenReturn(Observable.just(PropertySet.<PropertySet>create()), Observable.just(playlist.toPropertySet()));

        operations.playlistInfo(playlist.getUrn()).subscribe(playlistInfoObserver);

        InOrder inOrder = Mockito.inOrder(syncInitiator, playlistInfoObserver);
        inOrder.verify(syncInitiator).syncPlaylist(playlist.getUrn());
        inOrder.verify(playlistInfoObserver).onNext(new PlaylistInfo(playlist.toPropertySet(), Arrays.asList(track1, track2)));
        inOrder.verify(playlistInfoObserver).onCompleted();
    }

    @Test
    public void loadsPlaylistAndEmitsAgainAfterSyncIfNoTracksAvailable() throws Exception {
        final List<PropertySet> emptyTrackList = Collections.emptyList();
        final List<PropertySet> trackList = Arrays.asList(track1, track2);
        when(syncInitiator.syncPlaylist(playlist.getUrn())).thenReturn(Observable.just(true));
        when(loadPlaylistTracksCommand.toObservable()).thenReturn(Observable.just(emptyTrackList), Observable.just(trackList));
        when(loadPlaylistCommand.toObservable()).thenReturn(Observable.just(playlist.toPropertySet()));

        operations.playlistInfo(playlist.getUrn()).subscribe(playlistInfoObserver);

        InOrder inOrder = Mockito.inOrder(syncInitiator, playlistInfoObserver);
        inOrder.verify(syncInitiator).syncPlaylist(playlist.getUrn());
        inOrder.verify(playlistInfoObserver).onNext(new PlaylistInfo(playlist.toPropertySet(), emptyTrackList));
        inOrder.verify(playlistInfoObserver).onNext(new PlaylistInfo(playlist.toPropertySet(), trackList));
        inOrder.verify(playlistInfoObserver).onCompleted();
    }

    @Test
    public void loadsPlaylistAndEmitsOnceIfPlaylistHasEmptyTrackCount() throws Exception {
        final List<PropertySet> emptyTrackList = Collections.emptyList();
        final PropertySet playlistProperties = playlist.toPropertySet();
        playlistProperties.put(PlaylistProperty.TRACK_COUNT, 0);

        when(loadPlaylistTracksCommand.toObservable()).thenReturn(Observable.just(emptyTrackList));
        when(loadPlaylistCommand.toObservable()).thenReturn(Observable.just(playlistProperties));

        operations.playlistInfo(playlist.getUrn()).subscribe(playlistInfoObserver);

        verify(syncInitiator, never()).syncPlaylist(playlist.getUrn());
        verify(playlistInfoObserver).onNext(new PlaylistInfo(playlistProperties, emptyTrackList));
        verify(playlistInfoObserver).onCompleted();
    }

    @Test
    public void loadsLocalPlaylistAndRequestsMyPlaylistSyncWhenEmitting() throws Exception {
        final List<PropertySet> trackList = Arrays.asList(track1, track2);
        final PropertySet playlistProperties = playlist.toPropertySet();
        playlistProperties.put(PlaylistProperty.URN, Urn.forTrack(-123L)); // make it a local playlist

        when(loadPlaylistTracksCommand.toObservable()).thenReturn(Observable.just(trackList));
        when(loadPlaylistCommand.toObservable()).thenReturn(Observable.just(playlistProperties));

        operations.playlistInfo(playlist.getUrn()).subscribe(playlistInfoObserver);

        InOrder inOrder = Mockito.inOrder(syncInitiator, playlistInfoObserver);
        inOrder.verify(syncInitiator).syncLocalPlaylists();
        inOrder.verify(playlistInfoObserver).onNext(new PlaylistInfo(playlistProperties, trackList));
        inOrder.verify(playlistInfoObserver).onCompleted();
        verify(syncInitiator, never()).syncPlaylist(playlistProperties.get(PlaylistProperty.URN));
    }
}