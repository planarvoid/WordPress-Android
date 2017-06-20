package com.soundcloud.android.search.topresults;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.search.SearchPlayQueueFilter;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.app.Activity;

import java.util.ArrayList;

@RunWith(MockitoJUnitRunner.class)
public class SearchClickListenerTest {
    private static final String QUERY = "query";
    private static final Optional<Urn> QUERY_URN = Optional.of(new Urn("soundcloud:search:123"));
    private static final int POSITION = 1;
    private static final String CONTEXT = "search:tracks";
    private static final Screen SCREEN = Screen.SEARCH_EVERYTHING;
    @Mock private NavigationExecutor navigationExecutor;
    @Mock private EventTracker eventTracker;
    @Mock private SearchPlayQueueFilter searchPlayQueueFilter;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private EventBus eventBus;
    @Mock private Navigator navigator;
    private SearchClickListener searchClickListener;

    @Before
    public void setUp() throws Exception {
        searchClickListener = new SearchClickListener(eventTracker, searchPlayQueueFilter, playbackInitiator, eventBus, navigator);
    }

    @Test
    public void navigatesToPlaylist() {
        final Urn playlistUrn = Urn.forPlaylist(12);
        final SearchClickListener.ClickParams clickParams = SearchClickListener.ClickParams.create(playlistUrn,
                                                                                                   QUERY,
                                                                                                   QUERY_URN,
                                                                                                   POSITION,
                                                                                                   Module.create(CONTEXT, POSITION),
                                                                                                   SCREEN,
                                                                                                   SearchEvent.ClickSource.PLAYLISTS_BUCKET);
        final ClickResultAction clickResultAction = searchClickListener.playlistClickToNavigateAction(clickParams);

        final Activity context = mock(Activity.class);
        clickResultAction.run(context);

        verify(navigator).navigateTo(NavigationTarget.forPlaylist(context, playlistUrn, SCREEN, Optional.of(clickParams.searchQuerySourceInfo()), Optional.absent(),Optional.of(clickParams.uiEvent())));
    }

    @Test
    public void navigatesToProfile() {
        final Urn userUrn = Urn.forUser(12);
        final SearchClickListener.ClickParams clickParams = SearchClickListener.ClickParams.create(userUrn,
                                                                                                   QUERY,
                                                                                                   QUERY_URN,
                                                                                                   POSITION,
                                                                                                   Module.create(CONTEXT, POSITION),
                                                                                                   SCREEN,
                                                                                                   SearchEvent.ClickSource.PEOPLE_BUCKET);
        final ClickResultAction clickResultAction = searchClickListener.userClickToNavigateAction(clickParams);

        final Activity activity = mock(Activity.class);
        clickResultAction.run(activity);

        verify(navigator).navigateTo(NavigationTarget.forProfile(activity, userUrn, Optional.of(clickParams.uiEvent()), Optional.of(SCREEN), Optional.absent()));
    }

    @Test
    public void startsPlaybackAndTracksSuccess() {
        final Urn trackUrn1 = Urn.forTrack(1);
        final Urn trackUrn2 = Urn.forTrack(2);
        final Urn trackUrn3 = Urn.forTrack(3);
        final ArrayList<Urn> playQueue = Lists.newArrayList(trackUrn1, trackUrn2, trackUrn3);
        final SearchClickListener.TrackClickParams trackClickParams = SearchClickListener.TrackClickParams.create(trackUrn2,
                                                                                                                  QUERY,
                                                                                                                  QUERY_URN,
                                                                                                                  POSITION,
                                                                                                                  Module.create(CONTEXT, POSITION),
                                                                                                                  SCREEN,
                                                                                                                  SearchEvent.ClickSource.TRACKS_BUCKET,
                                                                                                                  playQueue);
        when(searchPlayQueueFilter.correctQueue(playQueue, POSITION)).thenReturn(playQueue);
        when(searchPlayQueueFilter.correctPosition(POSITION)).thenReturn(POSITION);
        when(playbackInitiator.playPosts(playQueue, trackUrn2, POSITION, trackClickParams.playSessionSource())).thenReturn(Single.just(PlaybackResult.success()));

        final TestObserver<PlaybackResult> observer = searchClickListener.trackClickToPlaybackResult(trackClickParams).test();

        observer.assertValue(PlaybackResult.success());
        verify(eventTracker).trackSearch(eq(SearchEvent.tapItemOnScreen(SCREEN, trackClickParams.clickParams().searchQuerySourceInfo(), trackClickParams.clickParams().clickSource())));
        verify(eventBus).publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
    }

    @Test
    public void doesntTrackPlaybackFailure() {
        final Urn trackUrn1 = Urn.forTrack(1);
        final Urn trackUrn2 = Urn.forTrack(2);
        final Urn trackUrn3 = Urn.forTrack(3);
        final ArrayList<Urn> playQueue = Lists.newArrayList(trackUrn1, trackUrn2, trackUrn3);
        final SearchClickListener.TrackClickParams trackClickParams = SearchClickListener.TrackClickParams.create(trackUrn2,
                                                                                                                  QUERY,
                                                                                                                  QUERY_URN,
                                                                                                                  POSITION,
                                                                                                                  Module.create(CONTEXT, POSITION),
                                                                                                                  SCREEN,
                                                                                                                  SearchEvent.ClickSource.TRACKS_BUCKET,
                                                                                                                  playQueue);
        when(searchPlayQueueFilter.correctQueue(playQueue, POSITION)).thenReturn(playQueue);
        when(searchPlayQueueFilter.correctPosition(POSITION)).thenReturn(POSITION);
        final PlaybackResult error = PlaybackResult.error(PlaybackResult.ErrorReason.UNSKIPPABLE);

        when(playbackInitiator.playPosts(playQueue, trackUrn2, POSITION, trackClickParams.playSessionSource())).thenReturn(Single.just(error));

        final TestObserver<PlaybackResult> observer = searchClickListener.trackClickToPlaybackResult(trackClickParams).test();

        observer.assertValue(error);
        verify(eventTracker).trackSearch(eq(SearchEvent.tapItemOnScreen(SCREEN, trackClickParams.clickParams().searchQuerySourceInfo(), trackClickParams.clickParams().clickSource())));
        verify(eventBus, never()).publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
    }
}
