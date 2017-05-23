package com.soundcloud.android.deeplinks;

import static com.soundcloud.android.playback.PlaybackResult.ErrorReason.TRACK_UNAVAILABLE_OFFLINE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.PlaybackServiceController;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.events.DeeplinkReportEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.main.NavigationResult;
import com.soundcloud.android.main.NavigationTarget;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.olddiscovery.charts.Chart;
import com.soundcloud.android.onboarding.auth.SignInOperations;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.payments.UpsellContext;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.rx.observers.DefaultSingleObserver;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import com.tobedevoured.modelcitizen.CreateModelException;
import io.reactivex.Single;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.shadows.ShadowToast;

import android.content.res.Resources;
import android.net.Uri;

public class IntentResolverTest extends AndroidUnitTest {
    private static final String TOP_FIFTY = "Top 50";
    private static final ResolveResult RESULT_TRACK = ResolveResult.succes(Urn.forTrack(123));
    private static final Screen NAVIGATION_SCREEN = Screen.DISCOVER;
    private static final Screen DEEPLINK_SCREEN = Screen.DEEPLINK;

    private LocalEntityUriResolver localEntityUriResolver = new LocalEntityUriResolver();
    @Mock private ResolveOperations resolveOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private EventBus eventBus;
    @Mock private Navigator navigator;
    @Mock private FeatureOperations featureOperations;
    @Mock private Resources resources;
    @Mock private ChartsUriResolver chartsUriResolver;
    @Mock private SignInOperations signInOperations;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaybackServiceController playbackServiceController;
    @Mock private ExpandPlayerSubscriber expandPlayerSubscriber;

    private IntentResolver resolver;


    @Before
    public void setUp() {
        resolver = new IntentResolver(resolveOperations,
                                      localEntityUriResolver,
                                      accountOperations,
                                      playbackServiceController,
                                      playbackInitiator,
                                      playQueueManager,
                                      eventBus,
                                      navigator,
                                      featureOperations,
                                      chartsUriResolver,
                                      signInOperations,
                                      () -> expandPlayerSubscriber);

        when(accountOperations.isUserLoggedIn()).thenReturn(true);
        when(playbackInitiator.startPlayback(any(Urn.class),
                                             any(Screen.class))).thenReturn(rx.Observable.empty());
        when(resources.getString(R.string.charts_top)).thenReturn(TOP_FIFTY);
    }

    // For Deeplink

