package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.helpers.NavigationTargetMatcher.matchesNavigationTarget;
import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.events.AttributingActivity;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.LinkType;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlayableWithReposter;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.search.suggestions.SearchSuggestionItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.rx.eventbus.TestEventBus;
import io.reactivex.Single;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import rx.Observable;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MixedItemClickListenerTest extends AndroidUnitTest {

    private final Screen screen = Screen.ACTIVITIES;
    private final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(Urn.forTrack(123), "query");
    private EventBus eventBus = new TestEventBus();
    private MixedItemClickListener listener;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private MixedPlayableRecyclerItemAdapter adapter;
    @Mock private AdapterView adapterView;
    @Mock private View view;
    @Mock private PlaybackFeedbackHelper playbackFeedbackHelper;
    @Mock private Navigator navigator;
    @Captor private ArgumentCaptor<UIEvent> uiEventArgumentCaptor;
    @Captor private ArgumentCaptor<NavigationTarget> navigationTargetArgumentCaptor;
    @Spy private ExpandPlayerSubscriber expandPlayerSubscriber = new ExpandPlayerSubscriber(eventBus, playbackFeedbackHelper, mock(PerformanceMetricsEngine.class));
    private AppCompatActivity activity;

    @Before
    public void setUp() {
        listener = new MixedItemClickListener(playbackInitiator,
                                              providerOf(expandPlayerSubscriber),
                                              screen,
                                              searchQuerySourceInfo,
                                              navigator);
        activity = activity();
        when(view.getContext()).thenReturn(activity);
    }

    @Test
    public void itemClickOnTrackStartsPlaybackWithJustTracks() {
        final TrackItem track1 = ModelFixtures.trackItem();
        final TrackItem track2 = ModelFixtures.trackItem();
        List<ListItem> items = Arrays.asList(
                ModelFixtures.playlistItem(),
                track1,
                ModelFixtures.playlistItem(),
                track2,
                ModelFixtures.userItem()
        );

        final List<Urn> trackList = Arrays.asList(track1.getUrn(), track2.getUrn());
        final PlaybackResult playbackResult = PlaybackResult.success();
        when(playbackInitiator.playTracks(trackList, 1, new PlaySessionSource(screen))).thenReturn(Single.just(playbackResult));

        listener.onItemClick(items, view.getContext(), 3);

        verify(expandPlayerSubscriber).onNext(playbackResult);
        verify(expandPlayerSubscriber).onCompleted();
    }

    @Test
    public void itemClickOnPromotedTrackPlaysWithPromotedSourceInfo() {
        final TrackItem promotedTrack = PlayableFixtures.expectedPromotedTrack();
        final PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(promotedTrack);
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        playSessionSource.setPromotedSourceInfo(promotedSourceInfo);

        final Observable<List<PlayableWithReposter>> trackList = Observable.just(Collections.emptyList());
        final PlaybackResult playbackResult = PlaybackResult.success();

        when(playbackInitiator.playPosts(eq(RxJava.toV2Single(trackList)),
                                         eq(promotedTrack.getUrn()),
                                         eq(0),
                                         not(eq(playSessionSource))))
                .thenThrow(new IllegalArgumentException());
        when(playbackInitiator.playPosts(any(Single.class), eq(promotedTrack.getUrn()), eq(0), eq(playSessionSource)))
                .thenReturn(Single.just(playbackResult));

        listener.legacyOnPostClick(trackList, view, 0, promotedTrack);

        verify(expandPlayerSubscriber).onNext(playbackResult);
        verify(expandPlayerSubscriber).onCompleted();
    }

    @Test
    public void itemClickOnPlaylistSendsPlaylistDetailIntent() {
        final PlaylistItem playlistItem = ModelFixtures.playlistItem();
        List<ListItem> items = Arrays.asList(
                ModelFixtures.playlistItem(),
                ModelFixtures.trackItem(),
                playlistItem,
                ModelFixtures.trackItem(),
                ModelFixtures.userItem()
        );

        listener.onItemClick(items, view.getContext(), 2);

        verify(navigator).navigateTo(argThat(matchesNavigationTarget(NavigationTarget.forPlaylist(playlistItem.getUrn(),
                                                                                                                screen,
                                                                                                                Optional.fromNullable(searchQuerySourceInfo),
                                                                                                                Optional.absent(),
                                                                                                                Optional.of(UIEvent.fromNavigation(
                                                                                                                        playlistItem.getUrn(),
                                                                                                                        EventContextMetadata.builder()
                                                                                                                                            .pageName(screen.get())
                                                                                                                                            .attributingActivity(AttributingActivity.fromPlayableItem(
                                                                                                                                                    playlistItem))
                                                                                                                                            .linkType(LinkType.SELF)
                                                                                                                                            .build()))))));
    }

    @Test
    public void itemClickOnPromotedPlaylistSendsPlaylistDetailIntent() {
        Observable<List<Urn>> playables = Observable.from(Collections.<List<Urn>>emptyList());
        PlaylistItem playlistItem = PlayableFixtures.expectedPromotedPlaylist();

        listener.onItemClick(playables, 0, playlistItem);

        verify(navigator).navigateTo(argThat(matchesNavigationTarget(NavigationTarget.forPlaylist(playlistItem.getUrn(),
                                                                                                                screen,
                                                                                                                Optional.fromNullable(searchQuerySourceInfo),
                                                                                                                Optional.of(PromotedSourceInfo.fromItem(playlistItem)),
                                                                                                                Optional.of(UIEvent.fromNavigation(
                                                                                                                        playlistItem.getUrn(),
                                                                                                                        EventContextMetadata.builder()
                                                                                                                                            .pageName(screen.get())
                                                                                                                                            .attributingActivity(AttributingActivity.fromPlayableItem(
                                                                                                                                                    playlistItem))
                                                                                                                                            .linkType(LinkType.SELF)
                                                                                                                                            .build()))))));
    }

    @Test
    public void itemClickOnSearchSuggestionPlaylistSendsLegacyPlaylistDetailIntent() {
        Observable<List<Urn>> playables = Observable.from(Collections.<List<Urn>>emptyList());

        final SearchSuggestionItem suggestionItem = SearchSuggestionItem.forPlaylist(Urn.forPlaylist(0), Optional.absent(), "", Optional.absent(), "");

        listener.onItemClick(playables, 0, suggestionItem);

        verify(navigator).navigateTo(argThat(matchesNavigationTarget(NavigationTarget.forLegacyPlaylist(suggestionItem.getUrn(),
                                                                                                                      screen,
                                                                                                                      Optional.of(searchQuerySourceInfo),
                                                                                                                      Optional.absent()))));
    }

    @Test
    public void itemClickOnUserGoesToUserProfile() {
        final UserItem userItem = ModelFixtures.userItem();
        List<ListItem> items = Arrays.asList(
                ModelFixtures.playlistItem(),
                ModelFixtures.trackItem(),
                userItem,
                ModelFixtures.playlistItem(),
                ModelFixtures.trackItem()
        );

        listener.onItemClick(items, activity, 2);

        verify(navigator).navigateTo(argThat(matchesNavigationTarget(NavigationTarget.forProfile(userItem.getUrn(),
                                                                                                               Optional.absent(),
                                                                                                               Optional.of(screen),
                                                                                                               Optional.of(searchQuerySourceInfo)))));
    }

    @Test
    public void itemClickOnLocalTrackStartsPlaybackThroughPlaybackOperations() {
        final TrackItem track1 = ModelFixtures.trackItem();
        final Observable<List<Urn>> tracklist = Observable.just(Collections.emptyList());
        final PlaybackResult playbackResult = PlaybackResult.success();
        when(playbackInitiator.playTracks(any(Single.class),
                                          eq(track1.getUrn()),
                                          eq(1),
                                          eq(new PlaySessionSource(screen)))).thenReturn(Single.just(playbackResult));

        listener.onItemClick(tracklist, 1, track1);

        verify(expandPlayerSubscriber).onNext(playbackResult);
        verify(expandPlayerSubscriber).onCompleted();
    }

    @Test
    public void itemClickOnLocalPlaylistSendsPlaylistDetailIntent() {
        final PlaylistItem playlistItem = ModelFixtures.playlistItem();
        List<Urn> items = Arrays.asList(Urn.forTrack(123L), playlistItem.getUrn());

        listener.onItemClick(Observable.just(items), 1, playlistItem);

        verify(navigator).navigateTo(
                                argThat(matchesNavigationTarget(NavigationTarget.forPlaylist(items.get(1),
                                                                             screen,
                                                                             Optional.fromNullable(searchQuerySourceInfo),
                                                                             Optional.absent(),
                                                                             Optional.of(UIEvent.fromNavigation(
                                                                                     items.get(1),
                                                                                     EventContextMetadata.builder()
                                                                                                         .pageName(screen.get())
                                                                                                         .attributingActivity(AttributingActivity.fromPlayableItem(playlistItem))
                                                                                                         .linkType(LinkType.SELF)
                                                                                                         .build()))))));
    }

    @Test
    public void itemClickOnLocalUserGoesToUserProfile() {
        final UserItem userItem = ModelFixtures.userItem();
        List<Urn> items = Arrays.asList(Urn.forTrack(123L),
                                        Urn.forPlaylist(123L),
                                        userItem.getUrn());

        listener.onItemClick(Observable.just(items), 2, userItem);

        verify(navigator).navigateTo(argThat(matchesNavigationTarget(NavigationTarget.forProfile(userItem.getUrn(),
                                                                                                               Optional.absent(),
                                                                                                               Optional.of(screen),
                                                                                                               Optional.of(searchQuerySourceInfo)))));
    }

    @Test
    public void postItemClickOnLocalTrackStartsPlaybackThroughPlaybackOperations() {
        final TrackItem track1 = ModelFixtures.trackItem();
        final Observable<List<PlayableWithReposter>> tracklist = Observable.just(Collections.emptyList());
        final PlaybackResult playbackResult = PlaybackResult.success();
        when(playbackInitiator.playPosts(any(Single.class),
                                         eq(track1.getUrn()),
                                         eq(1),
                                         eq(new PlaySessionSource(screen)))).thenReturn(Single.just(playbackResult));

        listener.legacyOnPostClick(tracklist, view, 1, track1);

        verify(expandPlayerSubscriber).onNext(playbackResult);
        verify(expandPlayerSubscriber).onCompleted();
    }

    @Test
    public void postItemClickOnLocalPlaylistSendsPlaylistDetailIntent() {
        final PlaylistItem playlistItem = ModelFixtures.playlistItem();
        List<PlayableWithReposter> items = Arrays.asList(createPlayable(Urn.forTrack(123L)),
                                                         createPlayable(playlistItem.getUrn()));

        listener.legacyOnPostClick(Observable.just(items), view, 1, playlistItem);


        verify(navigator).navigateTo(argThat(matchesNavigationTarget(NavigationTarget.forPlaylist(playlistItem.getUrn(),
                                                                                                                screen,
                                                                                                                Optional.fromNullable(searchQuerySourceInfo),
                                                                                                                Optional.absent(),
                                                                                                                Optional.of(UIEvent.fromNavigation(
                                                                                                                        playlistItem.getUrn(),
                                                                                                                        EventContextMetadata.builder()
                                                                                                                                            .pageName(screen.get())
                                                                                                                                            .attributingActivity(AttributingActivity.fromPlayableItem(
                                                                                                                                                    playlistItem))
                                                                                                                                            .linkType(LinkType.SELF)
                                                                                                                                            .build()))))));
    }

    @Test
    public void onPostClickOpensPlaylistOnNonTrackItem() {
        final PlaylistItem playlistItem = ModelFixtures.playlistItem();
        List<PlayableItem> items = Arrays.asList(createPlayableItem(Urn.forTrack(123L)),
                                                 createPlayableItem(playlistItem.getUrn()));

        final int modulePosition = 5;
        final Module module = Module.create(Module.USER_ALBUMS, modulePosition);
        listener.onPostClick(Observable.just(items), view, 1, playlistItem, module);

        verify(navigator).navigateTo(argThat(matchesNavigationTarget(NavigationTarget.forPlaylist(playlistItem.getUrn(),
                                                                                                                screen,
                                                                                                                Optional.fromNullable(searchQuerySourceInfo),
                                                                                                                Optional.absent(),
                                                                                                                Optional.of(UIEvent.fromNavigation(
                                                                                                                        playlistItem.getUrn(),
                                                                                                                        EventContextMetadata.builder()
                                                                                                                                            .pageName(screen.get())
                                                                                                                                            .attributingActivity(AttributingActivity.fromPlayableItem(
                                                                                                                                                    playlistItem))
                                                                                                                                            .linkType(LinkType.SELF)
                                                                                                                                            .module(module)
                                                                                                                                            .build()))))));
    }

    @Test
    public void postItemClickOnLocalUserGoesToUserProfile() {
        final UserItem userItem = ModelFixtures.userItem();
        List<PlayableWithReposter> items = Arrays.asList(createPlayable(Urn.forTrack(123L)),
                                                         createPlayable(Urn.forPlaylist(123L)),
                                                         createPlayable(userItem.getUrn()));

        listener.legacyOnPostClick(Observable.just(items), view, 2, userItem);

        verify(navigator).navigateTo(argThat(matchesNavigationTarget(NavigationTarget.forProfile(userItem.getUrn(),
                                                                                                               Optional.absent(),
                                                                                                               Optional.of(screen),
                                                                                                               Optional.of(searchQuerySourceInfo)))));
    }

    @NonNull
    private PlayableWithReposter createPlayable(Urn urn) {
        return PlayableWithReposter.from(urn);
    }

    @NonNull
    private PlayableItem createPlayableItem(Urn urn) {
        return ModelFixtures.playlistItem(urn);
    }
}
