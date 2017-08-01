package com.soundcloud.android.navigation;

import static com.soundcloud.android.navigation.IntentFactory.createCollectionIntent;
import static com.soundcloud.android.navigation.IntentFactory.createDirectCheckoutIntent;
import static com.soundcloud.android.navigation.IntentFactory.createDiscoveryIntent;
import static com.soundcloud.android.navigation.IntentFactory.createEmailIntent;
import static com.soundcloud.android.navigation.IntentFactory.createExternalAppIntent;
import static com.soundcloud.android.navigation.IntentFactory.createHomeIntent;
import static com.soundcloud.android.navigation.IntentFactory.createNotificationPreferencesFromDeeplinkIntent;
import static com.soundcloud.android.navigation.IntentFactory.createOnboardingIntent;
import static com.soundcloud.android.navigation.IntentFactory.createPlaylistIntent;
import static com.soundcloud.android.navigation.IntentFactory.createProductChoiceIntent;
import static com.soundcloud.android.navigation.IntentFactory.createRemoteSignInIntent;
import static com.soundcloud.android.navigation.IntentFactory.createStreamIntent;
import static com.soundcloud.android.playback.PlaybackResult.ErrorReason.TRACK_UNAVAILABLE_OFFLINE;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.robolectric.Shadows.shadowOf;

import com.soundcloud.android.PlaybackServiceController;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.deeplinks.ChartDetails;
import com.soundcloud.android.deeplinks.ChartsUriResolver;
import com.soundcloud.android.events.DeeplinkReportEvent;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.customtabs.CustomTabsHelper;
import com.soundcloud.android.navigation.customtabs.CustomTabsMetadata;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.olddiscovery.DefaultHomeScreenConfiguration;
import com.soundcloud.android.olddiscovery.charts.Chart;
import com.soundcloud.android.onboarding.auth.SignInOperations;
import com.soundcloud.android.payments.UpsellContext;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.search.topresults.TopResultsBucketViewModel;
import com.soundcloud.android.stations.StationsUriResolver;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.assertions.IntentAssert;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import com.tobedevoured.modelcitizen.CreateModelException;
import io.reactivex.Single;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.res.builder.RobolectricPackageManager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;

import java.util.Collections;
import java.util.List;

public class NavigationResolverTest extends AndroidUnitTest {
    private static final String TOP_FIFTY = "Top 50";
    private static final ResolveResult RESULT_TRACK = ResolveResult.success(Urn.forTrack(123));
    private static final Screen NAVIGATION_SCREEN = Screen.DISCOVER;
    private static final Screen DEEPLINK_SCREEN = Screen.DEEPLINK;

    private final LocalEntityUriResolver localEntityUriResolver = new LocalEntityUriResolver();
    private final StationsUriResolver stationsUriResolver = new StationsUriResolver();
    @Mock private ResolveOperations resolveOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private EventBus eventBus;
    @Mock private FeatureOperations featureOperations;
    @Mock private Resources resources;
    @Mock private ChartsUriResolver chartsUriResolver;
    @Mock private SignInOperations signInOperations;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaybackServiceController playbackServiceController;
    @Mock private ApplicationProperties applicationProperties;
    @Mock private EventTracker eventTracker;
    @Mock private DefaultHomeScreenConfiguration homeScreenConfiguration;
    @Mock private OfflineSettingsStorage offlineSettingsStorage;

    private NavigationResolver resolver;
    private Context context;

    @Before
    public void setUp() {
        context = activity();

        resolver = new NavigationResolver(context,
                                          resolveOperations,
                                          localEntityUriResolver,
                                          accountOperations,
                                          playbackServiceController,
                                          playbackInitiator,
                                          playQueueManager,
                                          eventBus,
                                          featureOperations,
                                          chartsUriResolver,
                                          signInOperations,
                                          stationsUriResolver,
                                          applicationProperties,
                                          eventTracker,
                                          homeScreenConfiguration,
                                          offlineSettingsStorage);

        when(accountOperations.isUserLoggedIn()).thenReturn(true);
        when(playbackInitiator.startPlayback(any(Urn.class),
                                             any(Screen.class))).thenReturn(Single.never());

        when(homeScreenConfiguration.isStreamHome()).thenReturn(true);
        when(resources.getString(R.string.charts_top)).thenReturn(TOP_FIFTY);
    }

    // For Deeplink

    @Test
    public void deeplink_shouldLaunchSoundStreamIfUriBlank() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(true);
        NavigationTarget navigationTarget = getTargetForDeeplink("");