    @Test
    public void deeplink_shouldLaunchSoundStreamIfUriBlank() {
        NavigationTarget navigationTarget = getTargetForDeeplink("");

        resolveTarget(navigationTarget);

        verify(navigator).openStream(navigationTarget.activity(), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldLaunchSoundStreamIfUriNull() {
        NavigationTarget navigationTarget = getTargetForDeeplink(null);

        resolveTarget(navigationTarget);

        verify(navigator).openStream(navigationTarget.activity(), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldConvertOpaqueUriToHierarchicalAndLaunchPlayer() {
        String target = "soundcloud:tracks:123";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verify(resolveOperations, never()).resolve(anyString());
        verify(playbackInitiator).startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldResolveLocallyAndLaunchPlayer() {
        NavigationTarget navigationTarget = getTargetForDeeplink("soundcloud://tracks:123");

        resolveTarget(navigationTarget);

        verify(resolveOperations, never()).resolve(anyString());
        verify(playbackInitiator).startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldConvertOpaqueUriWithAdjustReftagQueryParamToHierarchicalAndLaunchPlayer() {
        String target = "soundcloud:tracks:123?adjust_reftag=c6vlQZj4w9FOi";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);
        when(resolveOperations.resolve(target)).thenReturn(Single.just(RESULT_TRACK));

        resolveTarget(navigationTarget);

        verify(resolveOperations).resolve(target);
        verify(playbackInitiator).startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldLaunchPlayerDirectlyFromHierarchicalUriSplitByColon() throws CreateModelException {
        String target = "soundcloud://sounds:123";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);
        when(resolveOperations.resolve(target)).thenReturn(Single.just(RESULT_TRACK));

        resolveTarget(navigationTarget);

        verify(resolveOperations).resolve(target);
        verify(playbackInitiator).startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldLaunchPlayerDirectlyFromHierarchicalUriSplitBySlash() throws CreateModelException {
        String target = "soundcloud://tracks/123";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verify(resolveOperations, never()).resolve(anyString());
        verify(playbackInitiator).startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldShowTheStreamWithAnExpandedPlayer() throws CreateModelException {
        String target = "soundcloud://sounds:123";
        when(resolveOperations.resolve(target)).thenReturn(Single.just(RESULT_TRACK));
        NavigationTarget navigationTarget = getTargetForDeeplink(target);
        when(playbackInitiator.startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN))
                .thenReturn(rx.Observable.just(PlaybackResult.success()));

        resolveTarget(navigationTarget);

        verify(resolveOperations).resolve(target);
        verify(playbackInitiator).startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN);
        verify(navigator).openStreamWithExpandedPlayer(navigationTarget.activity(), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldNotOpenStreamWhenFailedToStartPlayback() throws CreateModelException {
        String target = "soundcloud://sounds:123";
        when(resolveOperations.resolve(target)).thenReturn(Single.just(RESULT_TRACK));
        NavigationTarget navigationTarget = getTargetForDeeplink(target);
        when(playbackInitiator.startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN))
                .thenReturn(rx.Observable.just(PlaybackResult.error(TRACK_UNAVAILABLE_OFFLINE)));

        resolveTarget(navigationTarget);

        verify(resolveOperations).resolve(target);
        verify(playbackInitiator).startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN);
        verify(navigator, never()).openStreamWithExpandedPlayer(navigationTarget.activity(), DEEPLINK_SCREEN);
        verify(navigator).openLauncher(navigationTarget.activity());
    }

    @Test
    public void deeplink_shouldConvertOpaqueUriToHierarchicalAndLaunchPlaylistDetails() {
        String target = "soundcloud:playlists:123";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verify(resolveOperations, never()).resolve(anyString());
        verify(navigator).legacyOpenPlaylist(navigationTarget.activity(), Urn.forPlaylist(123), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldLaunchPlaylistDetailsDirectlyFromHierarchicalUriSplitByColon() throws CreateModelException {
        String target = "soundcloud://playlists:123";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verify(resolveOperations, never()).resolve(anyString());
        verify(navigator).legacyOpenPlaylist(navigationTarget.activity(), Urn.forPlaylist(123), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldConvertOpaqueUriToHierarchicalAndLaunchUserProfile() {
        String target = "soundcloud:users:123";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verify(resolveOperations, never()).resolve(anyString());
        verify(navigator).legacyOpenProfile(navigationTarget.activity(), Urn.forUser(123), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldLaunchUserProfileDirectlyFromHierarchicalUriSplitByColon() throws CreateModelException {
        String target = "soundcloud://users:123";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verify(resolveOperations, never()).resolve(anyString());
        verify(navigator).legacyOpenProfile(navigationTarget.activity(), Urn.forUser(123), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldTrackForegroundEventsWithResources() {
        String target = "soundcloud://sounds:123";
        NavigationTarget navigationTarget = getTargetForDeeplink(target, Referrer.TWITTER);
        when(resolveOperations.resolve(target)).thenReturn(Single.just(RESULT_TRACK));
        when(playbackInitiator.startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN))
                .thenReturn(rx.Observable.just(PlaybackResult.success()));

        resolveTarget(navigationTarget);

        verify(resolveOperations).resolve(target);
        ArgumentCaptor<TrackingEvent> captor = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(eventBus, times(2)).publish(eq(EventQueue.TRACKING), captor.capture());

        ForegroundEvent event = (ForegroundEvent) captor.getAllValues().get(0);
        verifyTrackingEvent(event, Urn.forTrack(123), navigationTarget.referrer());
    }

    @Test
    public void deeplink_shouldTrackForegroundEventsWithResourcesOnPlaybackFailed() {
        String target = "soundcloud://sounds:123";
        NavigationTarget navigationTarget = getTargetForDeeplink(target, Referrer.TWITTER);
        when(resolveOperations.resolve(target)).thenReturn(Single.just(RESULT_TRACK));
        when(playbackInitiator.startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN))
                .thenReturn(rx.Observable.just(PlaybackResult.error(PlaybackResult.ErrorReason.TRACK_UNAVAILABLE_OFFLINE)));

        resolveTarget(navigationTarget);

        verify(resolveOperations).resolve(target);
        ArgumentCaptor<TrackingEvent> captor = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(eventBus, times(2)).publish(eq(EventQueue.TRACKING), captor.capture());

        ForegroundEvent event = (ForegroundEvent) captor.getAllValues().get(0);
        verifyTrackingEvent(event, Urn.forTrack(123), navigationTarget.referrer());
    }

    @Test
    public void deeplink_shouldTrackForegroundEventsWithResourceWhenUserIsNotLoggedIn() {
        String target = "soundcloud://sounds:123";
        NavigationTarget navigationTarget = getTargetForDeeplink(target, Referrer.FACEBOOK);
        when(resolveOperations.resolve(target)).thenReturn(Single.just(RESULT_TRACK));
        when(playbackInitiator.startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN))
                .thenReturn(rx.Observable.just(PlaybackResult.success()));
        when(accountOperations.isUserLoggedIn()).thenReturn(false);

        resolveTarget(navigationTarget);

        verify(resolveOperations).resolve(target);
        ArgumentCaptor<TrackingEvent> captor = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(eventBus, times(2)).publish(eq(EventQueue.TRACKING), captor.capture());

        ForegroundEvent event = (ForegroundEvent) captor.getAllValues().get(0);
        verifyTrackingEvent(event, Urn.forTrack(123), navigationTarget.referrer());
    }

    @Test
    public void deeplink_shouldTrackForegroundEventsWithoutResourcesForHome() {
        NavigationTarget navigationTarget = getTargetForDeeplink(null);

        resolveTarget(navigationTarget);

        verify(navigator).openStream(navigationTarget.activity(), DEEPLINK_SCREEN);
        verifyTrackingEvent(navigationTarget.referrer());
    }

    @Test
    public void deeplink_shouldLoginCrawler() {
        String target = "soundcloud://sounds:123";
        NavigationTarget navigationTarget = getTargetForDeeplink(target, Referrer.GOOGLE_CRAWLER);
        when(resolveOperations.resolve(target)).thenReturn(Single.just(RESULT_TRACK));
        when(playbackInitiator.startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN))
                .thenReturn(rx.Observable.just(PlaybackResult.success()));

        resolveTarget(navigationTarget);

        verify(resolveOperations).resolve(target);
        verify(accountOperations).loginCrawlerUser();
        verify(playbackInitiator).startPlayback(Urn.forTrack(123), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldLaunchOnboardingWithExtraUrnForLoggedOutUsers() {
        String target = "soundcloud://sounds:123";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);
        when(resolveOperations.resolve(target)).thenReturn(Single.just(RESULT_TRACK));
        when(accountOperations.isUserLoggedIn()).thenReturn(false);

        resolveTarget(navigationTarget);

        ArgumentCaptor<TrackingEvent> captor = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(eventBus, times(2)).publish(eq(EventQueue.TRACKING), captor.capture());

        ForegroundEvent event = (ForegroundEvent) captor.getAllValues().get(0);
        verifyTrackingEvent(event, Urn.forTrack(123), Optional.of(Referrer.OTHER.value()));
        verify(navigator).openOnboarding(navigationTarget.activity(), Uri.parse(target), DEEPLINK_SCREEN);

        DeeplinkReportEvent reportEvent = (DeeplinkReportEvent) captor.getAllValues().get(1);
        assertThat(reportEvent.kind()).isEqualTo(DeeplinkReportEvent.forResolvedDeeplink(Referrer.OTHER.toString()).kind());
    }

    @Test
    public void deeplink_shouldLaunchSearchForWebScheme() {
        String target = "https://soundcloud.com/search?q=skrillex";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);


        resolveTarget(navigationTarget);

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
        verify(navigator).openSearch(navigationTarget.activity(), Uri.parse(target), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldLaunchSearchForSoundCloudScheme() {
        String target = "soundcloud://search?q=skrillex";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);


        resolveTarget(navigationTarget);

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
        verify(navigator).openSearch(navigationTarget.activity(), Uri.parse(target), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldNotLaunchSearchForLoggedOutUsers() {
        String target = "soundcloud://search?q=skrillex";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);
        when(accountOperations.isUserLoggedIn()).thenReturn(false);


        resolveTarget(navigationTarget);

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
        verify(navigator).openOnboarding(navigationTarget.activity(), Uri.parse(target), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldLaunchSearchForCrawlers() {
        String target = "soundcloud://search?q=skrillex";
        NavigationTarget navigationTarget = getTargetForDeeplink(target, Referrer.GOOGLE_CRAWLER);

        resolveTarget(navigationTarget);

        verifyTrackingEvent(Optional.of(Referrer.GOOGLE_CRAWLER.value()));
        verify(accountOperations).loginCrawlerUser();
        verify(navigator).openSearch(navigationTarget.activity(), Uri.parse(target), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldLaunchRecordForWebScheme() {
        String target = "https://soundcloud.com/upload";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
        verify(navigator).openRecord(navigationTarget.activity(), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldLaunchRecordForSoundCloudSchemeWithUpload() {
        String target = "soundcloud://upload";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
        verify(navigator).openRecord(navigationTarget.activity(), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldLaunchRecordForSoundCloudSchemeWithRecord() {
        String target = "soundcloud://record";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
        verify(navigator).openRecord(navigationTarget.activity(), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldNotLaunchRecordForLoggedOutUsers() {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        String target = "soundcloud://record";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
        verify(navigator).openOnboarding(navigationTarget.activity(), Uri.parse(target), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldLaunchRecordForCrawlers() {
        String target = "soundcloud://upload";
        NavigationTarget navigationTarget = getTargetForDeeplink(target, Referrer.GOOGLE_CRAWLER);

        resolveTarget(navigationTarget);

        verify(accountOperations).loginCrawlerUser();
        verifyTrackingEvent(Optional.of(Referrer.GOOGLE_CRAWLER.value()));
        verify(navigator).openRecord(navigationTarget.activity(), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldLaunchUpgradeForSoundCloudScheme() {
        when(featureOperations.upsellHighTier()).thenReturn(true);
        String target = "soundcloud://soundcloudgo";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()), Screen.CONVERSION);
        verify(navigator).openUpgradeOnMain(navigationTarget.activity(), UpsellContext.DEFAULT);
    }

    @Test
    public void deeplink_shouldNotLaunchUpgradeWhenUpsellFeatureIsDisabled() {
        when(featureOperations.upsellHighTier()).thenReturn(false);
        String target = "soundcloud://soundcloudgo";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
        verify(navigator).openStream(navigationTarget.activity(), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldNotLaunchMidTierCheckoutIfUserAlreadyHasAHighTierPlan() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.HIGH_TIER);
        String target = "soundcloud://buysoundcloudgo";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
        verify(navigator).openStream(navigationTarget.activity(), DEEPLINK_SCREEN);
        assertThat(ShadowToast.getTextOfLatestToast())
                .isEqualTo(context().getString(R.string.product_choice_error_already_subscribed));
    }

    @Test
    public void deeplink_shouldNotLaunchMidTierCheckoutIfUserAlreadyHasAMidTierPlan() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.MID_TIER);
        String target = "soundcloud://buysoundcloudgo";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
        verify(navigator).openStream(navigationTarget.activity(), DEEPLINK_SCREEN);
        assertThat(ShadowToast.getTextOfLatestToast())
                .isEqualTo(context().getString(R.string.product_choice_error_already_subscribed));
    }

    @Test
    public void deeplink_shouldLaunchMidTierCheckoutForFreeUserWithSoundCloudScheme() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellBothTiers()).thenReturn(true);
        String target = "soundcloud://buysoundcloudgo";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()), Screen.CHECKOUT);
        verify(navigator).openDirectCheckout(navigationTarget.activity(), Plan.MID_TIER);
    }

    @Test
    public void deeplink_shouldNotLaunchMidTierCheckout() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellBothTiers()).thenReturn(false);
        String target = "soundcloud://buysoundcloudgo";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
        verify(navigator).openStream(navigationTarget.activity(), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldNotLaunchHighTierCheckoutIfUserAlreadyHasAHighTierPlan() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.HIGH_TIER);
        String target = "soundcloud://buysoundcloudgoplus";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
        verify(navigator).openStream(navigationTarget.activity(), DEEPLINK_SCREEN);
        assertThat(ShadowToast.getTextOfLatestToast())
                .isEqualTo(context().getString(R.string.product_choice_error_already_subscribed));
    }

    @Test
    public void deeplink_shouldLaunchHighTierCheckoutForMidTierUserWithSoundCloudScheme() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.MID_TIER);
        when(featureOperations.upsellHighTier()).thenReturn(true);
        String target = "soundcloud://buysoundcloudgoplus";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()), Screen.CHECKOUT);
        verify(navigator).openDirectCheckout(navigationTarget.activity(), Plan.HIGH_TIER);
    }

    @Test
    public void deeplink_shouldLaunchHighTierCheckoutForFreeUserWithSoundCloudScheme() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellHighTier()).thenReturn(true);
        String target = "soundcloud://buysoundcloudgoplus";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()), Screen.CHECKOUT);
        verify(navigator).openDirectCheckout(navigationTarget.activity(), Plan.HIGH_TIER);
    }

    @Test
    public void deeplink_shouldNotLaunchHighTierCheckout() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellHighTier()).thenReturn(false);
        String target = "soundcloud://buysoundcloudgoplus";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
        verify(navigator).openStream(navigationTarget.activity(), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldNotLaunchProductChoiceIfUserAlreadyHasAGoPlan() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.HIGH_TIER);
        String target = "soundcloud://soundcloudgo/soundcloudgo";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
        verify(navigator).openStream(navigationTarget.activity(), DEEPLINK_SCREEN);
        assertThat(ShadowToast.getTextOfLatestToast())
                .isEqualTo(context().getString(R.string.product_choice_error_already_subscribed));
    }

    @Test
    public void deeplink_shouldLaunchProductChoiceForMidTierUpsell() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellBothTiers()).thenReturn(true);
        String target = "soundcloud://soundcloudgo/soundcloudgo";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()), Screen.CONVERSION);
        verify(navigator).openProductChoiceOnMain(navigationTarget.activity(), Plan.MID_TIER);
    }

    @Test
    public void deeplink_shouldLaunchProductChoiceForHighTierUpsell() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellBothTiers()).thenReturn(true);
        String target = "soundcloud://soundcloudgo/soundcloudgoplus";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()), Screen.CONVERSION);
        verify(navigator).openProductChoiceOnMain(navigationTarget.activity(), Plan.HIGH_TIER);
    }

    @Test
    public void deeplink_shouldNotLaunchProductChoice() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellBothTiers()).thenReturn(false);
        String target = "soundcloud://soundcloudgo/soundcloudgo";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
        verify(navigator).openStream(navigationTarget.activity(), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldLaunchOfflineSettingsForSoundCloudScheme() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        String target = "soundcloud://settings_offlinelistening";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()), Screen.SETTINGS_OFFLINE);
        verify(navigator).openOfflineSettings(navigationTarget.activity());
    }

    @Test
    public void deeplink_shouldNotLaunchOfflineSettingsWhenOfflineContentIsNotEnabled() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        String target = "soundcloud://settings_offlinelistening";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
        verify(navigator).openStream(navigationTarget.activity(), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldLaunchNotificationPreferences() {
        String target = "soundcloud://notification_preferences";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
        verify(navigator).openNotificationPreferencesFromDeeplink(navigationTarget.activity());
    }

    @Test
    public void deeplink_shouldLaunchCollection() {
        String target = "soundcloud://collection";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
        verify(navigator).openCollection(navigationTarget.activity());
    }

    @Test
    public void deeplink_shouldOpenDiscoverFromWebLink() {
        String target = "https://soundcloud.com/discover";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verify(navigator).openViewAllRecommendations(navigationTarget.activity());
    }

    @Test
    public void deeplink_shouldOpenDiscoveryFromUri() {
        String target = "soundcloud://discovery";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verify(navigator).openDiscovery(navigationTarget.activity(), DEEPLINK_SCREEN);
    }

    @Test
    public void deeplink_shouldOpenChartsFromWebLink() {
        String target = "https://soundcloud.com/charts/top?genre=all";
        when(chartsUriResolver.resolveUri(Uri.parse(target))).thenReturn(ChartDetails.create(ChartType.TOP, Chart.GLOBAL_GENRE, ChartCategory.MUSIC, Optional.of(TOP_FIFTY)));
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verify(navigator).openChart(navigationTarget.activity(),
                                    Chart.GLOBAL_GENRE,
                                    ChartType.TOP,
                                    ChartCategory.MUSIC,
                                    TOP_FIFTY);
    }

    @Test
    public void deeplink_shouldOpenChartsFromUri() {
        String target = "soundcloud://charts:top:all";
        when(chartsUriResolver.resolveUri(Uri.parse(target))).thenReturn(ChartDetails.create(ChartType.TOP, Chart.GLOBAL_GENRE, ChartCategory.MUSIC, Optional.of(TOP_FIFTY)));
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verify(navigator).openChart(navigationTarget.activity(),
                                    Chart.GLOBAL_GENRE,
                                    ChartType.TOP,
                                    ChartCategory.MUSIC,
                                    TOP_FIFTY);
    }

    @Test
    public void deeplink_shouldOpenWebViewWithStateParameter() {
        Uri fakeUri = Uri.parse("http://foo.com");
        when(signInOperations.generateRemoteSignInUri("/activate/something")).thenReturn(fakeUri);
        String target = "http://soundcloud.com/activate/something";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verify(navigator).openRemoteSignInWebView(navigationTarget.activity(), fakeUri);
    }

    @Test
    public void deeplink_shouldOpenWebViewWithoutStateParameter() {
        Uri fakeUri = Uri.parse("http://foo.com");
        when(signInOperations.generateRemoteSignInUri()).thenReturn(fakeUri);
        String target = "soundcloud://remote-sign-in";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verify(navigator).openRemoteSignInWebView(navigationTarget.activity(), fakeUri);
    }

    @Test
    public void deeplink_shouldOpenExternEmailUri() throws Exception {
        String target = "mailto:test@abc.com";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verify(navigator).openEmail(navigationTarget.activity(), "test@abc.com");
    }

    @Test
    public void deeplink_shouldOpenExternEmail() throws Exception {
        String target = "test@abc.com";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verify(navigator).openEmail(navigationTarget.activity(), "test@abc.com");
    }

    @Test
    public void deeplink_shouldOpenExternWeb() throws Exception {
        String target = "https://www.google.com";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verify(navigator).openExternal(navigationTarget.activity(), Uri.parse(target));
    }

    @Test
    public void deeplink_shouldShowToastWhenNotResolvable() throws Exception {
        String target = "https://www.google.com";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verify(navigator).openExternal(navigationTarget.activity(), Uri.parse(target));
    }

    @Test
    public void deeplink_shouldGoToTheUpload() throws Exception {
        String target = "soundcloud://the-upload";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);

        resolveTarget(navigationTarget);

        verifyTrackingEvent(Optional.of(Referrer.OTHER.value()));
        verifyZeroInteractions(resolveOperations);
        verify(navigator).openNewForYou(navigationTarget.activity());
    }

    // For Navigation

    @Test
    public void navigation_shouldLaunchSoundStreamIfUriBlank() {
        NavigationTarget navigationTarget = getTargetForNavigation("");

        resolveTarget(navigationTarget);

        verify(navigator).openStream(navigationTarget.activity(), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldLaunchSoundStreamIfUriNull() {
        NavigationTarget navigationTarget = getTargetForNavigation(null);

        resolveTarget(navigationTarget);

        verify(navigator).openStream(navigationTarget.activity(), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldConvertOpaqueUriToHierarchicalAndLaunchPlayer() {
        String target = "soundcloud:tracks:123";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verify(resolveOperations, never()).resolve(anyString());
        verify(playbackInitiator).startPlayback(Urn.forTrack(123), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldResolveLocallyAndLaunchPlayer() {
        NavigationTarget navigationTarget = getTargetForNavigation("soundcloud://tracks:123");

        resolveTarget(navigationTarget);

        verify(resolveOperations, never()).resolve(anyString());
        verify(playbackInitiator).startPlayback(Urn.forTrack(123), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldConvertOpaqueUriWithAdjustReftagQueryParamToHierarchicalAndLaunchPlayer() {
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
                .thenReturn(rx.Observable.just(playbackResult));

        resolveTarget(navigationTarget);

        verify(resolveOperations).resolve(target);
        verify(playbackInitiator).startPlayback(Urn.forTrack(123), NAVIGATION_SCREEN);
        verifyZeroInteractions(navigator);
        verify(expandPlayerSubscriber).onNext(playbackResult);
    }

    @Test
    public void navigation_shouldNotOpenStreamWhenFailedToStartPlayback() throws CreateModelException {
        String target = "soundcloud://sounds:123";
        when(resolveOperations.resolve(target)).thenReturn(Single.just(RESULT_TRACK));
        NavigationTarget navigationTarget = getTargetForNavigation(target);
        PlaybackResult playbackResult = PlaybackResult.error(TRACK_UNAVAILABLE_OFFLINE);
        when(playbackInitiator.startPlayback(Urn.forTrack(123), NAVIGATION_SCREEN))
                .thenReturn(rx.Observable.just(playbackResult));

        resolveTarget(navigationTarget);

        verify(resolveOperations).resolve(target);
        verify(playbackInitiator).startPlayback(Urn.forTrack(123), NAVIGATION_SCREEN);
        verifyZeroInteractions(navigator);
        verify(expandPlayerSubscriber).onNext(playbackResult);
    }

    @Test
    public void navigation_shouldConvertOpaqueUriToHierarchicalAndLaunchPlaylistDetails() {
        String target = "soundcloud:playlists:123";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verify(resolveOperations, never()).resolve(anyString());
        verify(navigator).legacyOpenPlaylist(navigationTarget.activity(), Urn.forPlaylist(123), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldLaunchPlaylistDetailsDirectlyFromHierarchicalUriSplitByColon() throws CreateModelException {
        String target = "soundcloud://playlists:123";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verify(resolveOperations, never()).resolve(anyString());
        verify(navigator).legacyOpenPlaylist(navigationTarget.activity(), Urn.forPlaylist(123), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldConvertOpaqueUriToHierarchicalAndLaunchUserProfile() {
        String target = "soundcloud:users:123";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verify(resolveOperations, never()).resolve(anyString());
        verify(navigator).legacyOpenProfile(navigationTarget.activity(), Urn.forUser(123), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldLaunchUserProfileDirectlyFromHierarchicalUriSplitByColon() throws CreateModelException {
        String target = "soundcloud://users:123";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verify(resolveOperations, never()).resolve(anyString());
        verify(navigator).legacyOpenProfile(navigationTarget.activity(), Urn.forUser(123), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldLaunchOnboardingWithExtraUrnForLoggedOutUsers() {
        String target = "soundcloud://sounds:123";
        NavigationTarget navigationTarget = getTargetForNavigation(target);
        when(resolveOperations.resolve(target)).thenReturn(Single.just(RESULT_TRACK));
        when(accountOperations.isUserLoggedIn()).thenReturn(false);

        resolveTarget(navigationTarget);

        verify(navigator).openOnboarding(navigationTarget.activity(), Uri.parse(target), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldLaunchSearchForWebScheme() {
        String target = "https://soundcloud.com/search?q=skrillex";
        NavigationTarget navigationTarget = getTargetForNavigation(target);


        resolveTarget(navigationTarget);

        verifyZeroInteractions(eventBus);
        verify(navigator).openSearch(navigationTarget.activity(), Uri.parse(target), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldLaunchSearchForSoundCloudScheme() {
        String target = "soundcloud://search?q=skrillex";
        NavigationTarget navigationTarget = getTargetForNavigation(target);


        resolveTarget(navigationTarget);

        verifyZeroInteractions(eventBus);
        verify(navigator).openSearch(navigationTarget.activity(), Uri.parse(target), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldNotLaunchSearchForLoggedOutUsers() {
        String target = "soundcloud://search?q=skrillex";
        NavigationTarget navigationTarget = getTargetForNavigation(target);
        when(accountOperations.isUserLoggedIn()).thenReturn(false);


        resolveTarget(navigationTarget);

        verifyZeroInteractions(eventBus);
        verify(navigator).openOnboarding(navigationTarget.activity(), Uri.parse(target), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldLaunchRecordForWebScheme() {
        String target = "https://soundcloud.com/upload";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verifyZeroInteractions(eventBus);
        verify(navigator).openRecord(navigationTarget.activity(), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldLaunchRecordForSoundCloudSchemeWithUpload() {
        String target = "soundcloud://upload";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verifyZeroInteractions(eventBus);
        verify(navigator).openRecord(navigationTarget.activity(), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldLaunchRecordForSoundCloudSchemeWithRecord() {
        String target = "soundcloud://record";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verifyZeroInteractions(eventBus);
        verify(navigator).openRecord(navigationTarget.activity(), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldNotLaunchRecordForLoggedOutUsers() {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        String target = "soundcloud://record";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verifyZeroInteractions(eventBus);
        verify(navigator).openOnboarding(navigationTarget.activity(), Uri.parse(target), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldLaunchUpgradeForSoundCloudScheme() {
        when(featureOperations.upsellHighTier()).thenReturn(true);
        String target = "soundcloud://soundcloudgo";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verifyZeroInteractions(eventBus);
        verify(navigator).openUpgradeOnMain(navigationTarget.activity(), UpsellContext.DEFAULT);
    }

    @Test
    public void navigation_shouldNotLaunchUpgradeWhenUpsellFeatureIsDisabled() {
        when(featureOperations.upsellHighTier()).thenReturn(false);
        String target = "soundcloud://soundcloudgo";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verifyZeroInteractions(eventBus);
        verify(navigator).openStream(navigationTarget.activity(), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldNotLaunchMidTierCheckoutIfUserAlreadyHasAHighTierPlan() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.HIGH_TIER);
        String target = "soundcloud://buysoundcloudgo";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verifyZeroInteractions(eventBus);
        verify(navigator).openStream(navigationTarget.activity(), NAVIGATION_SCREEN);
        assertThat(ShadowToast.getTextOfLatestToast())
                .isEqualTo(context().getString(R.string.product_choice_error_already_subscribed));
    }

    @Test
    public void navigation_shouldNotLaunchMidTierCheckoutIfUserAlreadyHasAMidTierPlan() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.MID_TIER);
        String target = "soundcloud://buysoundcloudgo";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verifyZeroInteractions(eventBus);
        verify(navigator).openStream(navigationTarget.activity(), NAVIGATION_SCREEN);
        assertThat(ShadowToast.getTextOfLatestToast())
                .isEqualTo(context().getString(R.string.product_choice_error_already_subscribed));
    }

    @Test
    public void navigation_shouldLaunchMidTierCheckoutForFreeUserWithSoundCloudScheme() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellBothTiers()).thenReturn(true);
        String target = "soundcloud://buysoundcloudgo";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verifyZeroInteractions(eventBus);
        verify(navigator).openDirectCheckout(navigationTarget.activity(), Plan.MID_TIER);
    }

    @Test
    public void navigation_shouldNotLaunchMidTierCheckout() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellBothTiers()).thenReturn(false);
        String target = "soundcloud://buysoundcloudgo";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verifyZeroInteractions(eventBus);
        verify(navigator).openStream(navigationTarget.activity(), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldNotLaunchHighTierCheckoutIfUserAlreadyHasAHighTierPlan() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.HIGH_TIER);
        String target = "soundcloud://buysoundcloudgoplus";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verifyZeroInteractions(eventBus);
        verify(navigator).openStream(navigationTarget.activity(), NAVIGATION_SCREEN);
        assertThat(ShadowToast.getTextOfLatestToast())
                .isEqualTo(context().getString(R.string.product_choice_error_already_subscribed));
    }

    @Test
    public void navigation_shouldLaunchHighTierCheckoutForMidTierUserWithSoundCloudScheme() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.MID_TIER);
        when(featureOperations.upsellHighTier()).thenReturn(true);
        String target = "soundcloud://buysoundcloudgoplus";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verifyZeroInteractions(eventBus);
        verify(navigator).openDirectCheckout(navigationTarget.activity(), Plan.HIGH_TIER);
    }

    @Test
    public void navigation_shouldLaunchHighTierCheckoutForFreeUserWithSoundCloudScheme() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellHighTier()).thenReturn(true);
        String target = "soundcloud://buysoundcloudgoplus";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verifyZeroInteractions(eventBus);
        verify(navigator).openDirectCheckout(navigationTarget.activity(), Plan.HIGH_TIER);
    }

    @Test
    public void navigation_shouldNotLaunchHighTierCheckout() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellHighTier()).thenReturn(false);
        String target = "soundcloud://buysoundcloudgoplus";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verifyZeroInteractions(eventBus);
        verify(navigator).openStream(navigationTarget.activity(), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldNotLaunchProductChoiceIfUserAlreadyHasAGoPlan() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.HIGH_TIER);
        String target = "soundcloud://soundcloudgo/soundcloudgo";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verifyZeroInteractions(eventBus);
        verify(navigator).openStream(navigationTarget.activity(), NAVIGATION_SCREEN);
        assertThat(ShadowToast.getTextOfLatestToast())
                .isEqualTo(context().getString(R.string.product_choice_error_already_subscribed));
    }

    @Test
    public void navigation_shouldLaunchProductChoiceForMidTierUpsell() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellBothTiers()).thenReturn(true);
        String target = "soundcloud://soundcloudgo/soundcloudgo";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verifyZeroInteractions(eventBus);
        verify(navigator).openProductChoiceOnMain(navigationTarget.activity(), Plan.MID_TIER);
    }

    @Test
    public void navigation_shouldLaunchProductChoiceForHighTierUpsell() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellBothTiers()).thenReturn(true);
        String target = "soundcloud://soundcloudgo/soundcloudgoplus";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verifyZeroInteractions(eventBus);
        verify(navigator).openProductChoiceOnMain(navigationTarget.activity(), Plan.HIGH_TIER);
    }

    @Test
    public void navigation_shouldNotLaunchProductChoice() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(featureOperations.upsellBothTiers()).thenReturn(false);
        String target = "soundcloud://soundcloudgo/soundcloudgo";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verifyZeroInteractions(eventBus);
        verify(navigator).openStream(navigationTarget.activity(), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldLaunchOfflineSettingsForSoundCloudScheme() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        String target = "soundcloud://settings_offlinelistening";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verifyZeroInteractions(eventBus);
        verify(navigator).openOfflineSettings(navigationTarget.activity());
    }

    @Test
    public void navigation_shouldNotLaunchOfflineSettingsWhenOfflineContentIsNotEnabled() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        String target = "soundcloud://settings_offlinelistening";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verifyZeroInteractions(eventBus);
        verify(navigator).openStream(navigationTarget.activity(), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldLaunchNotificationPreferences() {
        String target = "soundcloud://notification_preferences";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verifyZeroInteractions(eventBus);
        verify(navigator).openNotificationPreferencesFromDeeplink(navigationTarget.activity());
    }

    @Test
    public void navigation_shouldLaunchCollection() {
        String target = "soundcloud://collection";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verify(navigator).openCollection(navigationTarget.activity());
    }

    @Test
    public void navigation_shouldOpenDiscoverFromWebLink() {
        String target = "https://soundcloud.com/discover";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verify(navigator).openViewAllRecommendations(navigationTarget.activity());
    }

    @Test
    public void navigation_shouldOpenDiscoveryFromUri() {
        String target = "soundcloud://discovery";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verify(navigator).openDiscovery(navigationTarget.activity(), NAVIGATION_SCREEN);
    }

    @Test
    public void navigation_shouldOpenChartsFromWebLink() {
        String target = "https://soundcloud.com/charts/top?genre=all";
        when(chartsUriResolver.resolveUri(Uri.parse(target))).thenReturn(ChartDetails.create(ChartType.TOP, Chart.GLOBAL_GENRE, ChartCategory.MUSIC, Optional.of(TOP_FIFTY)));
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verify(navigator).openChart(navigationTarget.activity(),
                                    Chart.GLOBAL_GENRE,
                                    ChartType.TOP,
                                    ChartCategory.MUSIC,
                                    TOP_FIFTY);
    }

    @Test
    public void navigation_shouldOpenChartsFromUri() {
        String target = "soundcloud://charts:top:all";
        when(chartsUriResolver.resolveUri(Uri.parse(target))).thenReturn(ChartDetails.create(ChartType.TOP, Chart.GLOBAL_GENRE, ChartCategory.MUSIC, Optional.of(TOP_FIFTY)));
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verify(navigator).openChart(navigationTarget.activity(),
                                    Chart.GLOBAL_GENRE,
                                    ChartType.TOP,
                                    ChartCategory.MUSIC,
                                    TOP_FIFTY);
    }

    @Test
    public void navigation_shouldOpenWebViewWithStateParameter() {
        Uri fakeUri = Uri.parse("http://foo.com");
        when(signInOperations.generateRemoteSignInUri("/activate/something")).thenReturn(fakeUri);
        String target = "http://soundcloud.com/activate/something";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verify(navigator).openRemoteSignInWebView(navigationTarget.activity(), fakeUri);
    }

    @Test
    public void navigation_shouldOpenWebViewWithoutStateParameter() {
        Uri fakeUri = Uri.parse("http://foo.com");
        when(signInOperations.generateRemoteSignInUri()).thenReturn(fakeUri);
        String target = "soundcloud://remote-sign-in";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verify(navigator).openRemoteSignInWebView(navigationTarget.activity(), fakeUri);
    }

    @Test
    public void navigation_shouldOpenExternEmailUri() throws Exception {
        String target = "mailto:test@abc.com";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verify(navigator).openEmail(navigationTarget.activity(), "test@abc.com");
    }

    @Test
    public void navigation_shouldOpenExternEmail() throws Exception {
        String target = "test@abc.com";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verify(navigator).openEmail(navigationTarget.activity(), "test@abc.com");
    }

    @Test
    public void navigation_shouldOpenExternWeb() throws Exception {
        String target = "https://www.google.com";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verify(navigator).openExternal(navigationTarget.activity(), Uri.parse(target));
    }

    @Test
    public void navigation_shouldGoToTheUpload() throws Exception {
        String target = "soundcloud://the-upload";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verifyZeroInteractions(resolveOperations);
        verify(navigator).openNewForYou(navigationTarget.activity());
    }

    // Fallback Errors

    @Test
    public void fallbackToWebView() throws Exception {
        String target = "https://soundcloud.com/jobs/";
        NavigationTarget navigationTarget = getTargetForNavigation(target);

        resolveTarget(navigationTarget);

        verifyZeroInteractions(resolveOperations);
        verify(navigator).openWebView(navigationTarget.activity(), Uri.parse(target));
    }

    @Test
    public void retryWithFallback() throws Exception {
        String target = "soundcloud://target";
        String fallback = "soundcloud://fallback";

        NavigationTarget navigationTarget = getTargetForNavigation(target, fallback);
        when(resolveOperations.resolve(target)).thenReturn(Single.just(ResolveResult.error(Uri.parse(target), null)));
        when(resolveOperations.resolve(fallback)).thenReturn(Single.just(ResolveResult.succes(Urn.forComment(123))));

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

        resolveTarget(navigationTarget);

        assertThat(ShadowToast.getTextOfLatestToast())
                .isEqualTo(context().getString(R.string.error_unknown_navigation));
    }

    @Test
    public void reportErrorForDeeplink() throws Exception {
        String target = "soundcloud://target";
        NavigationTarget navigationTarget = getTargetForDeeplink(target);
        when(resolveOperations.resolve(target)).thenReturn(Single.just(ResolveResult.error(Uri.parse(target), null)));

        resolveTarget(navigationTarget);

        assertThat(ShadowToast.getTextOfLatestToast())
                .isEqualTo(context().getString(R.string.error_unknown_navigation));
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
        return NavigationTarget.forNavigation(activity(), target, Optional.fromNullable(fallback), NAVIGATION_SCREEN);
    }

    private NavigationTarget getTargetForDeeplink(String target) {
        return getTargetForDeeplink(target, Referrer.OTHER);
    }

    private NavigationTarget getTargetForDeeplink(String target, Referrer referrer) {
        return NavigationTarget.forDeeplink(activity(), target, referrer.value());
    }

    private void resolveTarget(NavigationTarget navigationTarget) {
        resolver.resolveNavigationResult(navigationTarget).subscribeWith(new TestSubscriber());
    }

    private static final class TestSubscriber extends DefaultSingleObserver<NavigationResult> {
        @Override
        public void onSuccess(NavigationResult navigationResult) {
            try {
                navigationResult.action().run();
            } catch (Exception e) {
                fail("Exception during execution");
            }
        }
    }
}
