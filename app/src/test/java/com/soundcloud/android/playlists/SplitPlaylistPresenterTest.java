package com.soundcloud.android.playlists;

import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.CollapsingScrollHelper;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.os.Bundle;
import android.support.annotation.NonNull;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.List;

public class SplitPlaylistPresenterTest extends AndroidUnitTest {

    private static final Urn PLAYLIST_URN = Urn.forPlaylist(123L);

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.playlist_details_fragment, new Bundle());

    @Mock private PlaylistOperations operations;
    @Mock private SwipeRefreshAttacher swipeAttacher;
    @Mock private CollapsingScrollHelper profileScrollHelper;
    @Mock private PlaylistHeaderPresenter headerPresenter;
    @Mock private PlaylistAdapterFactory adapterFactory;
    @Mock private PlaylistAdapter adapter;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private Navigator navigator;

    private TestEventBus eventBus = new TestEventBus();
    private Bundle args;

    private PlaylistPresenter presenter;

    private final ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
    private final PropertySet track1 = ModelFixtures.create(ApiTrack.class).toPropertySet();
    private final PropertySet track2 = ModelFixtures.create(ApiTrack.class).toPropertySet();
    private PlaylistWithTracks playlistWithTracks = new PlaylistWithTracks(playlist.toPropertySet(), Arrays.asList(TrackItem.from(track1), TrackItem.from(track2)));
    private Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider = providerOf(mock(ExpandPlayerSubscriber.class));

    @Before
    public void setUp() throws Exception {
        args = PlaylistDetailFragment.createBundle(PLAYLIST_URN, Screen.PLAYLIST_DETAILS, null, null, false);
        fragmentRule.setFragmentArguments(args);

        when(adapterFactory.create(any(PlaylistHeaderPresenter.class))).thenReturn(adapter);
        when(operations.playlist(PLAYLIST_URN)).thenReturn(Observable.just(playlistWithTracks));

        presenter = getPlaylistPresenter(eventBus);
    }

    @Test
    public void shouldLoadInitialItemsInOnCreate() {
        presenter.onCreate(fragmentRule.getFragment(), args);

        verify(adapter).onNext(listItems());
    }

    @Test
    public void shouldSetHeaderInOnViewCreated() {
        presenter.onCreate(fragmentRule.getFragment(), args);
        presenter.onViewCreated(fragmentRule.getFragment(),fragmentRule.getView(), args);

        verify(headerPresenter).setPlaylist(PlaylistHeaderItem.create(playlistWithTracks, getPlaySessionSource()));
    }

    private PlaySessionSource getPlaySessionSource() {
        return PlaySessionSource.forPlaylist(
                Screen.fromBundle(args),
                PLAYLIST_URN,
                playlistWithTracks.getCreatorUrn(),
                playlistWithTracks.getTrackCount());
    }

    private List<ListItem> listItems() {
        return Arrays.<ListItem>asList(TrackItem.from(track1), TrackItem.from(track2));
    }

    @NonNull
    public PlaylistPresenter getPlaylistPresenter(EventBus eventBus) {
        return new SplitPlaylistPresenter(operations, swipeAttacher, SplitPlaylistPresenterTest.this.headerPresenter,
                adapterFactory, eventBus, navigator,
                new ViewStrategyFactory(providerOf(eventBus), providerOf(playbackInitiator), providerOf(operations), providerOf(expandPlayerSubscriberProvider)));
    }
}
