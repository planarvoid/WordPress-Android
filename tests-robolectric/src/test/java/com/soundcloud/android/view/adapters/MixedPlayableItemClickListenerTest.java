package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Actions;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class MixedPlayableItemClickListenerTest {

    private MixedPlayableItemClickListener listener;

    @Mock private PlaybackOperations playbackOperations;
    @Mock private MixedPlayableAdapter adapter;
    @Mock private AdapterView adapterView;
    @Mock private View view;
    @Mock private ExpandPlayerSubscriber expandPlayerSubscriber;

    private final Screen screen = Screen.ACTIVITIES;
    private final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(Urn.forTrack(123));

    @Before
    public void setUp() throws Exception {
        listener = new MixedPlayableItemClickListener(playbackOperations, InjectionSupport.providerOf(expandPlayerSubscriber),
                screen, searchQuerySourceInfo);

        when(adapterView.getAdapter()).thenReturn(adapter);
        when(view.getContext()).thenReturn(Robolectric.application);
    }

    @Test
    public void itemClickOnTrackStartsPlaybackWithJustTracks() throws Exception {
        final TrackItem track1 = ModelFixtures.create(TrackItem.class);
        final TrackItem track2 = ModelFixtures.create(TrackItem.class);
        when(adapter.getItems()).thenReturn(Arrays.asList(
                ModelFixtures.create(PlaylistItem.class),
                track1,
                ModelFixtures.create(PlaylistItem.class),
                track2
        ));

        final List<Urn> trackList = Arrays.asList(track1.getEntityUrn(), track2.getEntityUrn());
        final PlaybackResult playbackResult = PlaybackResult.success();
        when(playbackOperations.playTracks(trackList, 1, new PlaySessionSource(screen))).thenReturn(Observable.just(playbackResult));

        listener.onItemClick(adapterView, view, 3, 3);

        verify(expandPlayerSubscriber).onNext(playbackResult);
        verify(expandPlayerSubscriber).onCompleted();
    }

    @Test
    public void itemClickOnPlaylistSendsPlaylistDetailIntent() throws Exception {
        final PlaylistItem playlistItem = ModelFixtures.create(PlaylistItem.class);
        when(adapter.getItems()).thenReturn(Arrays.asList(
                ModelFixtures.create(PlaylistItem.class),
                ModelFixtures.create(TrackItem.class),
                playlistItem,
                ModelFixtures.create(TrackItem.class)
        ));

        listener.onItemClick(adapterView, view, 2, 2);

        Intent nextStartedActivity = Robolectric.shadowOf(Robolectric.application).getNextStartedActivity();
        expect(nextStartedActivity).not.toBeNull();
        expect(nextStartedActivity.getAction()).toEqual(Actions.PLAYLIST);
        expect(nextStartedActivity.getExtras().get(PlaylistDetailActivity.EXTRA_URN)).toEqual(playlistItem.getEntityUrn());
    }
}