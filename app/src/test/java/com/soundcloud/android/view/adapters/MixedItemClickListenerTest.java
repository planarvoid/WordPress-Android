package com.soundcloud.android.view.adapters;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PromotedPlaylistItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.AdapterView;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MixedItemClickListenerTest extends AndroidUnitTest {

    private MixedItemClickListener listener;

    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private MixedPlayableRecyclerItemAdapter adapter;
    @Mock private AdapterView adapterView;
    @Mock private View view;
    @Mock private ExpandPlayerSubscriber expandPlayerSubscriber;
    @Mock private FeatureOperations featureOperations;
    @Mock private Navigator navigator;
    @Mock private Context context;

    private final Screen screen = Screen.ACTIVITIES;
    private final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(Urn.forTrack(123));

    @Before
    public void setUp() {
        listener = new MixedItemClickListener(playbackInitiator, InjectionSupport.providerOf(expandPlayerSubscriber),
                featureOperations, navigator,
                screen, searchQuerySourceInfo);

        when(view.getContext()).thenReturn(context);
    }

    @Test
    public void itemClickOnTrackStartsPlaybackWithJustTracks() {
        final TrackItem track1 = ModelFixtures.create(TrackItem.class);
        final TrackItem track2 = ModelFixtures.create(TrackItem.class);
        List<ListItem> items = Arrays.asList(
                ModelFixtures.create(PlaylistItem.class),
                track1,
                ModelFixtures.create(PlaylistItem.class),
                track2,
                ModelFixtures.create(UserItem.class)
        );

        final List<Urn> trackList = Arrays.asList(track1.getEntityUrn(), track2.getEntityUrn());
        final PlaybackResult playbackResult = PlaybackResult.success();
        when(playbackInitiator.playTracks(trackList, 1, new PlaySessionSource(screen))).thenReturn(Observable.just(playbackResult));

        listener.onItemClick(items, view, 3);

        verify(expandPlayerSubscriber).onNext(playbackResult);
        verify(expandPlayerSubscriber).onCompleted();
    }

    @Test
    public void itemClickOnPromotedTrackPlaysWithPromotedSourceInfo() {
        final PromotedTrackItem promotedTrack = PromotedTrackItem.from(TestPropertySets.expectedPromotedTrack());
        final PromotedSourceInfo promotedSourceInfo = PromotedSourceInfo.fromItem(promotedTrack);
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        playSessionSource.setPromotedSourceInfo(promotedSourceInfo);

        final Observable<List<PropertySet>> trackList = Observable.empty();
        final PlaybackResult playbackResult = PlaybackResult.success();

        when(playbackInitiator.playPosts(eq(trackList), eq(promotedTrack.getEntityUrn()), eq(0), not(eq(playSessionSource))))
                .thenThrow(new IllegalArgumentException());
        when(playbackInitiator.playPosts(trackList, promotedTrack.getEntityUrn(), 0, playSessionSource))
                .thenReturn(Observable.just(playbackResult));

        listener.onPostClick(trackList, view, 0, promotedTrack);

        verify(expandPlayerSubscriber).onNext(playbackResult);
        verify(expandPlayerSubscriber).onCompleted();
    }

    @Test
    public void itemClickOnMidTierTrackWhenUserCanUpgradeShowsUpgrade() {
        final TrackItem track1 = new TrackItem(TestPropertySets.midTierTrack());
        final TrackItem track2 = ModelFixtures.create(TrackItem.class);
        List<ListItem> items = Arrays.asList(
                ModelFixtures.create(PlaylistItem.class),
                track1,
                ModelFixtures.create(PlaylistItem.class),
                track2,
                ModelFixtures.create(UserItem.class)
        );

        when(featureOperations.upsellMidTier()).thenReturn(true);

        listener.onItemClick(items, view, 3);

        verify(navigator).openUpgrade(context);
    }

    @Test
    public void itemClickOnMidTierTrackDoesNotUpsellIfUserCannotUpgrade() {
        final TrackItem track1 = new TrackItem(TestPropertySets.midTierTrack());
        final TrackItem track2 = ModelFixtures.create(TrackItem.class);
        List<ListItem> items = Arrays.asList(
                ModelFixtures.create(PlaylistItem.class),
                track1,
                ModelFixtures.create(PlaylistItem.class),
                track2,
                ModelFixtures.create(UserItem.class)
        );

        final List<Urn> trackList = Arrays.asList(track1.getEntityUrn(), track2.getEntityUrn());
        final PlaybackResult playbackResult = PlaybackResult.success();
        when(playbackInitiator.playTracks(trackList, 1, new PlaySessionSource(screen))).thenReturn(Observable.just(playbackResult));

        listener.onItemClick(items, view, 3);

        verify(navigator, never()).openUpgrade(any(Context.class));
    }

    @Test
    public void itemClickOnPlaylistSendsPlaylistDetailIntent() {
        final PlaylistItem playlistItem = ModelFixtures.create(PlaylistItem.class);
        List<ListItem> items = Arrays.asList(
                ModelFixtures.create(PlaylistItem.class),
                ModelFixtures.create(TrackItem.class),
                playlistItem,
                ModelFixtures.create(TrackItem.class),
                ModelFixtures.create(UserItem.class)
        );

        listener.onItemClick(items, view, 2);

        verify(navigator).openPlaylist(context, playlistItem.getEntityUrn(), screen, searchQuerySourceInfo, null);
    }

    @Test
    public void itemClickOnPromotedPlaylistSendsPlaylistDetailIntent() {
        Observable<List<Urn>> playables = Observable.from(Collections.<List<Urn>>emptyList());
        PromotedPlaylistItem playlistItem = PromotedPlaylistItem.from(TestPropertySets.expectedPromotedPlaylist());
        PromotedSourceInfo info = PromotedSourceInfo.fromItem(playlistItem);

        listener.onItemClick(playables, view, 0, playlistItem);

        verify(navigator).openPlaylist(context, playlistItem.getEntityUrn(), screen, searchQuerySourceInfo, info);
    }

    @Test
    public void itemClickOnUserGoesToUserProfile() {
        final UserItem userItem = ModelFixtures.create(UserItem.class);
        List<ListItem> items = Arrays.asList(
                ModelFixtures.create(PlaylistItem.class),
                ModelFixtures.create(TrackItem.class),
                userItem,
                ModelFixtures.create(PlaylistItem.class),
                ModelFixtures.create(TrackItem.class)
        );

        listener.onItemClick(items, view, 2);

        verify(navigator).openProfile(context, userItem.getEntityUrn(), screen, searchQuerySourceInfo);
    }

    @Test
    public void itemClickOnLocalTrackStartsPlaybackThroughPlaybackOperations() {
        final TrackItem track1 = ModelFixtures.create(TrackItem.class);
        final Observable<List<Urn>> tracklist = Observable.empty();
        final PlaybackResult playbackResult = PlaybackResult.success();
        when(playbackInitiator.playTracks(tracklist, track1.getEntityUrn(), 1, new PlaySessionSource(screen))).thenReturn(Observable.just(playbackResult));

        listener.onItemClick(tracklist, view, 1, track1);

        verify(expandPlayerSubscriber).onNext(playbackResult);
        verify(expandPlayerSubscriber).onCompleted();
    }

    @Test
    public void itemClickOnLocalMidTierTrackWhenUserCanUpgradeShowsUpgrade() {
        when(featureOperations.upsellMidTier()).thenReturn(true);

        listener.onItemClick(Observable.<List<Urn>>empty(), view, 1, new TrackItem(TestPropertySets.midTierTrack()));

        verify(navigator).openUpgrade(context);
    }

    @Test
    public void itemClickOnLocalMidTierTrackDoesNotShowUpgradeIfUserCannotUpgrade() {
        final TrackItem midTierTrack = new TrackItem(TestPropertySets.midTierTrack());
        final Observable<List<Urn>> tracklist = Observable.empty();
        final PlaybackResult playbackResult = PlaybackResult.success();
        when(playbackInitiator.playTracks(tracklist, midTierTrack.getEntityUrn(), 1, new PlaySessionSource(screen))).thenReturn(Observable.just(playbackResult));

        listener.onItemClick(Observable.<List<Urn>>empty(), view, 1, midTierTrack);

        verify(navigator, never()).openUpgrade(any(Context.class));
    }

    @Test
    public void itemClickOnLocalPlaylistSendsPlaylistDetailIntent() {
        final PlaylistItem playlistItem = ModelFixtures.create(PlaylistItem.class);
        List<Urn> items = Arrays.asList(Urn.forTrack(123L), playlistItem.getEntityUrn());

        listener.onItemClick(Observable.just(items), view, 1, playlistItem);

        verify(navigator).openPlaylist(context, playlistItem.getEntityUrn(), screen, searchQuerySourceInfo, null);
    }

    @Test
    public void itemClickOnLocalUserGoesToUserProfile() {
        final UserItem userItem = ModelFixtures.create(UserItem.class);
        List<Urn> items = Arrays.asList(Urn.forTrack(123L), Urn.forPlaylist(123L), userItem.getEntityUrn());

        listener.onItemClick(Observable.just(items), view, 2, userItem);

        verify(navigator).openProfile(context, userItem.getEntityUrn(), screen, searchQuerySourceInfo);
    }

    @Test
    public void postItemClickOnLocalTrackStartsPlaybackThroughPlaybackOperations() {
        final TrackItem track1 = ModelFixtures.create(TrackItem.class);
        final Observable<List<PropertySet>> tracklist = Observable.empty();
        final PlaybackResult playbackResult = PlaybackResult.success();
        when(playbackInitiator.playPosts(tracklist, track1.getEntityUrn(), 1, new PlaySessionSource(screen))).thenReturn(Observable.just(playbackResult));

        listener.onPostClick(tracklist, view, 1, track1);

        verify(expandPlayerSubscriber).onNext(playbackResult);
        verify(expandPlayerSubscriber).onCompleted();
    }

    @Test
    public void postItemClickOnLocalMidTierTrackWhenUserCanUpgradeShowsUpgrade() {
        when(featureOperations.upsellMidTier()).thenReturn(true);

        listener.onPostClick(Observable.<List<PropertySet>>empty(), view, 1, new TrackItem(TestPropertySets.midTierTrack()));

        verify(navigator).openUpgrade(context);
    }

    @Test
    public void postItemClickOnLocalMidTierTrackDoesNotShowUpgradeIfUserCannotUpgrade() {
        final TrackItem midTierTrack = new TrackItem(TestPropertySets.midTierTrack());
        final Observable<List<PropertySet>> tracklist = Observable.empty();
        final PlaybackResult playbackResult = PlaybackResult.success();
        when(playbackInitiator.playPosts(tracklist, midTierTrack.getEntityUrn(), 1, new PlaySessionSource(screen))).thenReturn(Observable.just(playbackResult));

        listener.onPostClick(Observable.<List<PropertySet>>empty(), view, 1, midTierTrack);

        verify(navigator, never()).openUpgrade(any(Context.class));
    }

    @Test
    public void postItemClickOnLocalPlaylistSendsPlaylistDetailIntent() {
        final PlaylistItem playlistItem = ModelFixtures.create(PlaylistItem.class);
        List<PropertySet> items = Arrays.asList(createTrackPropertySet(Urn.forTrack(123L)), createTrackPropertySet(playlistItem.getEntityUrn()));

        listener.onPostClick(Observable.just(items), view, 1, playlistItem);

        verify(navigator).openPlaylist(context, playlistItem.getEntityUrn(), screen, searchQuerySourceInfo, null);
    }

    @Test
    public void postItemClickOnLocalUserGoesToUserProfile() {
        final UserItem userItem = ModelFixtures.create(UserItem.class);
        List<PropertySet> items = Arrays.asList(createTrackPropertySet(Urn.forTrack(123L)),
                createTrackPropertySet(Urn.forPlaylist(123L)),
                createTrackPropertySet(userItem.getEntityUrn()));

        listener.onPostClick(Observable.just(items), view, 2, userItem);

        verify(navigator).openProfile(context, userItem.getEntityUrn(), screen, searchQuerySourceInfo);
    }

    @NonNull
    private PropertySet createTrackPropertySet(Urn urn) {
        return PropertySet.from(TrackProperty.URN.bind(urn));
    }
}
