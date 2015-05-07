package com.soundcloud.android.deeplinks;

import static com.pivotallabs.greatexpectations.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Actions;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.content.Intent;
import android.net.Uri;
import android.os.Parcelable;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class ResolveActivityTest {
    private ResolveActivity activity;
    private TestEventBus eventBus;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private AccountOperations accountOperations;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        activity = new ResolveActivity(playbackOperations, new ReferrerResolver(), eventBus, accountOperations);
        when(playbackOperations.startPlaybackWithRecommendations(any(PublicApiTrack.class), any(Screen.class))).thenReturn(Observable.<PlaybackResult>empty());
        when(accountOperations.isUserLoggedIn()).thenReturn(true);
    }

    @Test
    public void acceptsSoundCloudScheme() {
        expect(ResolveActivity.accept(Uri.parse("soundcloud:something:123"), Robolectric.application.getResources())).toBeTrue();
    }

    @Test
    public void doesNotAcceptOtherScheme() {
        expect(ResolveActivity.accept(Uri.parse("dubstep:something:123"), Robolectric.application.getResources())).toBeFalse();
    }

    @Test
    public void acceptsSoundCloudHost() {
        expect(ResolveActivity.accept(Uri.parse("http://www.soundcloud.com"), Robolectric.application.getResources())).toBeTrue();
    }

    @Test
    public void doesNotAcceptOtherHost() {
        expect(ResolveActivity.accept(Uri.parse("http://www.asdf.com"), Robolectric.application.getResources())).toBeFalse();
    }

    @Test
    public void shouldPlayTrack() throws CreateModelException {
        PublicApiTrack track = ModelFixtures.create(PublicApiTrack.class);
        activity.setIntent(new Intent());
        activity.onSuccess(track);

        verify(playbackOperations).startPlaybackWithRecommendations(track, Screen.DEEPLINK);
    }

    @Test
    public void shouldGotoPlaylistDetails() throws CreateModelException {
        PublicApiPlaylist playlist = ModelFixtures.create(PublicApiPlaylist.class);
        activity.setIntent(new Intent());
        activity.onSuccess(playlist);

        Intent expected = new Intent(Actions.PLAYLIST);
        Screen.DEEPLINK.addToIntent(expected);
        expected.putExtra(PlaylistDetailActivity.EXTRA_URN, playlist.getUrn());
        expected.putExtra(PlaylistDetailActivity.EXTRA_AUTO_PLAY, false);
        expected.putExtra(PlaylistDetailActivity.EXTRA_QUERY_SOURCE_INFO, (Parcelable) null);

        expect(shadowOf(activity).getNextStartedActivity()).toEqual(expected);
    }

    @Test
    public void shouldShowTheStreamWithAnExpandedPlayer() throws CreateModelException {
        PublicApiTrack track = ModelFixtures.create(PublicApiTrack.class);
        activity.setIntent(new Intent());
        activity.onSuccess(track);

        Intent expected = new Intent(Actions.STREAM);
        expected.putExtra(SlidingPlayerController.EXTRA_EXPAND_PLAYER, true);
        expect(shadowOf(activity).getNextStartedActivity()).toEqual(expected);
    }

    @Test
    public void shouldTrackForegroundEventsWithResources() throws Exception {
        PublicApiTrack track = ModelFixtures.create(PublicApiTrack.class);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("soundcloud://sounds:1234?origin=twitter"));
        activity.setIntent(intent);
        activity.onSuccess(track);

        expect(eventBus.eventsOn(EventQueue.TRACKING)).toNumber(1);
        ForegroundEvent event = ((ForegroundEvent) eventBus.lastEventOn(EventQueue.TRACKING));
        expect(event.getKind()).toBe(ForegroundEvent.KIND_OPEN);
        expect(event.get(ForegroundEvent.KEY_REFERRER)).toEqual(Referrer.TWITTER.get());
        expect(event.get(ForegroundEvent.KEY_PAGE_NAME)).toEqual(Screen.DEEPLINK.get());
        expect(event.get(ForegroundEvent.KEY_PAGE_URN)).toEqual(track.getUrn().toString());
    }

    @Test
    public void shouldTrackForegroundEventsWhenUserIsNotLoggedIn() throws Exception {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("soundcloud://sounds:1234?origin=facebook"));
        activity.setIntent(intent);
        activity.onResume();

        expect(eventBus.eventsOn(EventQueue.TRACKING)).toNumber(1);
        ForegroundEvent event = ((ForegroundEvent) eventBus.lastEventOn(EventQueue.TRACKING));
        expect(event.getKind()).toBe(ForegroundEvent.KIND_OPEN);
        expect(event.get(ForegroundEvent.KEY_REFERRER)).toEqual(Referrer.FACEBOOK.get());
        expect(event.get(ForegroundEvent.KEY_PAGE_NAME)).toEqual(Screen.DEEPLINK.get());
        expect(event.get(ForegroundEvent.KEY_PAGE_URN)).toBeNull();
    }

    @Test
    public void shouldTrackForegroundEventsWithoutResources() throws Exception {
        when(accountOperations.isUserLoggedIn()).thenReturn(true);
        Intent intent = new Intent();

        activity.setIntent(intent);
        activity.onResume();

        expect(eventBus.eventsOn(EventQueue.TRACKING)).toNumber(1);
        ForegroundEvent event = ((ForegroundEvent) eventBus.lastEventOn(EventQueue.TRACKING));
        expect(event.getKind()).toBe(ForegroundEvent.KIND_OPEN);
        expect(event.get(ForegroundEvent.KEY_REFERRER)).toEqual(Referrer.OTHER.get());
        expect(event.get(ForegroundEvent.KEY_PAGE_NAME)).toEqual(Screen.DEEPLINK.get());
        expect(event.get(ForegroundEvent.KEY_PAGE_URN)).toBeNull();
    }
}