        assertTarget(navigationTarget, createStreamIntent(DEEPLINK_SCREEN));
    }

    @Test
    public void deeplink_shouldLaunchSoundStreamIfUriNull() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(true);
        NavigationTarget navigationTarget = getTargetForDeeplink(null);

        assertTarget(navigationTarget, createStreamIntent(DEEPLINK_SCREEN));
    }

    @Test
    public void deeplink_shouldLaunchSoundStreamIfUriBlank_newHome() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(false);
        NavigationTarget navigationTarget = getTargetForDeeplink("");

        assertTarget(navigationTarget, createDiscoveryIntent(DEEPLINK_SCREEN));
    }

    @Test
    public void deeplink_shouldLaunchSoundStreamIfUriNull_newHome() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(false);
        NavigationTarget navigationTarget = getTargetForDeeplink(null);

        assertTarget(navigationTarget, createDiscoveryIntent(DEEPLINK_SCREEN));
    }

    @Test
    public void deeplink_shouldConvertOpaqueUriToHierarchicalAndLaunchPlayer() throws Exception {
        String target = "soundcloud:tracks:123";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);
        when(playbackInitiator.startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN)).thenReturn(Single.just(PlaybackResult.success()));

        assertTarget(navigationTarget, IntentFactory.createStreamWithExpandedPlayerIntent(DEEPLINK_SCREEN));

        verify(resolveOperations, never()).resolve(anyString());
        verify(playbackInitiator).startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldResolveLocallyAndLaunchPlayer() throws Exception {
        NavigationTarget navigationTarget = getTargetForDeeplink("soundcloud://tracks:123");
        when(playbackInitiator.startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN)).thenReturn(Single.just(PlaybackResult.success()));

        assertTarget(navigationTarget, IntentFactory.createStreamWithExpandedPlayerIntent(DEEPLINK_SCREEN));

        verify(resolveOperations, never()).resolve(anyString());
        verify(playbackInitiator).startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldConvertOpaqueUriWithAdjustReftagQueryParamToHierarchicalAndLaunchPlayer() throws Exception {
        String target = "soundcloud:tracks:123?adjust_reftag=c6vlQZj4w9FOi";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);
        when(resolveOperations.resolve(target)).thenReturn(Single.just(RESULT_TRACK));
        when(playbackInitiator.startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN)).thenReturn(Single.just(PlaybackResult.success()));

        assertTarget(navigationTarget, IntentFactory.createStreamWithExpandedPlayerIntent(DEEPLINK_SCREEN));

        verify(resolveOperations).resolve(target);
        verify(playbackInitiator).startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldLaunchPlayerDirectlyFromHierarchicalUriSplitByColon() throws CreateModelException {
        String target = "soundcloud://sounds:123";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);
        when(resolveOperations.resolve(target)).thenReturn(Single.just(RESULT_TRACK));
        when(playbackInitiator.startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN)).thenReturn(Single.just(PlaybackResult.success()));

        assertTarget(navigationTarget, IntentFactory.createStreamWithExpandedPlayerIntent(DEEPLINK_SCREEN));

        verify(resolveOperations).resolve(target);
        verify(playbackInitiator).startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldLaunchPlayerDirectlyFromHierarchicalUriSplitBySlash() throws CreateModelException {
        String target = "soundcloud://tracks/123";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);
        when(playbackInitiator.startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN)).thenReturn(Single.just(PlaybackResult.success()));

        assertTarget(navigationTarget, IntentFactory.createStreamWithExpandedPlayerIntent(DEEPLINK_SCREEN));

        verify(resolveOperations, never()).resolve(anyString());
        verify(playbackInitiator).startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldShowTheStreamWithAnExpandedPlayer() throws CreateModelException {
        String target = "soundcloud://sounds:123";
        when(resolveOperations.resolve(target)).thenReturn(Single.just(RESULT_TRACK));
        NavigationTarget navigationTarget = getTargetForDeeplink(target);
        when(playbackInitiator.startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN))
                .thenReturn(Single.just(PlaybackResult.success()));

        assertTarget(navigationTarget, IntentFactory.createStreamWithExpandedPlayerIntent(DEEPLINK_SCREEN));

        verify(resolveOperations).resolve(target);
        verify(playbackInitiator).startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldNotOpenStreamWhenFailedToStartPlayback() throws CreateModelException {
        String target = "soundcloud://sounds:123";
        when(resolveOperations.resolve(target)).thenReturn(Single.just(RESULT_TRACK));
        NavigationTarget navigationTarget = getTargetForDeeplink(target);
        when(playbackInitiator.startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN))
                .thenReturn(Single.just(PlaybackResult.error(TRACK_UNAVAILABLE_OFFLINE)));

        assertTarget(navigationTarget, IntentFactory.createLauncherIntent(context));

        verify(resolveOperations).resolve(target);
        verify(playbackInitiator).startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldConvertOpaqueUriToHierarchicalAndLaunchPlaylistDetails() throws Exception {
        String target = "soundcloud:playlists:123";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createPlaylistIntent(Urn.forPlaylist(123), DEEPLINK_SCREEN, false));

        verify(resolveOperations, never()).resolve(anyString());
    }

    @Test
    public void deeplink_shouldLaunchPlaylistDetailsDirectlyFromHierarchicalUriSplitByColon() throws CreateModelException {
        String target = "soundcloud://playlists:123";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createPlaylistIntent(Urn.forPlaylist(123), DEEPLINK_SCREEN, false));

        verify(resolveOperations, never()).resolve(anyString());
    }

    @Test
    public void deeplink_shouldConvertOpaqueUriToHierarchicalAndLaunchUserProfile() throws Exception {
        String target = "soundcloud:users:123";
        Urn urn = Urn.forUser(123);
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, IntentFactory.createProfileIntent(context, urn, Optional.of(DEEPLINK_SCREEN), Optional.absent(), Optional.of(Referrer.OTHER)));

        verify(resolveOperations, never()).resolve(anyString());
        verify(eventTracker, never()).trackNavigation(any(UIEvent.class));
        verify(eventBus).publish(EventQueue.TRACKING, ForegroundEvent.open(navigationTarget.screen(), navigationTarget.referrer().get()));
    }

    @Test
    public void deeplink_shouldLaunchUserProfileDirectlyFromHierarchicalUriSplitByColon() throws CreateModelException {
        String target = "soundcloud://users:123";
        Urn urn = Urn.forUser(123);
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, IntentFactory.createProfileIntent(context, urn, Optional.of(DEEPLINK_SCREEN), Optional.absent(), Optional.of(Referrer.OTHER)));

        verify(resolveOperations, never()).resolve(anyString());
        verify(eventTracker, never()).trackNavigation(any(UIEvent.class));
        verify(eventBus).publish(EventQueue.TRACKING, ForegroundEvent.open(navigationTarget.screen(), navigationTarget.referrer().get()));
    }

    @Test
    public void deeplink_shouldTrackForegroundEventsWithResources() throws Exception {
        String target = "soundcloud://sounds:123";
        NavigationTarget navigationTarget = getTargetForDeeplink(target, Referrer.TWITTER);
        when(resolveOperations.resolve(target)).thenReturn(Single.just(RESULT_TRACK));
        when(playbackInitiator.startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN))
                .thenReturn(Single.just(PlaybackResult.success()));

        resolveTarget(navigationTarget);

        verify(resolveOperations).resolve(target);
        ArgumentCaptor<TrackingEvent> captor = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(eventBus, times(2)).publish(eq(EventQueue.TRACKING), captor.capture());

        ForegroundEvent event = (ForegroundEvent) captor.getAllValues().get(0);
        verifyTrackingEvent(event, Urn.forTrack(123), navigationTarget.referrer());
    }

    @Test
    public void deeplink_shouldTrackForegroundEventsWithResourcesOnPlaybackFailed() throws Exception {
        String target = "soundcloud://sounds:123";
        NavigationTarget navigationTarget = getTargetForDeeplink(target, Referrer.TWITTER);
        when(resolveOperations.resolve(target)).thenReturn(Single.just(RESULT_TRACK));
        when(playbackInitiator.startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN))
                .thenReturn(Single.just(PlaybackResult.error(TRACK_UNAVAILABLE_OFFLINE)));

        resolveTarget(navigationTarget);

        verify(resolveOperations).resolve(target);
        ArgumentCaptor<TrackingEvent> captor = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(eventBus, times(2)).publish(eq(EventQueue.TRACKING), captor.capture());

        ForegroundEvent event = (ForegroundEvent) captor.getAllValues().get(0);
        verifyTrackingEvent(event, Urn.forTrack(123), navigationTarget.referrer());
    }

    @Test
    public void deeplink_shouldTrackForegroundEventsWithResourceWhenUserIsNotLoggedIn() throws Exception {
        String target = "soundcloud://sounds:123";
        NavigationTarget navigationTarget = getTargetForDeeplink(target, Referrer.FACEBOOK);
        when(resolveOperations.resolve(target)).thenReturn(Single.just(RESULT_TRACK));
        when(playbackInitiator.startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN))
                .thenReturn(Single.just(PlaybackResult.success()));
        when(accountOperations.isUserLoggedIn()).thenReturn(false);

        resolveTarget(navigationTarget);

        verify(resolveOperations).resolve(target);
        ArgumentCaptor<TrackingEvent> captor = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(eventBus, times(2)).publish(eq(EventQueue.TRACKING), captor.capture());

        ForegroundEvent event = (ForegroundEvent) captor.getAllValues().get(0);
        verifyTrackingEvent(event, Urn.forTrack(123), navigationTarget.referrer());
    }

    @Test
    public void deeplink_shouldTrackForegroundEventsWithoutResourcesForHome() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(true);
        NavigationTarget navigationTarget = getTargetForDeeplink(null);

        assertTarget(navigationTarget, createStreamIntent(DEEPLINK_SCREEN));
        verifyTrackingEvent(navigationTarget.referrer());
    }

    @Test
    public void deeplink_shouldTrackForegroundEventsWithoutResourcesForHome_newHome() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(false);
        NavigationTarget navigationTarget = getTargetForDeeplink(null);

        assertTarget(navigationTarget, createDiscoveryIntent(DEEPLINK_SCREEN));

        verifyTrackingEvent(navigationTarget.referrer());
    }

    @Test
    public void deeplink_shouldLoginCrawler() throws Exception {
        String target = "soundcloud://sounds:123";
        NavigationTarget navigationTarget = getTargetForDeeplink(target, Referrer.GOOGLE_CRAWLER);
        when(resolveOperations.resolve(target)).thenReturn(Single.just(RESULT_TRACK));
        when(playbackInitiator.startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN))
                .thenReturn(Single.just(PlaybackResult.success()));

        assertTarget(navigationTarget, IntentFactory.createStreamWithExpandedPlayerIntent(DEEPLINK_SCREEN));

        verify(resolveOperations).resolve(target);
        verify(accountOperations).loginCrawlerUser();
        verify(playbackInitiator).startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldLaunchOnboardingWithExtraUrnForLoggedOutUsers() throws Exception {
        String target = "soundcloud://sounds:123";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);
        when(resolveOperations.resolve(target)).thenReturn(Single.just(RESULT_TRACK));
        when(accountOperations.isUserLoggedIn()).thenReturn(false);

        assertTarget(navigationTarget, createOnboardingIntent(context, DEEPLINK_SCREEN, Uri.parse(target)));

        ArgumentCaptor<TrackingEvent> captor = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(eventBus, times(2)).publish(eq(EventQueue.TRACKING), captor.capture());

        ForegroundEvent event = (ForegroundEvent) captor.getAllValues().get(0);
        verifyTrackingEvent(event, Urn.forTrack(123), Optional.of(Referrer.OTHER.value()));

        DeeplinkReportEvent reportEvent = (DeeplinkReportEvent) captor.getAllValues().get(1);
        assertThat(reportEvent.kind()).isEqualTo(DeeplinkReportEvent.forResolvedDeeplink(Referrer.OTHER.toString()).kind());
    }

    @Test
    public void deeplink_shouldLaunchSearchForWebScheme() throws Exception {
        String target = "https://soundcloud.com/search?q=skrillex";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, IntentFactory.createSearchActionIntent(context, Uri.parse(target), DEEPLINK_SCREEN));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldLaunchSearchForSoundCloudScheme() throws Exception {
        String target = "soundcloud://search?q=skrillex";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, IntentFactory.createSearchActionIntent(context, Uri.parse(target), DEEPLINK_SCREEN));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldNotLaunchSearchForLoggedOutUsers() throws Exception {
        String target = "soundcloud://search?q=skrillex";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);
        when(accountOperations.isUserLoggedIn()).thenReturn(false);

        assertTarget(navigationTarget, createOnboardingIntent(context, DEEPLINK_SCREEN, Uri.parse(target)));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldLaunchSearchForCrawlers() throws Exception {
        String target = "soundcloud://search?q=skrillex";
        NavigationTarget navigationTarget = getTargetForDeeplink(target, Referrer.GOOGLE_CRAWLER);

        assertTarget(navigationTarget, IntentFactory.createSearchActionIntent(context, Uri.parse(target), DEEPLINK_SCREEN));

        verifyTrackingEvent(Optional.of(Referrer.GOOGLE_CRAWLER.value()));
        verify(accountOperations).loginCrawlerUser();
    }

    @Test
    public void deeplink_shouldLaunchRecordForWebScheme() throws Exception {
        shadowOf(activity().getApplication()).grantPermissions(Manifest.permission.RECORD_AUDIO);

        String target = "https://soundcloud.com/upload";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, IntentFactory.createRecordIntent(context, Optional.absent(), DEEPLINK_SCREEN));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldLaunchRecordPermissionsForWebSchemeWhenNotGranted() throws Exception {
        String target = "https://soundcloud.com/upload";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, IntentFactory.createRecordPermissionIntent(context, Optional.absent(), DEEPLINK_SCREEN));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldLaunchRecordForSoundCloudSchemeWithUpload() throws Exception {
        shadowOf(activity().getApplication()).grantPermissions(Manifest.permission.RECORD_AUDIO);

        String target = "soundcloud://upload";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, IntentFactory.createRecordIntent(context, Optional.absent(), DEEPLINK_SCREEN));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldLaunchRecordPermissionsForSoundCloudSchemeWithUploadWhenNotGranted() throws Exception {
        String target = "soundcloud://upload";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, IntentFactory.createRecordPermissionIntent(context, Optional.absent(), DEEPLINK_SCREEN));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldLaunchRecordForSoundCloudSchemeWithRecord() throws Exception {
        shadowOf(activity().getApplication()).grantPermissions(Manifest.permission.RECORD_AUDIO);

        String target = "soundcloud://record";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, IntentFactory.createRecordIntent(context, Optional.absent(), DEEPLINK_SCREEN));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldLaunchRecordPermissionsForSoundCloudSchemeWithRecordWhenNotGranted() throws Exception {
        String target = "soundcloud://record";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, IntentFactory.createRecordPermissionIntent(context, Optional.absent(), DEEPLINK_SCREEN));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldNotLaunchRecordForLoggedOutUsers() throws Exception {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        String target = "soundcloud://record";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createOnboardingIntent(context, DEEPLINK_SCREEN, Uri.parse(target)));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldLaunchRecordForCrawlers() throws Exception {
        shadowOf(activity().getApplication()).grantPermissions(Manifest.permission.RECORD_AUDIO);

        String target = "soundcloud://upload";
        NavigationTarget navigationTarget = getTargetForDeeplink(target, Referrer.GOOGLE_CRAWLER);

        assertTarget(navigationTarget, IntentFactory.createRecordIntent(context, Optional.absent(), DEEPLINK_SCREEN));

        verify(accountOperations).loginCrawlerUser();
        verifyTrackingEvent(Optional.of(Referrer.GOOGLE_CRAWLER.value()));
    }

    @Test
    public void deeplink_shouldLaunchRecordPermissionsForCrawlersWhenNotPermitted() throws Exception {
        String target = "soundcloud://upload";
        NavigationTarget navigationTarget = getTargetForDeeplink(target, Referrer.GOOGLE_CRAWLER);

        assertTarget(navigationTarget, IntentFactory.createRecordPermissionIntent(context, Optional.absent(), DEEPLINK_SCREEN));

        verify(accountOperations).loginCrawlerUser();
        verifyTrackingEvent(Optional.of(Referrer.GOOGLE_CRAWLER.value()));
    }

    @Test
    public void deeplink_shouldLaunchUpgradeForSoundCloudScheme() throws Exception {
        when(featureOperations.upsellHighTier()).thenReturn(true);
        String target = "soundcloud://soundcloudgo";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, IntentFactory.createConversionIntent(context, UpsellContext.DEFAULT), Collections.singletonList(createHomeIntent(context)));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()), Screen.CONVERSION);
    }

    @Test
    public void deeplink_shouldNotLaunchUpgradeWhenUpsellFeatureIsDisabled() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(true);
        when(featureOperations.upsellHighTier()).thenReturn(false);
        String target = "soundcloud://soundcloudgo";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createStreamIntent(DEEPLINK_SCREEN));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldNotLaunchUpgradeWhenUpsellFeatureIsDisabled_newHome() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(false);
        when(featureOperations.upsellHighTier()).thenReturn(false);
        String target = "soundcloud://soundcloudgo";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createDiscoveryIntent(DEEPLINK_SCREEN));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldNotLaunchMidTierCheckoutIfUserAlreadyHasAHighTierPlan() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(true);
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.HIGH_TIER);
        String target = "soundcloud://buysoundcloudgo";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createStreamIntent(DEEPLINK_SCREEN), context().getString(R.string.product_choice_error_already_subscribed));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldNotLaunchMidTierCheckoutIfUserAlreadyHasAHighTierPlan_newHome() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(false);
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.HIGH_TIER);
        String target = "soundcloud://buysoundcloudgo";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createDiscoveryIntent(DEEPLINK_SCREEN), context().getString(R.string.product_choice_error_already_subscribed));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldNotLaunchMidTierCheckoutIfUserAlreadyHasAMidTierPlan() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(true);
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.MID_TIER);
        String target = "soundcloud://buysoundcloudgo";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createStreamIntent(DEEPLINK_SCREEN), context().getString(R.string.product_choice_error_already_subscribed));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldNotLaunchMidTierCheckoutIfUserAlreadyHasAMidTierPlan_newHome() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(false);
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.MID_TIER);
        String target = "soundcloud://buysoundcloudgo";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createDiscoveryIntent(DEEPLINK_SCREEN), context().getString(R.string.product_choice_error_already_subscribed));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldLaunchMidTierCheckoutForFreeUserWithSoundCloudScheme() throws Exception {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellBothTiers()).thenReturn(true);
        String target = "soundcloud://buysoundcloudgo";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createDirectCheckoutIntent(context, Plan.MID_TIER));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()), Screen.CHECKOUT);
    }

    @Test
    public void deeplink_shouldNotLaunchMidTierCheckout() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(true);
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellBothTiers()).thenReturn(false);
        String target = "soundcloud://buysoundcloudgo";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createStreamIntent(DEEPLINK_SCREEN));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldNotLaunchMidTierCheckout_newHome() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(false);
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellBothTiers()).thenReturn(false);
        String target = "soundcloud://buysoundcloudgo";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createDiscoveryIntent(DEEPLINK_SCREEN));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldNotLaunchHighTierCheckoutIfUserAlreadyHasAHighTierPlan() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(true);
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.HIGH_TIER);
        String target = "soundcloud://buysoundcloudgoplus";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createStreamIntent(DEEPLINK_SCREEN), context().getString(R.string.product_choice_error_already_subscribed));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldNotLaunchHighTierCheckoutIfUserAlreadyHasAHighTierPlan_newHome() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(false);
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.HIGH_TIER);
        String target = "soundcloud://buysoundcloudgoplus";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createDiscoveryIntent(DEEPLINK_SCREEN), context().getString(R.string.product_choice_error_already_subscribed));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldLaunchHighTierCheckoutForMidTierUserWithSoundCloudScheme() throws Exception {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.MID_TIER);
        when(featureOperations.upsellHighTier()).thenReturn(true);
        String target = "soundcloud://buysoundcloudgoplus";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createDirectCheckoutIntent(context, Plan.HIGH_TIER));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()), Screen.CHECKOUT);
    }

    @Test
    public void deeplink_shouldLaunchHighTierCheckoutForFreeUserWithSoundCloudScheme() throws Exception {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellHighTier()).thenReturn(true);
        String target = "soundcloud://buysoundcloudgoplus";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createDirectCheckoutIntent(context, Plan.HIGH_TIER));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()), Screen.CHECKOUT);
    }

    @Test
    public void deeplink_shouldNotLaunchHighTierCheckout() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(true);
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellHighTier()).thenReturn(false);
        String target = "soundcloud://buysoundcloudgoplus";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createStreamIntent(DEEPLINK_SCREEN));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldNotLaunchHighTierCheckout_newHome() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(false);
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellHighTier()).thenReturn(false);
        String target = "soundcloud://buysoundcloudgoplus";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createDiscoveryIntent(DEEPLINK_SCREEN));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldNotLaunchProductChoiceIfUserAlreadyHasAGoPlan() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(true);
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.HIGH_TIER);
        String target = "soundcloud://soundcloudgo/soundcloudgo";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createStreamIntent(DEEPLINK_SCREEN), context().getString(R.string.product_choice_error_already_subscribed));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldNotLaunchProductChoiceIfUserAlreadyHasAGoPlan_newHome() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(false);
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.HIGH_TIER);
        String target = "soundcloud://soundcloudgo/soundcloudgo";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createDiscoveryIntent(DEEPLINK_SCREEN), context().getString(R.string.product_choice_error_already_subscribed));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldLaunchProductChoiceForMidTierUpsell() throws Exception {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellBothTiers()).thenReturn(true);
        String target = "soundcloud://soundcloudgo/soundcloudgo";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createProductChoiceIntent(context, Plan.MID_TIER), Collections.singletonList(createHomeIntent(context)));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()), Screen.CONVERSION);
    }

    @Test
    public void deeplink_shouldLaunchProductChoiceForHighTierUpsell() throws Exception {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellBothTiers()).thenReturn(true);
        String target = "soundcloud://soundcloudgo/soundcloudgoplus";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createProductChoiceIntent(context, Plan.HIGH_TIER), Collections.singletonList(createHomeIntent(context)));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()), Screen.CONVERSION);
    }

    @Test
    public void deeplink_shouldNotLaunchProductChoice() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(true);
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellBothTiers()).thenReturn(false);
        String target = "soundcloud://soundcloudgo/soundcloudgo";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createStreamIntent(DEEPLINK_SCREEN));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldNotLaunchProductChoice_newHome() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(false);
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellBothTiers()).thenReturn(false);
        String target = "soundcloud://soundcloudgo/soundcloudgo";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createDiscoveryIntent(DEEPLINK_SCREEN));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldLaunchOfflineSettingsForSoundCloudSchemeWhenOnboardingNotSeenBefore() throws Exception {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(offlineSettingsStorage.hasSeenOfflineSettingsOnboarding()).thenReturn(false);

        String target = "soundcloud://settings_offlinelistening";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, IntentFactory.createOfflineSettingsIntent(context));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()), Screen.SETTINGS_OFFLINE);
    }

    @Test
    public void deeplink_shouldLaunchOfflineSettingsForSoundCloudSchemeWhenOnboardingSeenBefore() throws Exception {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(offlineSettingsStorage.hasSeenOfflineSettingsOnboarding()).thenReturn(true);

        String target = "soundcloud://settings_offlinelistening";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, IntentFactory.createOfflineSettingsIntent(context));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()), Screen.SETTINGS_OFFLINE);
    }

    @Test
    public void deeplink_shouldNotLaunchOfflineSettingsWhenOfflineContentIsNotEnabled() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(true);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        String target = "soundcloud://settings_offlinelistening";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createStreamIntent(DEEPLINK_SCREEN));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldNotLaunchOfflineSettingsWhenOfflineContentIsNotEnabled_newHome() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(false);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        String target = "soundcloud://settings_offlinelistening";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createDiscoveryIntent(DEEPLINK_SCREEN));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldLaunchNotificationPreferences() throws Exception {
        String target = "soundcloud://notification_preferences";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createNotificationPreferencesFromDeeplinkIntent(context));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldLaunchCollection() throws Exception {
        String target = "soundcloud://collection";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createCollectionIntent());

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
    }

    @Test
    public void deeplink_shouldOpenDiscoverFromWebLink() throws Exception {
        String target = "https://soundcloud.com/discover";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createDiscoveryIntent(Screen.DEEPLINK));
    }

    @Test
    public void deeplink_shouldOpenChartsFromWebLink() throws Exception {
        String target = "https://soundcloud.com/charts/top?genre=all";
        when(chartsUriResolver.resolveUri(Uri.parse(target))).thenReturn(ChartDetails.create(ChartType.TOP, Chart.GLOBAL_GENRE, ChartCategory.MUSIC, Optional.of(TOP_FIFTY)));
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, IntentFactory.createChartsIntent(context, ChartDetails.create(ChartType.TOP,
                                                                                                     Chart.GLOBAL_GENRE,
                                                                                                     ChartCategory.MUSIC,
                                                                                                     Optional.of(TOP_FIFTY))));
    }

    @Test
    public void deeplink_shouldOpenChartsFromUri() throws Exception {
        String target = "soundcloud://charts:top:all";
        when(chartsUriResolver.resolveUri(Uri.parse(target))).thenReturn(ChartDetails.create(ChartType.TOP, Chart.GLOBAL_GENRE, ChartCategory.MUSIC, Optional.of(TOP_FIFTY)));
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, IntentFactory.createChartsIntent(context, ChartDetails.create(ChartType.TOP,
                                                                                                     Chart.GLOBAL_GENRE,
                                                                                                     ChartCategory.MUSIC,
                                                                                                     Optional.of(TOP_FIFTY))));
    }

    @Test
    public void deeplink_shouldOpenWebViewWithStateParameter() throws Exception {
        Uri fakeUri = Uri.parse("http://foo.com");
        when(signInOperations.generateRemoteSignInUri("/activate/something")).thenReturn(fakeUri);
        String target = "http://soundcloud.com/activate/something";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createRemoteSignInIntent(context, fakeUri));
    }

    @Test
    public void deeplink_shouldOpenWebViewWithoutStateParameter() throws Exception {
        Uri fakeUri = Uri.parse("http://foo.com");
        when(signInOperations.generateRemoteSignInUri()).thenReturn(fakeUri);
        String target = "soundcloud://remote-sign-in";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createRemoteSignInIntent(context, fakeUri));
    }

    @Test
    public void deeplink_shouldOpenExternEmailUri() throws Exception {
        String target = "mailto:test@abc.com";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createEmailIntent("test@abc.com"));
    }

    @Test
    public void deeplink_shouldOpenExternEmail() throws Exception {
        String target = "test@abc.com";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, createEmailIntent("test@abc.com"));
    }

    @Test
    public void deeplink_shouldOpenExternWeb() throws Exception {
        String target = "https://www.google.com";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, IntentFactory.createViewIntent(Uri.parse(target)));
    }

    @Test
    public void deeplink_shouldShowToastWhenNotResolvable() throws Exception {
        String target = "https://www.google.com";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, IntentFactory.createViewIntent(Uri.parse(target)));
    }

    @Test
    public void deeplink_shouldOpenStationArtist() throws Exception {
        String target = "soundcloud://stations/artist/123";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, IntentFactory.createStationsInfoIntent(context, Urn.forArtistStation(123L), Optional.absent(), Optional.of(DiscoverySource.DEEPLINK)));

        verifyZeroInteractions(resolveOperations);
    }

    @Test
    public void deeplink_shouldOpenStationTrack() throws Exception {
        String target = "soundcloud://stations/track/123";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, IntentFactory.createStationsInfoIntent(context, Urn.forTrackStation(123L), Optional.absent(), Optional.of(DiscoverySource.DEEPLINK)));

        verifyZeroInteractions(resolveOperations);
    }

    @Test
    public void deeplink_shouldGoToTheUpload() throws Exception {
        String target = "soundcloud://the-upload";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, IntentFactory.createNewForYouIntent(context));

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
        verifyZeroInteractions(resolveOperations);
    }

    @Test
    public void deeplink_shouldOpenStationArtistLocally() throws Exception {
        String target = "soundcloud:artist-stations:123";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, IntentFactory.createStationsInfoIntent(context, Urn.forArtistStation(123L), Optional.absent(), Optional.of(DiscoverySource.DEEPLINK)));

        verifyZeroInteractions(resolveOperations);
    }

    @Test
    public void deeplink_shouldOpenStationTrackLocally() throws Exception {
        String target = "soundcloud:track-stations:123";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        assertTarget(navigationTarget, IntentFactory.createStationsInfoIntent(context, Urn.forTrackStation(123L), Optional.absent(), Optional.of(DiscoverySource.DEEPLINK)));

        verifyZeroInteractions(resolveOperations);
    }


    // For Navigation

    @Test
    public void navigation_shouldLaunchSoundStreamIfUriBlank() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(true);
        NavigationTarget navigationTarget = getTargetForNavigation("");

        assertTarget(navigationTarget, createStreamIntent(NAVIGATION_SCREEN));
    }

    @Test
    public void navigation_shouldLaunchSoundStreamIfUriBlank_newHome() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(false);
        NavigationTarget navigationTarget = getTargetForNavigation("");

        assertTarget(navigationTarget, createDiscoveryIntent(NAVIGATION_SCREEN));
    }

    @Test
    public void navigation_shouldLaunchSoundStreamIfUriNull() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(true);
        NavigationTarget navigationTarget = getTargetForNavigation(null);

        assertTarget(navigationTarget, createStreamIntent(NAVIGATION_SCREEN));
    }

    @Test
    public void navigation_shouldLaunchSoundStreamIfUriNull_newHome() throws Exception {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(false);
        NavigationTarget navigationTarget = getTargetForNavigation(null);

        assertTarget(navigationTarget, createDiscoveryIntent(NAVIGATION_SCREEN));
    }

    @Test
    public void navigation_shouldConvertOpaqueUriToHierarchicalAndLaunchPlayer() throws Exception {
        String target = "soundcloud:tracks:123";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verify(resolveOperations, never()).resolve(anyString());
        verify(playbackInitiator).startPlayback(Urn.forTrack(123), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldResolveLocallyAndLaunchPlayer() throws Exception {
        NavigationTarget navigationTarget = getTargetForNavigation("soundcloud://tracks:123");
        resolveTarget(navigationTarget);
        verify(resolveOperations, never()).resolve(anyString());
        verify(playbackInitiator).startPlayback(Urn.forTrack(123), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldConvertOpaqueUriWithAdjustReftagQueryParamToHierarchicalAndLaunchPlayer() throws Exception {
        String target = "soundcloud:tracks:123?adjust_reftag=c6vlQZj4w9FOi";
        NavigationTarget navigationTarget = getTargetForNavigation(target);
        when(resolveOperations.resolve(target)).thenReturn(Single.just(RESULT_TRACK));
        resolveTarget(navigationTarget);

        verify(resolveOperations).resolve(target);
        verify(playbackInitiator).startPlayback(Urn.forTrack(123), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldLaunchPlayerDirectlyFromHierarchicalUriSplitByColon() throws CreateModelException {
        String target = "soundcloud://sounds:123";
        NavigationTarget navigationTarget = getTargetForNavigation(target);
        when(resolveOperations.resolve(target)).thenReturn(Single.just(RESULT_TRACK));
        resolveTarget(navigationTarget);
        verify(resolveOperations).resolve(target);
        verify(playbackInitiator).startPlayback(Urn.forTrack(123), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldLaunchPlayerDirectlyFromHierarchicalUriSplitBySlash() throws CreateModelException {
        String target = "soundcloud://tracks/123";
        NavigationTarget navigationTarget = getTargetForNavigation(target);
        resolveTarget(navigationTarget);
        verify(resolveOperations, never()).resolve(anyString());
        verify(playbackInitiator).startPlayback(Urn.forTrack(123), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldShowTheStreamWithAnExpandedPlayer() throws CreateModelException {
        String target = "soundcloud://sounds:123";
        when(resolveOperations.resolve(target)).thenReturn(Single.just(RESULT_TRACK));
        NavigationTarget navigationTarget = getTargetForNavigation(target);
        PlaybackResult playbackResult = PlaybackResult.success();
        when(playbackInitiator.startPlayback(Urn.forTrack(123), NAVIGATION_SCREEN))
                .thenReturn(Single.just(playbackResult));

        resolveTarget(navigationTarget);

        verify(resolveOperations).resolve(target);
        verify(playbackInitiator).startPlayback(Urn.forTrack(123), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldNotOpenStreamWhenFailedToStartPlayback() throws CreateModelException {
        String target = "soundcloud://sounds:123";
        when(resolveOperations.resolve(target)).thenReturn(Single.just(RESULT_TRACK));
        NavigationTarget navigationTarget = getTargetForNavigation(target);
        PlaybackResult playbackResult = PlaybackResult.error(TRACK_UNAVAILABLE_OFFLINE);
        when(playbackInitiator.startPlayback(Urn.forTrack(123), NAVIGATION_SCREEN))
                .thenReturn(Single.just(playbackResult));

        resolveTarget(navigationTarget);

        verify(resolveOperations).resolve(target);
        verify(playbackInitiator).startPlayback(Urn.forTrack(123), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldConvertOpaqueUriToHierarchicalAndLaunchPlaylistDetails() throws Exception {
        String target = "soundcloud:playlists:123";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, createPlaylistIntent(Urn.forPlaylist(123), NAVIGATION_SCREEN, false));

        verify(resolveOperations, never()).resolve(anyString());
    }

    @Test
    public void navigation_shouldLaunchPlaylistDetailsDirectlyFromHierarchicalUriSplitByColon() throws CreateModelException {
        String target = "soundcloud://playlists:123";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, createPlaylistIntent(Urn.forPlaylist(123), NAVIGATION_SCREEN, false));

        verify(resolveOperations, never()).resolve(anyString());
    }

    @Test
    public void navigation_shouldConvertOpaqueUriToHierarchicalAndLaunchUserProfile() throws Exception {
        String target = "soundcloud:users:123";
        Urn urn = Urn.forUser(123);
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, IntentFactory.createProfileIntent(context, urn, Optional.of(NAVIGATION_SCREEN), Optional.absent(), Optional.absent()));

        verify(resolveOperations, never()).resolve(anyString());
        verify(eventTracker, never()).trackNavigation(any(UIEvent.class));
        verify(eventBus, never()).publish(eq(EventQueue.TRACKING), any(ForegroundEvent.class));
    }

    @Test
    public void navigation_shouldLaunchUserProfileDirectlyFromHierarchicalUriSplitByColon() throws CreateModelException {
        String target = "soundcloud://users:123";
        Urn urn = Urn.forUser(123);
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, IntentFactory.createProfileIntent(context, urn, Optional.of(NAVIGATION_SCREEN), Optional.absent(), Optional.absent()));

        verify(resolveOperations, never()).resolve(anyString());
        verify(eventTracker, never()).trackNavigation(any(UIEvent.class));
        verify(eventBus, never()).publish(eq(EventQueue.TRACKING), any(ForegroundEvent.class));
    }

    @Test
    public void navigation_shouldLaunchOnboardingWithExtraUrnForLoggedOutUsers() throws Exception {
        String target = "soundcloud://sounds:123";
        NavigationTarget navigationTarget = getTargetForNavigation(target);
        when(resolveOperations.resolve(target)).thenReturn(Single.just(RESULT_TRACK));
        when(accountOperations.isUserLoggedIn()).thenReturn(false);

        assertTarget(navigationTarget, createOnboardingIntent(context, NAVIGATION_SCREEN, Uri.parse(target)));
    }

    @Test
    public void navigation_shouldLaunchSearchForWebScheme() throws Exception {
        String target = "https://soundcloud.com/search?q=skrillex";
        NavigationTarget navigationTarget = getTargetForNavigation(target);


        assertTarget(navigationTarget, IntentFactory.createSearchActionIntent(context, Uri.parse(target), NAVIGATION_SCREEN));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldLaunchSearchForSoundCloudScheme() throws Exception {
        String target = "soundcloud://search?q=skrillex";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, IntentFactory.createSearchActionIntent(context, Uri.parse(target), NAVIGATION_SCREEN));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldNotLaunchSearchForLoggedOutUsers() throws Exception {
        String target = "soundcloud://search?q=skrillex";
        NavigationTarget navigationTarget = getTargetForNavigation(target);
        when(accountOperations.isUserLoggedIn()).thenReturn(false);

        assertTarget(navigationTarget, createOnboardingIntent(context, NAVIGATION_SCREEN, Uri.parse(target)));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldLaunchRecordForWebScheme() throws Exception {
        shadowOf(activity().getApplication()).grantPermissions(Manifest.permission.RECORD_AUDIO);

        String target = "https://soundcloud.com/upload";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, IntentFactory.createRecordIntent(context, Optional.absent(), NAVIGATION_SCREEN));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldLaunchRecordPermissionsForWebSchemeWhenNotGranted() throws Exception {
        String target = "https://soundcloud.com/upload";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, IntentFactory.createRecordPermissionIntent(context, Optional.absent(), NAVIGATION_SCREEN));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldLaunchRecordForSoundCloudSchemeWithUpload() throws Exception {
        shadowOf(activity().getApplication()).grantPermissions(Manifest.permission.RECORD_AUDIO);

        String target = "soundcloud://upload";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, IntentFactory.createRecordIntent(context, Optional.absent(), NAVIGATION_SCREEN));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldLaunchRecordPermissionsForSoundCloudSchemeWithUploadWhenNotGranted() throws Exception {
        String target = "soundcloud://upload";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, IntentFactory.createRecordPermissionIntent(context, Optional.absent(), NAVIGATION_SCREEN));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldLaunchRecordForSoundCloudSchemeWithRecord() throws Exception {
        shadowOf(activity().getApplication()).grantPermissions(Manifest.permission.RECORD_AUDIO);

        String target = "soundcloud://record";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, IntentFactory.createRecordIntent(context, Optional.absent(), NAVIGATION_SCREEN));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldLaunchRecordPermissionsForSoundCloudSchemeWithRecordWhenNotGranted() throws Exception {
        String target = "soundcloud://record";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, IntentFactory.createRecordPermissionIntent(context, Optional.absent(), NAVIGATION_SCREEN));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldNotLaunchRecordForLoggedOutUsers() throws Exception {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        String target = "soundcloud://record";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, createOnboardingIntent(context, NAVIGATION_SCREEN, Uri.parse(target)));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldLaunchUpgradeForSoundCloudScheme() throws Exception {
        when(featureOperations.upsellHighTier()).thenReturn(true);
        String target = "soundcloud://soundcloudgo";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, IntentFactory.createConversionIntent(context, UpsellContext.DEFAULT), Collections.singletonList(createHomeIntent(context)));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldNotLaunchUpgradeWhenUpsellFeatureIsDisabled() throws Exception {
        when(featureOperations.upsellHighTier()).thenReturn(false);
        String target = "soundcloud://soundcloudgo";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, createStreamIntent(NAVIGATION_SCREEN));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldNotLaunchMidTierCheckoutIfUserAlreadyHasAHighTierPlan() throws Exception {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.HIGH_TIER);
        String target = "soundcloud://buysoundcloudgo";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, createStreamIntent(NAVIGATION_SCREEN), context().getString(R.string.product_choice_error_already_subscribed));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldNotLaunchMidTierCheckoutIfUserAlreadyHasAMidTierPlan() throws Exception {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.MID_TIER);
        String target = "soundcloud://buysoundcloudgo";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, createStreamIntent(NAVIGATION_SCREEN), context().getString(R.string.product_choice_error_already_subscribed));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldLaunchMidTierCheckoutForFreeUserWithSoundCloudScheme() throws Exception {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellBothTiers()).thenReturn(true);
        String target = "soundcloud://buysoundcloudgo";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, createDirectCheckoutIntent(context, Plan.MID_TIER));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldNotLaunchMidTierCheckout() throws Exception {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellBothTiers()).thenReturn(false);
        String target = "soundcloud://buysoundcloudgo";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, createStreamIntent(NAVIGATION_SCREEN));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldNotLaunchHighTierCheckoutIfUserAlreadyHasAHighTierPlan() throws Exception {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.HIGH_TIER);
        String target = "soundcloud://buysoundcloudgoplus";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, createStreamIntent(NAVIGATION_SCREEN), context().getString(R.string.product_choice_error_already_subscribed));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldLaunchHighTierCheckoutForMidTierUserWithSoundCloudScheme() throws Exception {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.MID_TIER);
        when(featureOperations.upsellHighTier()).thenReturn(true);
        String target = "soundcloud://buysoundcloudgoplus";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, createDirectCheckoutIntent(context, Plan.HIGH_TIER));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldLaunchHighTierCheckoutForFreeUserWithSoundCloudScheme() throws Exception {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellHighTier()).thenReturn(true);
        String target = "soundcloud://buysoundcloudgoplus";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, createDirectCheckoutIntent(context, Plan.HIGH_TIER));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldNotLaunchHighTierCheckout() throws Exception {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellHighTier()).thenReturn(false);
        String target = "soundcloud://buysoundcloudgoplus";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, createStreamIntent(NAVIGATION_SCREEN));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldNotLaunchProductChoiceIfUserAlreadyHasAGoPlan() throws Exception {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.HIGH_TIER);
        String target = "soundcloud://soundcloudgo/soundcloudgo";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, createStreamIntent(NAVIGATION_SCREEN), context().getString(R.string.product_choice_error_already_subscribed));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldLaunchProductChoiceForMidTierUpsell() throws Exception {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellBothTiers()).thenReturn(true);
        String target = "soundcloud://soundcloudgo/soundcloudgo";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, createProductChoiceIntent(context, Plan.MID_TIER), Collections.singletonList(createHomeIntent(context)));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldLaunchProductChoiceForHighTierUpsell() throws Exception {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellBothTiers()).thenReturn(true);
        String target = "soundcloud://soundcloudgo/soundcloudgoplus";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, createProductChoiceIntent(context, Plan.HIGH_TIER), Collections.singletonList(createHomeIntent(context)));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldNotLaunchProductChoice() throws Exception {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellBothTiers()).thenReturn(false);
        String target = "soundcloud://soundcloudgo/soundcloudgo";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, createStreamIntent(NAVIGATION_SCREEN));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldLaunchOfflineSettingsForSoundCloudSchemeWhenOnboardingNotSeenBefore() throws Exception {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(offlineSettingsStorage.hasSeenOfflineSettingsOnboarding()).thenReturn(false);
        String target = "soundcloud://settings_offlinelistening";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, IntentFactory.createOfflineSettingsIntent(context));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldLaunchOfflineSettingsForSoundCloudSchemeWhenOnboardingSeenBefore() throws Exception {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(offlineSettingsStorage.hasSeenOfflineSettingsOnboarding()).thenReturn(true);
        String target = "soundcloud://settings_offlinelistening";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, IntentFactory.createOfflineSettingsIntent(context));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldNotLaunchOfflineSettingsWhenOfflineContentIsNotEnabled() throws Exception {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        String target = "soundcloud://settings_offlinelistening";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, createStreamIntent(NAVIGATION_SCREEN));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldLaunchNotificationPreferences() throws Exception {
        String target = "soundcloud://notification_preferences";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, createNotificationPreferencesFromDeeplinkIntent(context));

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void navigation_shouldLaunchCollection() throws Exception {
        String target = "soundcloud://collection";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, createCollectionIntent());
    }

    @Test
    public void navigation_shouldOpenDiscoverFromWebLink() throws Exception {
        String target = "https://soundcloud.com/discover";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, createDiscoveryIntent(Screen.DISCOVER));
    }

    @Test
    public void navigation_shouldOpenChartsFromWebLink() throws Exception {
        String target = "https://soundcloud.com/charts/top?genre=all";
        when(chartsUriResolver.resolveUri(Uri.parse(target))).thenReturn(ChartDetails.create(ChartType.TOP, Chart.GLOBAL_GENRE, ChartCategory.MUSIC, Optional.of(TOP_FIFTY)));
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, IntentFactory.createChartsIntent(context, ChartDetails.create(ChartType.TOP,
                                                                                                     Chart.GLOBAL_GENRE,
                                                                                                     ChartCategory.MUSIC,
                                                                                                     Optional.of(TOP_FIFTY))));
    }

    @Test
    public void navigation_shouldOpenChartsFromUri() throws Exception {
        String target = "soundcloud://charts:top:all";
        when(chartsUriResolver.resolveUri(Uri.parse(target))).thenReturn(ChartDetails.create(ChartType.TOP, Chart.GLOBAL_GENRE, ChartCategory.MUSIC, Optional.of(TOP_FIFTY)));
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, IntentFactory.createChartsIntent(context, ChartDetails.create(ChartType.TOP,
                                                                                                     Chart.GLOBAL_GENRE,
                                                                                                     ChartCategory.MUSIC,
                                                                                                     Optional.of(TOP_FIFTY))));
    }

    @Test
    public void navigation_shouldOpenWebViewWithStateParameter() throws Exception {
        Uri fakeUri = Uri.parse("http://foo.com");
        when(signInOperations.generateRemoteSignInUri("/activate/something")).thenReturn(fakeUri);
        String target = "http://soundcloud.com/activate/something";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, createRemoteSignInIntent(context, fakeUri));
    }

    @Test
    public void navigation_shouldOpenWebViewWithoutStateParameter() throws Exception {
        Uri fakeUri = Uri.parse("http://foo.com");
        when(signInOperations.generateRemoteSignInUri()).thenReturn(fakeUri);
        String target = "soundcloud://remote-sign-in";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, createRemoteSignInIntent(context, fakeUri));
    }

    @Test
    public void navigation_shouldOpenExternalEmailUri() throws Exception {
        String target = "mailto:test@abc.com";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, createEmailIntent("test@abc.com"));
    }

    @Test
    public void navigation_shouldOpenExternalEmail() throws Exception {
        String target = "test@abc.com";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, createEmailIntent("test@abc.com"));
    }

    @Test
    public void navigation_shouldOpenExternWeb() throws Exception {
        String target = "https://www.google.com";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, IntentFactory.createViewIntent(Uri.parse(target)));
    }

    @Test
    public void navigation_shouldOpenStationArtist() throws Exception {
        String target = "soundcloud://stations/artist/123";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, IntentFactory.createStationsInfoIntent(context, Urn.forArtistStation(123L), Optional.absent(), Optional.of(DiscoverySource.RECOMMENDATIONS)));

        verifyZeroInteractions(resolveOperations);
    }

    @Test
    public void navigation_shouldOpenStationTrack() throws Exception {
        String target = "soundcloud://stations/track/123";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, IntentFactory.createStationsInfoIntent(context, Urn.forTrackStation(123L), Optional.absent(), Optional.of(DiscoverySource.RECOMMENDATIONS)));

        verifyZeroInteractions(resolveOperations);
    }

    @Test
    public void navigation_shouldOpenStationArtistLocally() throws Exception {
        String target = "soundcloud:artist-stations:123";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, IntentFactory.createStationsInfoIntent(context, Urn.forArtistStation(123L), Optional.absent(), Optional.of(DiscoverySource.RECOMMENDATIONS)));

        verifyZeroInteractions(resolveOperations);
    }

    @Test
    public void navigation_shouldOpenStationTrackLocally() throws Exception {
        String target = "soundcloud:track-stations:123";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, IntentFactory.createStationsInfoIntent(context, Urn.forTrackStation(123L), Optional.absent(), Optional.of(DiscoverySource.RECOMMENDATIONS)));

        verifyZeroInteractions(resolveOperations);
    }

    @Test
    public void navigation_shouldGoToTheUpload() throws Exception {
        String target = "soundcloud://the-upload";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        assertTarget(navigationTarget, IntentFactory.createNewForYouIntent(context));

        verifyZeroInteractions(resolveOperations);
    }

    @Test
    public void navigation_shouldOpensSystemPlaylist() {
        String target = "soundcloud:system-playlists:123";
        Urn urn = new Urn(target);
        NavigationTarget navigationTarget = NavigationTarget.forNavigation(target, Optional.absent(), Screen.DEEPLINK, Optional.of(DiscoverySource.RECOMMENDATIONS));

        assertTarget(navigationTarget, IntentFactory.createSystemPlaylistIntent(context, urn, Screen.DEEPLINK));

        verify(resolveOperations, never()).resolve(anyString());
    }

    @Test
    public void navigation_shouldOpenLegacyPlaylistWithoutSearchQuerySourceInfo() {
        Urn playlistUrn = Urn.forPlaylist(123L);
        NavigationTarget navigationTarget = NavigationTarget.forLegacyPlaylist(playlistUrn, Screen.SEARCH_PLAYLISTS);

        assertTarget(navigationTarget, createPlaylistIntent(playlistUrn, Screen.SEARCH_PLAYLISTS, false));
    }

    @Test
    public void navigation_shouldOpenLegacyPlaylistWithSearchQuerySourceInfo() {
        PlaylistItem playlist = PlayableFixtures.expectedPromotedPlaylist();
        Urn playlistUrn = playlist.getUrn();

        PromotedSourceInfo promotedInfo = PromotedSourceInfo.fromItem(playlist);
        SearchQuerySourceInfo queryInfo = new SearchQuerySourceInfo(playlistUrn, "query");
        NavigationTarget navigationTarget = NavigationTarget.forLegacyPlaylist(playlistUrn, Screen.SEARCH_PLAYLISTS, Optional.of(queryInfo), Optional.of(promotedInfo));
        assertTarget(navigationTarget, createPlaylistIntent(playlistUrn, Screen.SEARCH_PLAYLISTS, Optional.of(queryInfo), Optional.of(promotedInfo)));
    }

    @Test
    public void navigation_shouldOpensPlaylist() {
        PlaylistItem playlist = PlayableFixtures.expectedPromotedPlaylist();
        Urn playlistUrn = playlist.getUrn();
        UIEvent event = mock(UIEvent.class);

        PromotedSourceInfo promotedInfo = PromotedSourceInfo.fromItem(playlist);
        SearchQuerySourceInfo queryInfo = new SearchQuerySourceInfo(playlistUrn, "query");
        NavigationTarget navigationTarget = NavigationTarget.forPlaylist(playlistUrn, Screen.SEARCH_PLAYLISTS, Optional.of(queryInfo), Optional.of(promotedInfo), Optional.of(event));
        assertTarget(navigationTarget, createPlaylistIntent(playlistUrn, Screen.SEARCH_PLAYLISTS, Optional.of(queryInfo), Optional.of(promotedInfo)));

        verify(eventTracker).trackNavigation(event);
    }

    @Test
    public void navigation_shouldOpenExternalApp() {
        String packageName = "com.soundcloud.android";
        NavigationTarget navigationTarget = NavigationTarget.forExternalPackage(packageName);
        assertTarget(navigationTarget, createExternalAppIntent(context, packageName));
    }

    // For Deeplink Navigation

    @Test
    public void navigationDeeplink_shouldOpenBasicSettings() throws Exception {
        NavigationTarget navigationTarget = NavigationTarget.forBasicSettings();

        assertTarget(navigationTarget, IntentFactory.createSettingsIntent(context));

        verify(eventTracker, never()).trackNavigation(any(UIEvent.class));
    }

    @Test
    public void navigationDeeplink_shouldOpenProfile() throws Exception {
        UIEvent event = mock(UIEvent.class);
        Urn urn = Urn.forUser(123L);
        NavigationTarget navigationTarget = NavigationTarget.forProfile(urn, Optional.of(event), Optional.of(Screen.USER_FOLLOWERS), Optional.absent());

        assertTarget(navigationTarget, IntentFactory.createProfileIntent(context, urn, Optional.of(Screen.USER_FOLLOWERS), Optional.absent(), Optional.absent()));

        verify(eventTracker).trackNavigation(event);
        verify(eventBus, never()).publish(eq(EventQueue.TRACKING), any(ForegroundEvent.class));
    }

    @Test
    public void navigationDeeplink_shouldOpenProfileReposts() throws Exception {
        Optional<SearchQuerySourceInfo> searchQuerySourceInfo = Optional.of(mock(SearchQuerySourceInfo.class));
        Urn urn = Urn.forUser(123L);
        NavigationTarget navigationTarget = NavigationTarget.forProfileReposts(urn, searchQuerySourceInfo);

        assertTarget(navigationTarget, IntentFactory.createProfileRepostsIntent(context, urn, Screen.USERS_REPOSTS, searchQuerySourceInfo));

        verify(eventBus, never()).publish(eq(EventQueue.TRACKING), any(ForegroundEvent.class));
    }

    @Test
    public void navigationDeeplink_shouldOpenProfileTracks() throws Exception {
        Optional<SearchQuerySourceInfo> searchQuerySourceInfo = Optional.of(mock(SearchQuerySourceInfo.class));
        Urn urn = Urn.forUser(123L);
        NavigationTarget navigationTarget = NavigationTarget.forProfileTracks(urn, searchQuerySourceInfo);

        assertTarget(navigationTarget, IntentFactory.createProfileTracksIntent(context, urn, Screen.USER_TRACKS, searchQuerySourceInfo));

        verify(eventBus, never()).publish(eq(EventQueue.TRACKING), any(ForegroundEvent.class));
    }

    @Test
    public void navigationDeeplink_shouldOpenProfileAlbums() throws Exception {
        Optional<SearchQuerySourceInfo> searchQuerySourceInfo = Optional.of(mock(SearchQuerySourceInfo.class));
        Urn urn = Urn.forUser(123L);
        NavigationTarget navigationTarget = NavigationTarget.forProfileAlbums(urn, searchQuerySourceInfo);

        assertTarget(navigationTarget, IntentFactory.createProfileAlbumsIntent(context, urn, Screen.USER_ALBUMS, searchQuerySourceInfo));

        verify(eventBus, never()).publish(eq(EventQueue.TRACKING), any(ForegroundEvent.class));
    }

    @Test
    public void navigationDeeplink_shouldOpenProfileLikes() throws Exception {
        Optional<SearchQuerySourceInfo> searchQuerySourceInfo = Optional.of(mock(SearchQuerySourceInfo.class));
        Urn urn = Urn.forUser(123L);
        NavigationTarget navigationTarget = NavigationTarget.forProfileLikes(urn, searchQuerySourceInfo);

        assertTarget(navigationTarget, IntentFactory.createProfileLikesIntent(context, urn, Screen.USER_LIKES, searchQuerySourceInfo));

        verify(eventBus, never()).publish(eq(EventQueue.TRACKING), any(ForegroundEvent.class));
    }

    @Test
    public void navigationDeeplink_shouldOpenProfilePlaylists() throws Exception {
        Optional<SearchQuerySourceInfo> searchQuerySourceInfo = Optional.of(mock(SearchQuerySourceInfo.class));
        Urn urn = Urn.forUser(123L);
        NavigationTarget navigationTarget = NavigationTarget.forProfilePlaylists(urn, searchQuerySourceInfo);

        assertTarget(navigationTarget, IntentFactory.createProfilePlaylistsIntent(context, urn, Screen.USER_PLAYLISTS, searchQuerySourceInfo));

        verify(eventBus, never()).publish(eq(EventQueue.TRACKING), any(ForegroundEvent.class));
    }

    @Test
    public void navigationDeeplink_shouldOpenActivities() throws Exception {
        NavigationTarget navigationTarget = NavigationTarget.forActivities();

        assertTarget(navigationTarget, IntentFactory.createActivitiesIntent(context));
    }

    @Test
    public void navigationDeeplink_shouldOpenNotificationPreferences() throws Exception {
        NavigationTarget navigationTarget = NavigationTarget.forNotificationPreferences();

        assertTarget(navigationTarget, IntentFactory.createNotificationPreferencesIntent(context));
    }

    @Test
    public void navigationDeeplink_shouldOpenHelpCenter() throws Exception {
        NavigationTarget navigationTarget = NavigationTarget.forHelpCenter();

        assertTarget(navigationTarget, IntentFactory.createHelpCenterIntent(context));
    }

    @Test
    public void navigationDeeplink_shouldOpenLegal() throws Exception {
        NavigationTarget navigationTarget = NavigationTarget.forLegal();

        assertTarget(navigationTarget, IntentFactory.createLegalIntent(context));
    }

    @Test
    public void navigationDeeplink_shouldOpenSearchTopResultsViewAllPage() throws Exception {
        NavigationTarget navigationTarget = NavigationTarget.forSearchViewAll(Optional.of(Urn.NOT_SET), "test", TopResultsBucketViewModel.Kind.GO_TRACKS, true);

        assertTarget(navigationTarget, IntentFactory.createSearchViewAllIntent(context, navigationTarget.topResultsMetaData().get(), navigationTarget.queryUrn()));
    }

    @Test
    public void navigationDeeplink_shouldOpenFollowers() throws Exception {
        NavigationTarget navigationTarget = NavigationTarget.forFollowers(Urn.forUser(123L), Optional.absent());

        assertTarget(navigationTarget, IntentFactory.createFollowersIntent(context, navigationTarget.targetUrn().get(), navigationTarget.searchQuerySourceInfo()));
    }

    @Test
    public void navigationDeeplink_shouldOpenFollowings() throws Exception {
        NavigationTarget navigationTarget = NavigationTarget.forFollowings(Urn.forUser(123L), Optional.absent());

        assertTarget(navigationTarget, IntentFactory.createFollowingsIntent(context, navigationTarget.targetUrn().get(), navigationTarget.searchQuerySourceInfo()));
    }

    @Test
    public void navigationDeeplink_shouldOpenSearchAutocomplete() throws Exception {
        NavigationTarget navigationTarget = NavigationTarget.forSearchAutocomplete(Screen.DISCOVER);

        assertTarget(navigationTarget, IntentFactory.createSearchIntent(context));
    }

    @Test
    public void navigationDeeplink_shouldOpenFullscreenVideoAd() throws Exception {
        NavigationTarget navigationTarget = NavigationTarget.forFullscreenVideoAd(Urn.forAd("dfp", "video"));

        assertTarget(navigationTarget, IntentFactory.createFullscreenVideoAdIntent(context, navigationTarget.targetUrn().get()));
    }

    @Test
    public void navigationDeeplink_shouldOpenPrestitialAd() throws Exception {
        NavigationTarget navigationTarget = NavigationTarget.forPrestitialAd();

        assertTarget(navigationTarget, IntentFactory.createPrestititalAdIntent(context));
    }

    @Test
    public void navigationDeeplink_shouldOpenClickthroughAd() throws Exception {
        String target = "https://ferrari.com/";
        NavigationTarget navigationTarget = NavigationTarget.forAdClickthrough(target);

        assertTarget(navigationTarget, IntentFactory.createAdClickthroughIntent(Uri.parse(target)));
    }

    @Test
    public void opensChartTracks() {
        Urn genreUrn = new Urn("soundcloud:genre:123");
        ChartType chartType = ChartType.TOP;
        String header = "header";
        ChartCategory chartCategory = ChartCategory.AUDIO;
        ChartDetails chartDetails = ChartDetails.create(chartType, genreUrn, chartCategory, Optional.of(header));
        NavigationTarget navigationTarget = NavigationTarget.forChart(chartType, genreUrn, chartCategory, header);

        assertTarget(navigationTarget, IntentFactory.createChartsIntent(context, chartDetails));
    }

    @Test
    public void opensAllGenres() {
        NavigationTarget navigationTarget = NavigationTarget.forAllGenres();

        assertTarget(navigationTarget, IntentFactory.createAllGenresIntent(context, null));
    }

    @Test
    public void opensAllGenresFromDeeplink() throws Exception {
        ChartCategory chartCategory = ChartCategory.MUSIC;
        NavigationTarget navigationTarget = NavigationTarget.forAllGenres(chartCategory);

        assertTarget(navigationTarget, IntentFactory.createAllGenresIntent(context, chartCategory));
    }

    @Test
    public void navigationDeeplink_shouldOpenPlaylistsAndAlbums() throws Exception {
        NavigationTarget navigationTarget = NavigationTarget.forPlaylistsAndAlbumsCollection();

        assertTarget(navigationTarget, IntentFactory.createPlaylistsAndAlbumsCollectionIntent(context));
    }

    @Test
    public void navigationDeeplink_shouldOpenStationInfo() {
        Urn someStation = Urn.forArtistStation(123L);
        NavigationTarget navigationTarget = NavigationTarget.forStationInfo(someStation, Optional.absent(), Optional.of(DiscoverySource.STATIONS), Optional.absent());

        assertTarget(navigationTarget, IntentFactory.createStationsInfoIntent(context, someStation, Optional.absent(), Optional.of(DiscoverySource.STATIONS)));
    }

    @Test
    public void navigationDeeplink_shouldOpenStationInfoWithSeedtrack() {
        Urn someStation = Urn.forArtistStation(123L);
        Urn seedTrack = Urn.forTrack(123L);
        UIEvent navigationEvent = UIEvent.fromNavigation(seedTrack, EventContextMetadata.builder().build());
        NavigationTarget navigationTarget = NavigationTarget.forStationInfo(someStation, Optional.of(seedTrack), Optional.of(DiscoverySource.STATIONS), Optional.of(navigationEvent));

        assertTarget(navigationTarget, IntentFactory.createStationsInfoIntent(context, someStation, Optional.of(seedTrack), Optional.of(DiscoverySource.STATIONS)));
        verify(eventTracker).trackNavigation(navigationEvent);
    }

    @Test
    public void navigationDeeplink_shouldOpenOfflineSettingsOnboardingWhenOnboardingRequestedAndHasNotBeenSeenBefore() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(offlineSettingsStorage.hasSeenOfflineSettingsOnboarding()).thenReturn(false);
        NavigationTarget navigationTarget = NavigationTarget.forOfflineSettings(true);

        assertTarget(navigationTarget, IntentFactory.createOfflineSettingsOnboardingIntent(context));

    }

    @Test
    public void navigationDeeplink_shouldOpenOfflineSettingsWhenOnboardingRequestedAndHasBeenSeenBefore() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(offlineSettingsStorage.hasSeenOfflineSettingsOnboarding()).thenReturn(true);
        NavigationTarget navigationTarget = NavigationTarget.forOfflineSettings(true);

        assertTarget(navigationTarget, IntentFactory.createOfflineSettingsIntent(context));

    }

    @Test
    public void navigationDeeplink_shouldOpenOfflineSettingsWhenOnboardingNotRequestedAndHasNotBeenSeenBefore() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(offlineSettingsStorage.hasSeenOfflineSettingsOnboarding()).thenReturn(false);
        NavigationTarget navigationTarget = NavigationTarget.forOfflineSettings(false);

        assertTarget(navigationTarget, IntentFactory.createOfflineSettingsIntent(context));

    }

    @Test
    public void navigationDeeplink_shouldOpenOfflineSettingsWhenOnboardingNotRequestedAndHasBeenSeenBefore() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        when(offlineSettingsStorage.hasSeenOfflineSettingsOnboarding()).thenReturn(true);
        NavigationTarget navigationTarget = NavigationTarget.forOfflineSettings(false);

        assertTarget(navigationTarget, IntentFactory.createOfflineSettingsIntent(context));

    }

    @Test
    public void navigationDeeplink_shouldNotOpenOfflineSettingsWhenOfflineContentIsNotEnabled() {
        when(homeScreenConfiguration.isStreamHome()).thenReturn(true);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        NavigationTarget navigationTarget = NavigationTarget.forOfflineSettings(false);

        assertTarget(navigationTarget, createStreamIntent(Screen.UNKNOWN));
    }

    @Test
    public void resetPasswordLinkNavigatesToNativeWebView() throws Exception {
        String resetPasswordLink = "http%3A%2F%2Fsoundcloud.com%2Flogin%2Freset%2F123456789abcdef1234567";
        final String redirectTrackedLink = "http://soundcloud.com/-/t/click/postman-email-account_lifecycle-password_reset_request?url=" + resetPasswordLink;
        when(resolveOperations.resolve(redirectTrackedLink)).thenReturn(Single.just(ResolveResult.error(Uri.parse(resetPasswordLink), null)));
        NavigationTarget navigationTarget = NavigationTarget.forExternalDeeplink(redirectTrackedLink, "");

        resolveTarget(navigationTarget);

        verify(resolveOperations).resolve(redirectTrackedLink);
        assertTarget(navigationTarget, IntentFactory.createViewIntent(Uri.parse(resetPasswordLink)));
    }

    // Fallback Errors

    @Test
    public void fallbackToChromeCustomTabsIfNoResolving() throws Exception {
        addChromeCustomTabsIntentResolverInPackageManager();

        String target = "https://soundcloud.com/jobs/";
        NavigationTarget navigationTarget = NavigationTarget.forWebView(target);

        resolveTarget(navigationTarget);

        assertTargetWithChromeCustomTabsMetadata(navigationTarget, Uri.parse(target));
        verifyZeroInteractions(resolveOperations);
    }

    @Test
    public void fallbackToNativeWebViewIfNoResolvingAndChromeIsNotInstalled() {
        String target = "https://soundcloud.com/jobs/";
        NavigationTarget navigationTarget = NavigationTarget.forWebView(target);

        resolveTarget(navigationTarget);

        assertTarget(navigationTarget, IntentFactory.createWebViewIntent(context, Uri.parse(target)));
        verifyZeroInteractions(resolveOperations);
    }

    @Test
    public void retryWithFallback() throws Exception {
        String target = "soundcloud://target";
        String fallback = "soundcloud://fallback";

        NavigationTarget navigationTarget = getTargetForNavigation(target, fallback);
        when(resolveOperations.resolve(target)).thenReturn(Single.just(ResolveResult.error(Uri.parse(target), null)));
        when(resolveOperations.resolve(fallback)).thenReturn(Single.just(ResolveResult.success(Urn.forComment(123))));

        resolveTarget(navigationTarget);

        verify(resolveOperations).resolve(target);
        verify(resolveOperations).resolve(fallback);
    }

    @Test
    public void dontRetryWithoutFallback() throws Exception {
        String target = "soundcloud://target";
        NavigationTarget navigationTarget = getTargetForNavigation(target);
        when(resolveOperations.resolve(target)).thenReturn(Single.just(ResolveResult.error(Uri.parse(target), null)));

        resolveTarget(navigationTarget);

        verify(resolveOperations).resolve(target);
        verifyNoMoreInteractions(resolveOperations);
    }

    @Test
    public void dontRetryWithSameTargetAndFallback() throws Exception {
        String target = "soundcloud://target";
        NavigationTarget navigationTarget = getTargetForNavigation(target, target);
        when(resolveOperations.resolve(target)).thenReturn(Single.just(ResolveResult.error(Uri.parse(target), null)));

        resolveTarget(navigationTarget);

        verify(resolveOperations).resolve(target);
        verifyNoMoreInteractions(resolveOperations);
    }

    @Test
    public void reportErrorForNavigation() throws Exception {
        String target = "soundcloud://target";
        NavigationTarget navigationTarget = getTargetForNavigation(target);
        when(resolveOperations.resolve(target)).thenReturn(Single.just(ResolveResult.error(Uri.parse(target), null)));

        boolean success = resolver.resolveNavigationResult(navigationTarget)
                                  .test()
                                  .values().get(0)
                                  .isSuccess();

        assertThat(success).isFalse();
    }

    @Test
    public void reportErrorForDeeplink() throws Exception {
        String target = "soundcloud://target";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);
        when(resolveOperations.resolve(target)).thenReturn(Single.just(ResolveResult.error(Uri.parse(target), null)));

        assertTargetWithToast(navigationTarget, context().getString(R.string.error_unknown_navigation));
    }

    private ForegroundEvent captureForegroundEvent() {
        ArgumentCaptor<ForegroundEvent> captor = ArgumentCaptor.forClass(ForegroundEvent.class);
        verify(eventBus).publish(eq(EventQueue.TRACKING), captor.capture());
        return captor.getValue();
    }

    private void verifyTrackingEvent(ForegroundEvent event, Urn urn, Optional<String> referrer) {
        assertThat(event.referrer()).isEqualTo(referrer.orNull());
        assertThat(event.pageName()).isEqualTo(DEEPLINK_SCREEN.get());
        assertThat(event.pageUrn().get()).isEqualTo(urn);
    }

    private void verifyTrackingEvent(Optional<String> referrer) {
        verifyTrackingEvent(referrer, DEEPLINK_SCREEN);
    }

    private void verifyTrackingEvent(Optional<String> referrer, Screen screen) {
        ForegroundEvent event = captureForegroundEvent();

        assertThat(event.referrer()).isEqualTo(referrer.orNull());
        assertThat(event.pageName()).isEqualTo(screen.get());
        assertThat(event.pageUrn().isPresent()).isFalse();
    }

    private NavigationTarget getTargetForNavigation(String target) {
        return getTargetForNavigation(target, null);
    }

    private NavigationTarget getTargetForNavigation(String target, String fallback) {
        return NavigationTarget.forNavigation(target, Optional.fromNullable(fallback), NAVIGATION_SCREEN, Optional.of(DiscoverySource.RECOMMENDATIONS));
    }

    private NavigationTarget getTargetForDeeplink(String target) {
        return getTargetForDeeplink(target, Referrer.OTHER);
    }

    private NavigationTarget getTargetForDeeplink(String target, Referrer referrer) {
        return NavigationTarget.forExternalDeeplink(target, referrer.value());
    }

    private void assertTargetWithToast(NavigationTarget navigationTarget, String toastMessage) {
        NavigationResult result = resolver.resolveNavigationResult(navigationTarget)
                                          .test()
                                          .assertNoErrors()
                                          .values().get(0);

        assertThat(result.toastMessage().get()).isEqualTo(toastMessage);
    }

    private void assertTargetWithChromeCustomTabsMetadata(NavigationTarget navigationTarget, Uri uri) {
        NavigationResult result = resolver.resolveNavigationResult(navigationTarget)
                                          .test()
                                          .assertNoErrors()
                                          .values().get(0);

        assertThat(result.customTabsMetadata().get().getUri()).isEqualTo(uri);
    }

    private void assertTarget(NavigationTarget navigationTarget, Intent expected) {
        assertTarget(navigationTarget, expected, Collections.emptyList(), Optional.absent());
    }

    private void assertTarget(NavigationTarget navigationTarget, Intent expected, String toastMessage) {
        assertTarget(navigationTarget, expected, Collections.emptyList(), Optional.of(toastMessage));
    }

    private void assertTarget(NavigationTarget navigationTarget, Intent expected, List<Intent> taskStack) {
        assertTarget(navigationTarget, expected, taskStack, Optional.absent());
    }

    private void assertTarget(NavigationTarget navigationTarget, Intent expected, List<Intent> taskStack, Optional<String> toastMessage) {
        NavigationResult result = resolver.resolveNavigationResult(navigationTarget)
                                          .test()
                                          .assertNoErrors()
                                          .values().get(0);

        new IntentAssert(result.intent().get()).isEqualToIntent(expected);

        assertThat(result.taskStack().size()).isEqualTo(taskStack.size());
        for (int i = 0; i < taskStack.size(); i++) {
            Intent taskStackExpected = taskStack.get(i);
            Intent resultIntent = result.taskStack().get(i);
            new IntentAssert(resultIntent).isEqualToIntent(taskStackExpected);
        }

        toastMessage.ifPresent(message -> assertThat(result.toastMessage().get()).isEqualTo(message));
    }

    private void resolveTarget(NavigationTarget navigationTarget) {
        resolver.resolveNavigationResult(navigationTarget)
                .test()
                .assertNoErrors();
    }

    private void addChromeCustomTabsIntentResolverInPackageManager() {
        RobolectricPackageManager packageManager = shadowOf(RuntimeEnvironment.application.getPackageManager());
        String chromePackageName = CustomTabsHelper.STABLE_PACKAGE;

        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.packageName = chromePackageName;
        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = chromePackageName;
        activityInfo.applicationInfo = applicationInfo;
        ResolveInfo info = new ResolveInfo();
        info.activityInfo = activityInfo;

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(CustomTabsHelper.URL_TO_BE_MATCHED_AGAINST_VIEW_INTENT));
        packageManager.addResolveInfoForIntent(intent, info);

        Intent serviceIntent = new Intent();
        serviceIntent.setAction(CustomTabsHelper.ACTION_CUSTOM_TABS_CONNECTION);
        serviceIntent.setPackage(chromePackageName);
        packageManager.addResolveInfoForIntent(serviceIntent, info);
    }
}
