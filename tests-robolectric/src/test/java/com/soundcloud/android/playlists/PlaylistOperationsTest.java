package com.soundcloud.android.playlists;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistOperationsTest {

    private PlaylistOperations operations;

    @Mock private LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrns;
    @Mock private Observer<List<Urn>> urnListObserver;
    @Mock private LegacyPlaylistOperations legacyPlaylistOperations;

    @Before
    public void setUp() throws Exception {
        operations = new PlaylistOperations(Schedulers.immediate(), loadPlaylistTrackUrns, legacyPlaylistOperations);
    }

    @Test
    public void trackUrnsForPlaybackReturnsTrackUrnsFromCommand() throws Exception {
        final List<Urn> urnList = Arrays.asList(Urn.forTrack(123L), Urn.forTrack(456L));
        when(loadPlaylistTrackUrns.toObservable()).thenReturn(Observable.just(urnList));

        operations.trackUrnsForPlayback(Urn.forPlaylist(123L)).subscribe(urnListObserver);

        verify(urnListObserver).onNext(urnList);
        verify(urnListObserver).onCompleted();
    }
}