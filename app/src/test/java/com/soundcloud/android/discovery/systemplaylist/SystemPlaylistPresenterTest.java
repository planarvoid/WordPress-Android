package com.soundcloud.android.discovery.systemplaylist;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.TrackingStateProvider;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.olddiscovery.newforyou.NewForYou;
import com.soundcloud.android.olddiscovery.newforyou.NewForYouOperations;
import com.soundcloud.android.playback.ExpandPlayerSingleObserver;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestSubscribers;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.content.res.Resources;
import android.os.Bundle;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class SystemPlaylistPresenterTest extends AndroidUnitTest {
    private static final Optional<Urn> QUERY_URN = Optional.of(new Urn("my:fake:systemPlaylistUrn"));
    private static final Optional<Date> DATE = Optional.of(new TestDateProvider().getCurrentDate());
    private static final List<Track> TRACKS = ModelFixtures.tracks(3);
    private static final TrackItem FIRST_TRACK_ITEM = ModelFixtures.trackItem(TRACKS.get(0));
    private static final TrackItem SECOND_TRACK_ITEM = ModelFixtures.trackItem(TRACKS.get(1));
    private static final TrackItem THIRD_TRACK_ITEM = ModelFixtures.trackItem(TRACKS.get(2));
    private static final Urn URN = Urn.forSystemPlaylist("123");
    private static final Optional<String> TITLE = Optional.of("Title");
    private static final Optional<String> DESCRIPTION = Optional.of("Description");
    private static final Optional<String> ARTWORK_URL = Optional.of("https://cool.artwork/url.jpg");
    private static final Optional<String> TRACKING_FEATURE_NAME = Optional.of("The Upload");
    private static final Optional<String> NEW_FOR_YOU_TITLE = Optional.of("The Upload");
    private static final Optional<String> NEW_FOR_YOU_DESCRIPTION = Optional.of("Something uploaded");
    private static final NewForYou NEW_FOR_YOU = NewForYou.create(DATE.get(), QUERY_URN.get(), TRACKS);
    private static final SystemPlaylist SYSTEM_PLAYLIST = SystemPlaylist.create(URN, QUERY_URN, TITLE, DESCRIPTION, TRACKS, DATE, ARTWORK_URL, TRACKING_FEATURE_NAME);
    private static final SystemPlaylist EMPTY_SYSTEM_PLAYLIST = SystemPlaylist.create(URN, QUERY_URN, TITLE, DESCRIPTION, Lists.newArrayList(), DATE, ARTWORK_URL, TRACKING_FEATURE_NAME);
    private static final SystemPlaylist NEW_FOR_YOU_SYSTEM_PLAYLIST = SystemPlaylist.create(Urn.NOT_SET,
                                                                                            QUERY_URN,
                                                                                            NEW_FOR_YOU_TITLE,
                                                                                            NEW_FOR_YOU_DESCRIPTION,
                                                                                            TRACKS,
                                                                                            DATE,
                                                                                            Optional.absent(),
                                                                                            Optional.absent());
    private static final String METADATA = "duration";
    private static final Optional<String> LAST_UPDATED = Optional.of("last_updated");

    private static final SystemPlaylistItem.Header HEADER = SystemPlaylistItem.Header.create(URN,
                                                                                             TITLE,
                                                                                             DESCRIPTION,
                                                                                             METADATA,
                                                                                             LAST_UPDATED,
                                                                                             SYSTEM_PLAYLIST.imageResource(),
                                                                                             QUERY_URN,
                                                                                             TRACKING_FEATURE_NAME,
                                                                                             true);
    private static final SystemPlaylistItem.Header NEW_FOR_YOU_HEADER = SystemPlaylistItem.Header.create(Urn.NOT_SET,
                                                                                                         NEW_FOR_YOU_TITLE,
                                                                                                         NEW_FOR_YOU_DESCRIPTION,
                                                                                                         METADATA,
                                                                                                         LAST_UPDATED,
                                                                                                         NEW_FOR_YOU_SYSTEM_PLAYLIST.imageResource(),
                                                                                                         QUERY_URN,
                                                                                                         Optional.absent(),
                                                                                                         true);

    private static final SystemPlaylistItem.Track FIRST = SystemPlaylistItem.Track.create(URN, FIRST_TRACK_ITEM, QUERY_URN, TRACKING_FEATURE_NAME);
    private static final SystemPlaylistItem.Track SECOND = SystemPlaylistItem.Track.create(URN, SECOND_TRACK_ITEM, QUERY_URN, TRACKING_FEATURE_NAME);
    private static final SystemPlaylistItem.Track THIRD = SystemPlaylistItem.Track.create(URN, THIRD_TRACK_ITEM, QUERY_URN, TRACKING_FEATURE_NAME);

    private static final SystemPlaylistItem.Track NEW_FOR_YOU_FIRST = SystemPlaylistItem.Track.createNewForYouTrack(Urn.NOT_SET, FIRST_TRACK_ITEM, QUERY_URN, Optional.absent());
    private static final SystemPlaylistItem.Track NEW_FOR_YOU_SECOND = SystemPlaylistItem.Track.createNewForYouTrack(Urn.NOT_SET, SECOND_TRACK_ITEM, QUERY_URN, Optional.absent());
    private static final SystemPlaylistItem.Track NEW_FOR_YOU_THIRD = SystemPlaylistItem.Track.createNewForYouTrack(Urn.NOT_SET, THIRD_TRACK_ITEM, QUERY_URN, Optional.absent());

    private static final ArrayList<SystemPlaylistItem> ADAPTER_ITEMS = newArrayList(HEADER, FIRST, SECOND, THIRD);
    private static final ArrayList<SystemPlaylistItem> NEW_FOR_YOU_ADAPTER_ITEMS = newArrayList(NEW_FOR_YOU_HEADER, NEW_FOR_YOU_FIRST, NEW_FOR_YOU_SECOND, NEW_FOR_YOU_THIRD);
    private static final Bundle bundle = spy(Bundle.class);

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.default_recyclerview_with_refresh, bundle);

    @Mock SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock NewForYouOperations newForYouOperations;
    @Mock SystemPlaylistOperations systemPlaylistOperations;
    @Mock SystemPlaylistAdapterFactory systemPlaylistAdapterFactory;
    @Mock SystemPlaylistAdapter systemPlaylistAdapter;
    @Mock PlaybackInitiator playbackInitiator;
    @Mock Resources resources;
    @Mock PlaySessionStateProvider playSessionStateProvider;
    @Mock EventTracker eventTracker;
    @Mock TrackingStateProvider trackingStateProvider;

    private final TestEventBusV2 eventBus = new TestEventBusV2();
    private final Provider<ExpandPlayerSingleObserver> expandPlayerSubscriberProvider = TestSubscribers.expandPlayerObserver(eventBus);
    private SystemPlaylistPresenter presenter;

    @Before
    public void setUp() {
        Mockito.reset(bundle);
        when(systemPlaylistAdapterFactory.create(any(SystemPlaylistHeaderRenderer.Listener.class), any(TrackItemRenderer.Listener.class))).thenReturn(systemPlaylistAdapter);
        when(systemPlaylistOperations.systemPlaylist(URN)).thenReturn(Maybe.just(SYSTEM_PLAYLIST));
        when(newForYouOperations.newForYou()).thenReturn(Single.just(NEW_FOR_YOU));
        when(resources.getString(eq(R.string.system_playlist_duration), any(Object.class), any(Object.class))).thenReturn(METADATA);
        when(resources.getQuantityString(eq(R.plurals.elapsed_seconds_ago), any(Integer.class), any(Integer.class))).thenReturn(LAST_UPDATED.get());
        when(resources.getString(eq(R.string.system_playlist_updated_at), any(Object.class))).thenReturn(LAST_UPDATED.get());
        when(resources.getString(eq(R.string.new_for_you_title))).thenReturn(NEW_FOR_YOU_TITLE.get());
        when(resources.getString(eq(R.string.new_for_you_intro))).thenReturn(NEW_FOR_YOU_DESCRIPTION.get());
        when(playbackInitiator.playTracks(any(List.class), any(Integer.class), any(PlaySessionSource.class))).thenReturn(Single.just(mock(PlaybackResult.class)));
        when(playSessionStateProvider.isCurrentlyPlaying(FIRST_TRACK_ITEM.getUrn())).thenReturn(false);

        presenter = new SystemPlaylistPresenter(swipeRefreshAttacher,
                                                systemPlaylistOperations,
                                                newForYouOperations,
                                                systemPlaylistAdapterFactory,
                                                playbackInitiator,
                                                expandPlayerSubscriberProvider,
                                                resources,
                                                eventBus,
                                                playSessionStateProvider,
                                                ModelFixtures.entityItemCreator(),
                                                eventTracker,
                                                trackingStateProvider);
    }

    @Test
    public void mapsNewForYouToViewModels() {
        when(bundle.getBoolean(SystemPlaylistFragment.EXTRA_FOR_NEW_FOR_YOU, false)).thenReturn(true);

        presenter.onCreate(fragmentRule.getFragment(), null);

        verify(systemPlaylistAdapter).onNext(NEW_FOR_YOU_ADAPTER_ITEMS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsForSystemPlaylistWithoutUrn() {
        when(bundle.getBoolean(SystemPlaylistFragment.EXTRA_FOR_NEW_FOR_YOU, false)).thenReturn(false);

        presenter.onCreate(fragmentRule.getFragment(), null);
    }

    @Test
    public void mapsSystemPlaylistToViewModels() {
        when(bundle.getBoolean(SystemPlaylistFragment.EXTRA_FOR_NEW_FOR_YOU, false)).thenReturn(false);
        when(bundle.getString(SystemPlaylistFragment.EXTRA_PLAYLIST_URN)).thenReturn(URN.getContent());

        presenter.onCreate(fragmentRule.getFragment(), null);

        verify(systemPlaylistAdapter).onNext(ADAPTER_ITEMS);
    }

    @Test
    public void trackItemClickedNewForYouStartsPlaybackFromCorrectPosition() {
        when(bundle.getBoolean(SystemPlaylistFragment.EXTRA_FOR_NEW_FOR_YOU, false)).thenReturn(true);

        final int position = 1;
        final int finalPosition = 0;
        final PlaySessionSource playSessionSource = PlaySessionSource.forNewForYou(Screen.NEW_FOR_YOU.get(),
                                                                                   finalPosition,
                                                                                   QUERY_URN.get());

        when(systemPlaylistAdapter.getItems()).thenReturn(NEW_FOR_YOU_ADAPTER_ITEMS);
        when(systemPlaylistAdapter.getItem(position)).thenReturn(NEW_FOR_YOU_FIRST);
        when(systemPlaylistAdapter.getItemCount()).thenReturn(NEW_FOR_YOU_ADAPTER_ITEMS.size());

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.trackItemClicked(TRACKS.get(0).urn(), position);

        verify(playbackInitiator).playTracks(newArrayList(FIRST_TRACK_ITEM.getUrn(), SECOND_TRACK_ITEM.getUrn(), THIRD_TRACK_ITEM.getUrn()), finalPosition, playSessionSource);
    }

    @Test
    public void playClickedNewForYouStartsPlaybackFromBeginning() {
        when(bundle.getBoolean(SystemPlaylistFragment.EXTRA_FOR_NEW_FOR_YOU, false)).thenReturn(true);

        final int adapterPosition = 0;
        final int playbackPosition = 0;
        final PlaySessionSource playSessionSource = PlaySessionSource.forNewForYou(Screen.NEW_FOR_YOU.get(),
                                                                                   playbackPosition,
                                                                                   QUERY_URN.get());

        when(systemPlaylistAdapter.getItems()).thenReturn(NEW_FOR_YOU_ADAPTER_ITEMS);
        when(systemPlaylistAdapter.getItem(adapterPosition)).thenReturn(NEW_FOR_YOU_FIRST);
        when(systemPlaylistAdapter.getItemCount()).thenReturn(NEW_FOR_YOU_ADAPTER_ITEMS.size());

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.playClicked();

        verify(playbackInitiator).playTracks(newArrayList(FIRST_TRACK_ITEM.getUrn(), SECOND_TRACK_ITEM.getUrn(), THIRD_TRACK_ITEM.getUrn()), playbackPosition, playSessionSource);
    }

    @Test
    public void trackItemClickedStartsPlaybackFromCorrectPosition() {
        when(bundle.getBoolean(SystemPlaylistFragment.EXTRA_FOR_NEW_FOR_YOU, false)).thenReturn(false);
        when(bundle.getString(SystemPlaylistFragment.EXTRA_PLAYLIST_URN)).thenReturn(URN.getContent());

        final int position = 1;
        final int finalPosition = 0;
        final PlaySessionSource playSessionSource = PlaySessionSource.forSystemPlaylist(Screen.SYSTEM_PLAYLIST.get(),
                                                                                        TRACKING_FEATURE_NAME,
                                                                                        finalPosition,
                                                                                        QUERY_URN,
                                                                                        URN,
                                                                                        3);

        when(systemPlaylistAdapter.getItems()).thenReturn(ADAPTER_ITEMS);
        when(systemPlaylistAdapter.getItem(position)).thenReturn(FIRST);
        when(systemPlaylistAdapter.getItemCount()).thenReturn(ADAPTER_ITEMS.size());

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.trackItemClicked(TRACKS.get(0).urn(), position);

        verify(playbackInitiator).playTracks(newArrayList(FIRST_TRACK_ITEM.getUrn(), SECOND_TRACK_ITEM.getUrn(), THIRD_TRACK_ITEM.getUrn()), finalPosition, playSessionSource);
    }

    @Test
    public void playClickedStartsPlaybackFromBeginning() {
        when(bundle.getBoolean(SystemPlaylistFragment.EXTRA_FOR_NEW_FOR_YOU, false)).thenReturn(false);
        when(bundle.getString(SystemPlaylistFragment.EXTRA_PLAYLIST_URN)).thenReturn(URN.getContent());

        final int adapterPosition = 0;
        final int playbackPosition = 0;
        final PlaySessionSource playSessionSource = PlaySessionSource.forSystemPlaylist(Screen.SYSTEM_PLAYLIST.get(),
                                                                                        TRACKING_FEATURE_NAME,
                                                                                        playbackPosition,
                                                                                        QUERY_URN,
                                                                                        URN,
                                                                                        3);

        when(systemPlaylistAdapter.getItems()).thenReturn(ADAPTER_ITEMS);
        when(systemPlaylistAdapter.getItem(adapterPosition)).thenReturn(FIRST);
        when(systemPlaylistAdapter.getItemCount()).thenReturn(ADAPTER_ITEMS.size());

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.playClicked();

        verify(playbackInitiator).playTracks(newArrayList(FIRST_TRACK_ITEM.getUrn(), SECOND_TRACK_ITEM.getUrn(), THIRD_TRACK_ITEM.getUrn()), playbackPosition, playSessionSource);
    }

    @Test
    public void canTransformNewForYouToNewForYouItemsWithHeaderAndTracksWithIsPlaying() throws Exception {
        when(playSessionStateProvider.isCurrentlyPlaying(TRACKS.get(0).urn())).thenReturn(false);
        when(playSessionStateProvider.isCurrentlyPlaying(TRACKS.get(1).urn())).thenReturn(true);
        when(playSessionStateProvider.isCurrentlyPlaying(TRACKS.get(2).urn())).thenReturn(false);

        final Iterator<SystemPlaylistItem> systemPlaylistItem = presenter.toSystemPlaylistItems().apply(SYSTEM_PLAYLIST).iterator();

        assertThat(systemPlaylistItem.next().isHeader()).isTrue();
        assertThat(((SystemPlaylistItem.Track) systemPlaylistItem.next()).track().isPlaying()).isFalse();
        assertThat(((SystemPlaylistItem.Track) systemPlaylistItem.next()).track().isPlaying()).isTrue();
        assertThat(((SystemPlaylistItem.Track) systemPlaylistItem.next()).track().isPlaying()).isFalse();

    }

    @Test
    public void hidesPlayButtonWithEmptyTracks() throws Exception {
        final Iterator<SystemPlaylistItem> systemPlaylistItem = presenter.toSystemPlaylistItems().apply(EMPTY_SYSTEM_PLAYLIST).iterator();

        final SystemPlaylistItem.Header header = ((SystemPlaylistItem.Header) systemPlaylistItem.next());

        assertThat(header.isHeader()).isTrue();
        assertThat(header.shouldShowPlayButton()).isFalse();
    }

    @Test
    public void showsPlayButtonWithNonEmptyTracks() throws Exception {
        final Iterator<SystemPlaylistItem> systemPlaylistItem = presenter.toSystemPlaylistItems().apply(SYSTEM_PLAYLIST).iterator();

        final SystemPlaylistItem.Header header = ((SystemPlaylistItem.Header) systemPlaylistItem.next());

        assertThat(header.isHeader()).isTrue();
        assertThat(header.shouldShowPlayButton()).isTrue();
    }
}
