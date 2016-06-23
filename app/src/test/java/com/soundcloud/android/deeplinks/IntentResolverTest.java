package com.soundcloud.android.deeplinks;

import static com.soundcloud.android.playback.PlaybackResult.ErrorReason.TRACK_UNAVAILABLE_OFFLINE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.PlaybackServiceInitiator;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.configuration.FeatureOperations;
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
    @Mock private PlaybackServiceInitiator serviceInitiator;
    @Mock private ReferrerResolver referrerResolver;
    @Mock private EventBus eventBus;
    @Mock private Navigator navigator;
    @Mock private FeatureOperations featureOperations;

    @InjectMocks private IntentResolver resolver;

    private Uri uri;
    private Intent intent;
    private Context context;

    @Before
    public void setUp() {
        context = context();
        setupReferrer(Referrer.OTHER);
        when(accountOperations.isUserLoggedIn()).thenReturn(true);
        when(playbackInitiator.startPlayback(any(Urn.class),
                                             any(Screen.class))).thenReturn(Observable.<PlaybackResult>empty());
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
        when(playbackInitiator.startPlayback(Urn.forTrack(123), Screen.DEEPLINK))
                .thenReturn(Observable.just(PlaybackResult.success()));

        resolver.handleIntent(intent, context);

        verify(navigator).openStreamWithExpandedPlayer(context, Screen.DEEPLINK);
    }

    @Test
    public void shouldNotOpenStreamWhenFailedToStartPlayback() throws CreateModelException {
        setupIntentForUrl("soundcloud://sounds:123");
        when(resolveOperations.resolve(uri)).thenReturn(Observable.just(Urn.forTrack(123)));
        when(playbackInitiator.startPlayback(Urn.forTrack(123), Screen.DEEPLINK))
                .thenReturn(Observable.just(PlaybackResult.error(TRACK_UNAVAILABLE_OFFLINE)));

        resolver.handleIntent(intent, context);

        verify(navigator, never()).openStreamWithExpandedPlayer(context, Screen.DEEPLINK);
        verify(navigator).openLauncher(context);
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
    public void shouldTrackForegroundEventsWithResources() {
        setupIntentForUrl("soundcloud://sounds:123");
        when(resolveOperations.resolve(uri)).thenReturn(Observable.just(Urn.forTrack(123)));
        setupReferrer(Referrer.TWITTER);

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Urn.forTrack(123), Referrer.TWITTER);
    }

    @Test
    public void shouldTrackForegroundEventsWithResourceWhenUserIsNotLoggedIn() {
        setupIntentForUrl("soundcloud://sounds:123");
        when(resolveOperations.resolve(uri)).thenReturn(Observable.just(Urn.forTrack(123)));
        setupReferrer(Referrer.FACEBOOK);
        when(accountOperations.isUserLoggedIn()).thenReturn(false);

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Urn.forTrack(123), Referrer.FACEBOOK);
    }

    @Test
    public void shouldTrackForegroundEventsWithoutResourcesForHome() {
        intent = new Intent();

        resolver.handleIntent(intent, context);

        verify(navigator).openStream(context, Screen.DEEPLINK);
        verifyTrackingEvent(Referrer.OTHER);
    }

    @Test
    public void shouldLoginCrawler() {
        setupIntentForUrl("soundcloud://playlists:123");
        when(resolveOperations.resolve(uri)).thenReturn(Observable.just(Urn.forPlaylist(123)));
        setupReferrer(Referrer.GOOGLE_CRAWLER);

        resolver.handleIntent(intent, context);

        verify(accountOperations).loginCrawlerUser();
        verify(navigator).openPlaylist(context, Urn.forPlaylist(123), Screen.DEEPLINK);
    }

    @Test
    public void shouldStartPlaybackForTracksOnCrawlersWithoutRelated() {
        setupIntentForUrl("soundcloud://sounds:123");
        setupReferrer(Referrer.GOOGLE_CRAWLER);
        when(resolveOperations.resolve(uri)).thenReturn(Observable.just(Urn.forTrack(123)));

        resolver.handleIntent(intent, context);

        verify(playbackInitiator).startPlayback(Urn.forTrack(123), Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchOnboardingWithExtraUrnForLoggedOutUsers() {
        setupIntentForUrl("soundcloud://sounds:123");
        when(resolveOperations.resolve(uri)).thenReturn(Observable.just(Urn.forTrack(123)));
        when(accountOperations.isUserLoggedIn()).thenReturn(false);

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Urn.forTrack(123), Referrer.OTHER);
        verify(navigator).openOnboarding(context, Urn.forTrack(123), Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchSearchForWebScheme() {
        setupIntentForUrl("https://soundcloud.com/search?q=skrillex");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER);
        verify(navigator).openSearch(context, uri, Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchSearchForSoundCloudScheme() {
        setupIntentForUrl("soundcloud://search?q=skrillex");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER);
        verify(navigator).openSearch(context, uri, Screen.DEEPLINK);
    }

    @Test
    public void shouldNotLaunchSearchForLoggedOutUsers() {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        setupIntentForUrl("soundcloud://search?q=skrillex");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER);
        verify(navigator).openOnboarding(context, Urn.NOT_SET, Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchSearchForCrawlers() {
        setupReferrer(Referrer.GOOGLE_CRAWLER);
        setupIntentForUrl("soundcloud://search?q=skrillex");

        resolver.handleIntent(intent, context);

        verify(accountOperations).loginCrawlerUser();
        verifyTrackingEvent(Referrer.GOOGLE_CRAWLER);
        verify(navigator).openSearch(context, uri, Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchExploreForWebScheme() {
        setupIntentForUrl("https://soundcloud.com/explore");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER);
        verify(navigator).openExplore(context, Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchExploreForSoundCloudScheme() {
        setupIntentForUrl("soundcloud://explore");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER);
        verify(navigator).openExplore(context, Screen.DEEPLINK);
    }

    @Test
    public void shouldNotLaunchExploreForLoggedOutUsers() {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        setupIntentForUrl("soundcloud://explore");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER);
        verify(navigator).openOnboarding(context, Urn.NOT_SET, Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchExploreForCrawlers() {
        setupReferrer(Referrer.GOOGLE_CRAWLER);
        setupIntentForUrl("soundcloud://explore");

        resolver.handleIntent(intent, context);

        verify(accountOperations).loginCrawlerUser();
        verifyTrackingEvent(Referrer.GOOGLE_CRAWLER);
        verify(navigator).openExplore(context, Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchRecordForWebScheme() {
        setupIntentForUrl("https://soundcloud.com/upload");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER);
        verify(navigator).openRecord(context, Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchRecordForSoundCloudSchemeWithUpload() {
        setupIntentForUrl("soundcloud://upload");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER);
        verify(navigator).openRecord(context, Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchRecordForSoundCloudSchemeWithRecord() {
        setupIntentForUrl("soundcloud://record");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER);
        verify(navigator).openRecord(context, Screen.DEEPLINK);
    }

    @Test
    public void shouldNotLaunchRecordForLoggedOutUsers() {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        setupIntentForUrl("soundcloud://record");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER);
        verify(navigator).openOnboarding(context, Urn.NOT_SET, Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchRecordForCrawlers() {
        setupReferrer(Referrer.GOOGLE_CRAWLER);
        setupIntentForUrl("soundcloud://upload");

        resolver.handleIntent(intent, context);

        verify(accountOperations).loginCrawlerUser();
        verifyTrackingEvent(Referrer.GOOGLE_CRAWLER);
        verify(navigator).openRecord(context, Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchUpgradeForSoundCloudScheme() {
        when(featureOperations.upsellHighTier()).thenReturn(true);
        setupIntentForUrl("soundcloud://soundcloudgo");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER, Screen.CONVERSION);
        verify(navigator).openUpgradeOnMain(context);
    }

    @Test
    public void shouldNotLaunchUpgradeWhenUpsellFeatureIsDisabled() {
        when(featureOperations.upsellHighTier()).thenReturn(false);
        setupIntentForUrl("soundcloud://soundcloudgo");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER);
        verify(navigator).openStream(context, Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchCheckoutForSoundCloudScheme() {
        when(featureOperations.upsellHighTier()).thenReturn(true);
        setupIntentForUrl("soundcloud://buysoundcloudgo");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER, Screen.CHECKOUT);
        verify(navigator).openDirectCheckout(context);
    }

    @Test
    public void shouldLaunchOfflineSettingsForSoundCloudScheme() {
        when(featureOperations.isOfflineContentOrUpsellEnabled()).thenReturn(true);
        setupIntentForUrl("soundcloud://settings_offlinelistening");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER, Screen.SETTINGS_OFFLINE);
        verify(navigator).openOfflineSettings(context);
    }

    @Test
    public void shouldNotLaunchOfflineSettingsWhenPlanAndUpsellAreDisabled() {
        when(featureOperations.isOfflineContentOrUpsellEnabled()).thenReturn(false);
        setupIntentForUrl("soundcloud://settings_offlinelistening");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER);
        verify(navigator).openStream(context, Screen.DEEPLINK);
    }

    @Test
    public void shouldNotLaunchCheckoutWhenUpsellFeatureIsDisabled() {
        when(featureOperations.upsellHighTier()).thenReturn(false);
        setupIntentForUrl("soundcloud://buysoundcloudgo");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER);
        verify(navigator).openStream(context, Screen.DEEPLINK);
    }

    @Test
    public void shouldLaunchNotificationPreferences() {
        setupIntentForUrl("soundcloud://notification_preferences");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER);
        verify(navigator).openNotificationPreferencesFromDeeplink(context);
    }

    @Test
    public void shouldLaunchCollection() {
        setupIntentForUrl("soundcloud://collection");

        resolver.handleIntent(intent, context);

        verifyTrackingEvent(Referrer.OTHER);
        verify(navigator).openCollection(context);
    }

    @Test
    public void shouldOpenDiscoveryFromWebLink() {
        setupIntentForUrl("https://soundcloud.com/discover");

        resolver.handleIntent(intent, context);

        verify(navigator).openDiscovery(context, Screen.DEEPLINK);
    }

    @Test
    public void shouldOpenDiscoveryFromUri() {
        setupIntentForUrl("soundcloud://discovery");

        resolver.handleIntent(intent, context);

        verify(navigator).openDiscovery(context, Screen.DEEPLINK);
    }

    public void setupIntentForUrl(String url) {
        if (url != null) {
            uri = Uri.parse(url);
        }
        intent = new Intent().setData(uri);
    }

    private void setupReferrer(Referrer referrer) {
        when(referrerResolver.getReferrerFromIntent(any(Intent.class),
                                                    any(Resources.class))).thenReturn(referrer.value());
    }

    private ForegroundEvent captureForegroundEvent() {
        ArgumentCaptor<ForegroundEvent> captor = ArgumentCaptor.forClass(ForegroundEvent.class);
        verify(eventBus).publish(eq(EventQueue.TRACKING), captor.capture());
        return captor.getValue();
    }

    private void verifyTrackingEvent(Urn urn, Referrer referrer) {
        ForegroundEvent event = captureForegroundEvent();

        assertThat(event.getKind()).isEqualTo(ForegroundEvent.KIND_OPEN);
        assertThat(event.get(ForegroundEvent.KEY_REFERRER)).isEqualTo(referrer.value());
        assertThat(event.get(ForegroundEvent.KEY_PAGE_NAME)).isEqualTo(Screen.DEEPLINK.get());
        assertThat(event.get(ForegroundEvent.KEY_PAGE_URN)).isEqualTo(urn.toString());
    }

    private void verifyTrackingEvent(Referrer referrer) {
        verifyTrackingEvent(referrer, Screen.DEEPLINK);
    }

    private void verifyTrackingEvent(Referrer referrer, Screen screen) {
        ForegroundEvent event = captureForegroundEvent();

        assertThat(event.getKind()).isEqualTo(ForegroundEvent.KIND_OPEN);
        assertThat(event.get(ForegroundEvent.KEY_REFERRER)).isEqualTo(referrer.value());
        assertThat(event.get(ForegroundEvent.KEY_PAGE_NAME)).isEqualTo(screen.get());
    }

}
