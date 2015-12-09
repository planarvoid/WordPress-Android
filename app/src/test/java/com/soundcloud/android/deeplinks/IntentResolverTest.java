package com.soundcloud.android.deeplinks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.ServiceInitiator;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.EventBus;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import rx.Observable;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;

public class IntentResolverTest extends AndroidUnitTest {
    @Mock private ResolveOperations resolveOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private ServiceInitiator serviceInitiator;
    @Mock private ReferrerResolver referrerResolver;
    @Mock private EventBus eventBus;
    @Mock private Navigator navigator;

    @InjectMocks private IntentResolver resolver;

    private Uri uri;
    private Intent intent;
    private Context context;

    @Before
    public void setUp() throws Exception {
        context = context();
        setupReferrer(Referrer.OTHER);
        when(accountOperations.isUserLoggedIn()).thenReturn(true);
        when(playbackInitiator.startPlayback(any(Urn.class), any(Screen.class))).thenReturn(Observable.<PlaybackResult>empty());
    }

    @Test
    public void shouldLaunchSoundStreamIfUriBlank() {
        setupIntentForUrl("");

        resolver.handleIntent(intent, context);

        verify(navigator).openStream(context, Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchSoundStreamIfUriNull() {
        setupIntentForUrl(null);

        resolver.handleIntent(intent, context);

        verify(navigator).openStream(context, Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchPlayerDirectlyFromPreviouslyResolvedUrn() {
        setupIntentForUrl("soundcloud:tracks:123");

        resolver.handleIntent(intent, context);

        verify(playbackInitiator).startPlayback(Urn.forTrack(123), Screen.DEEPLINK);
        verify(resolveOperations, never()).resolve(any(Uri.class));
    }

    @Test
    public void shouldPlayTrackAfterResolvingDeepLink() throws CreateModelException {
        setupIntentForUrl("soundcloud://sounds:123");
        when(resolveOperations.resolve(uri)).thenReturn(Observable.just(Urn.forTrack(123)));

        resolver.handleIntent(intent, context);

        verify(playbackInitiator).startPlayback(Urn.forTrack(123), Screen.DEEPLINK);
    }

    @Test
    public void shouldShowTheStreamWithAnExpandedPlayer() throws CreateModelException {
        setupIntentForUrl("soundcloud://sounds:123");
        when(resolveOperations.resolve(uri)).thenReturn(Observable.just(Urn.forTrack(123)));

        resolver.handleIntent(intent, context);

        verify(navigator).openStreamWithExpandedPlayer(context, Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchPlaylistDetailsDirectlyFromPreviouslyResolvedUrn() {
        setupIntentForUrl("soundcloud:playlists:123");

        resolver.handleIntent(intent, context);

        verify(navigator).openPlaylist(context, Urn.forPlaylist(123), Screen.DEEPLINK);
        verify(resolveOperations, never()).resolve(any(Uri.class));
    }

    @Test
    public void shouldGotoPlaylistDetailsAfterResolvingDeepLink() throws CreateModelException {
        setupIntentForUrl("soundcloud://playlists:123");
        when(resolveOperations.resolve(uri)).thenReturn(Observable.just(Urn.forPlaylist(123)));

        resolver.handleIntent(intent, context);

        verify(navigator).openPlaylist(context, Urn.forPlaylist(123), Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchUserProfileDirectlyFromPreviouslyResolvedUrn() {
        setupIntentForUrl("soundcloud:users:123");

        resolver.handleIntent(intent, context);

        verify(navigator).openProfile(context, Urn.forUser(123), Screen.DEEPLINK);
        verify(resolveOperations, never()).resolve(any(Uri.class));
    }

    @Test
    public void shouldGotoUserProfileAfterResolvingDeepLink() throws CreateModelException {
        setupIntentForUrl("soundcloud://users:123");
        when(resolveOperations.resolve(uri)).thenReturn(Observable.just(Urn.forUser(123)));

        resolver.handleIntent(intent, context);

        verify(navigator).openProfile(context, Urn.forUser(123), Screen.DEEPLINK);
    }

    @Test
    public void shouldTrackForegroundEventsWithResources() throws Exception {
        setupIntentForUrl("soundcloud://sounds:123");
        when(resolveOperations.resolve(uri)).thenReturn(Observable.just(Urn.forTrack(123)));
        setupReferrer(Referrer.TWITTER);

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Urn.forTrack(123), Referrer.TWITTER);
    }

    @Test
    public void shouldTrackForegroundEventsWithResourceWhenUserIsNotLoggedIn() throws Exception {
        setupIntentForUrl("soundcloud://sounds:123");
        when(resolveOperations.resolve(uri)).thenReturn(Observable.just(Urn.forTrack(123)));
        setupReferrer(Referrer.FACEBOOK);
        when(accountOperations.isUserLoggedIn()).thenReturn(false);

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Urn.forTrack(123), Referrer.FACEBOOK);
    }

    @Test
    public void shouldTrackForegroundEventsWithoutResourcesForHome() throws Exception {
        intent = new Intent();

        resolver.handleIntent(intent, context);

        verify(navigator).openStream(context, Screen.DEEPLINK);
        verifyTrackingEvent(Referrer.OTHER);
    }

    @Test
    public void shouldLoginCrawler() throws Exception {
        setupIntentForUrl("soundcloud://playlists:123");
        when(resolveOperations.resolve(uri)).thenReturn(Observable.just(Urn.forPlaylist(123)));
        setupReferrer(Referrer.GOOGLE_CRAWLER);

        resolver.handleIntent(intent, context);

        verify(accountOperations).loginCrawlerUser();
        verify(navigator).openPlaylist(context, Urn.forPlaylist(123), Screen.DEEPLINK);
    }

    @Test
    public void shouldStartPlaybackForTracksOnCrawlersWithoutRelated() throws Exception {
        setupIntentForUrl("soundcloud://sounds:123");
        setupReferrer(Referrer.GOOGLE_CRAWLER);
        when(resolveOperations.resolve(uri)).thenReturn(Observable.just(Urn.forTrack(123)));

        resolver.handleIntent(intent, context);

        verify(playbackInitiator).startPlayback(Urn.forTrack(123), Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchOnboardingWithExtraUrnForLoggedOutUsers() throws Exception {
        setupIntentForUrl("soundcloud://sounds:123");
        when(resolveOperations.resolve(uri)).thenReturn(Observable.just(Urn.forTrack(123)));
        when(accountOperations.isUserLoggedIn()).thenReturn(false);

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Urn.forTrack(123), Referrer.OTHER);
        verify(navigator).openOnboarding(context, Urn.forTrack(123), Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchSearchForWebScheme() throws Exception {
        setupIntentForUrl("https://soundcloud.com/search?q=skrillex");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER);
        verify(navigator).openSearch(context, uri, Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchSearchForSoundCloudScheme() throws Exception {
        setupIntentForUrl("soundcloud://search?q=skrillex");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER);
        verify(navigator).openSearch(context, uri, Screen.DEEPLINK);
    }

    @Test
    public void shouldNotLaunchSearchForLoggedOutUsers() throws Exception {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        setupIntentForUrl("soundcloud://search?q=skrillex");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER);
        verify(navigator).openOnboarding(context, Urn.NOT_SET, Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchSearchForCrawlers() throws Exception {
        setupReferrer(Referrer.GOOGLE_CRAWLER);
        setupIntentForUrl("soundcloud://search?q=skrillex");

        resolver.handleIntent(intent, context);

        verify(accountOperations).loginCrawlerUser();
        verifyTrackingEvent(Referrer.GOOGLE_CRAWLER);
        verify(navigator).openSearch(context, uri, Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchExploreForWebScheme() throws Exception {
        setupIntentForUrl("https://soundcloud.com/explore");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER);
        verify(navigator).openExplore(context, Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchExploreForSoundCloudScheme() throws Exception {
        setupIntentForUrl("soundcloud://explore");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER);
        verify(navigator).openExplore(context, Screen.DEEPLINK);
    }

    @Test
    public void shouldNotLaunchExploreForLoggedOutUsers() throws Exception {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        setupIntentForUrl("soundcloud://explore");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER);
        verify(navigator).openOnboarding(context, Urn.NOT_SET, Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchExploreForCrawlers() throws Exception {
        setupReferrer(Referrer.GOOGLE_CRAWLER);
        setupIntentForUrl("soundcloud://explore");

        resolver.handleIntent(intent, context);

        verify(accountOperations).loginCrawlerUser();
        verifyTrackingEvent(Referrer.GOOGLE_CRAWLER);
        verify(navigator).openExplore(context, Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchRecordForWebScheme() throws Exception {
        setupIntentForUrl("https://soundcloud.com/upload");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER);
        verify(navigator).openRecord(context, Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchRecordForSoundCloudSchemeWithUpload() throws Exception {
        setupIntentForUrl("soundcloud://upload");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER);
        verify(navigator).openRecord(context, Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchRecordForSoundCloudSchemeWithRecord() throws Exception {
        setupIntentForUrl("soundcloud://record");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER);
        verify(navigator).openRecord(context, Screen.DEEPLINK);
    }

    @Test
    public void shouldNotLaunchRecordForLoggedOutUsers() throws Exception {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        setupIntentForUrl("soundcloud://record");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER);
        verify(navigator).openOnboarding(context, Urn.NOT_SET, Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchRecordForCrawlers() throws Exception {
        setupReferrer(Referrer.GOOGLE_CRAWLER);
        setupIntentForUrl("soundcloud://upload");

        resolver.handleIntent(intent, context);

        verify(accountOperations).loginCrawlerUser();
        verifyTrackingEvent(Referrer.GOOGLE_CRAWLER);
        verify(navigator).openRecord(context, Screen.DEEPLINK);
    }

    public void setupIntentForUrl(String url) {
        if (url != null) {
            uri = Uri.parse(url);
        }
        intent = new Intent().setData(uri);
    }

    private void setupReferrer(Referrer referrer) {
        when(referrerResolver.getReferrerFromIntent(any(Intent.class), any(Resources.class))).thenReturn(referrer);
    }

    private ForegroundEvent captureForegroundEvent() {
        ArgumentCaptor<ForegroundEvent> captor = ArgumentCaptor.forClass(ForegroundEvent.class);
        verify(eventBus).publish(eq(EventQueue.TRACKING), captor.capture());
        return captor.getValue();
    }

    private void verifyTrackingEvent(Urn urn, Referrer referrer) {
        ForegroundEvent event = captureForegroundEvent();

        assertThat(event.getKind()).isEqualTo(ForegroundEvent.KIND_OPEN);
        assertThat(event.get(ForegroundEvent.KEY_REFERRER)).isEqualTo(referrer.get());
        assertThat(event.get(ForegroundEvent.KEY_PAGE_NAME)).isEqualTo(Screen.DEEPLINK.get());
        assertThat(event.get(ForegroundEvent.KEY_PAGE_URN)).isEqualTo(urn.toString());
    }

    private void verifyTrackingEvent(Referrer referrer) {
        ForegroundEvent event = captureForegroundEvent();

        assertThat(event.getKind()).isEqualTo(ForegroundEvent.KIND_OPEN);
        assertThat(event.get(ForegroundEvent.KEY_REFERRER)).isEqualTo(referrer.get());
        assertThat(event.get(ForegroundEvent.KEY_PAGE_NAME)).isEqualTo(Screen.DEEPLINK.get());
    }

}
