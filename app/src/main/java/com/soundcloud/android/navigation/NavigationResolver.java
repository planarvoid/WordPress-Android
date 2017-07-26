package com.soundcloud.android.navigation;

import static com.soundcloud.android.navigation.IntentFactory.createActivitiesIntent;
import static com.soundcloud.android.navigation.IntentFactory.createAdClickthroughIntent;
import static com.soundcloud.android.navigation.IntentFactory.createAllGenresIntent;
import static com.soundcloud.android.navigation.IntentFactory.createChartsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createCollectionIntent;
import static com.soundcloud.android.navigation.IntentFactory.createConversionIntent;
import static com.soundcloud.android.navigation.IntentFactory.createFollowersIntent;
import static com.soundcloud.android.navigation.IntentFactory.createFollowingsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createFullscreenVideoAdIntent;
import static com.soundcloud.android.navigation.IntentFactory.createHelpCenterIntent;
import static com.soundcloud.android.navigation.IntentFactory.createHomeIntent;
import static com.soundcloud.android.navigation.IntentFactory.createLauncherIntent;
import static com.soundcloud.android.navigation.IntentFactory.createLegalIntent;
import static com.soundcloud.android.navigation.IntentFactory.createLikedStationsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createNewForYouIntent;
import static com.soundcloud.android.navigation.IntentFactory.createOfflineSettingsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createOfflineSettingsOnboardingIntent;
import static com.soundcloud.android.navigation.IntentFactory.createOnboardingIntent;
import static com.soundcloud.android.navigation.IntentFactory.createPlaylistIntent;
import static com.soundcloud.android.navigation.IntentFactory.createPlaylistsAndAlbumsCollectionIntent;
import static com.soundcloud.android.navigation.IntentFactory.createPlaylistsCollectionIntent;
import static com.soundcloud.android.navigation.IntentFactory.createPrestititalAdIntent;
import static com.soundcloud.android.navigation.IntentFactory.createProductChoiceIntent;
import static com.soundcloud.android.navigation.IntentFactory.createProfileAlbumsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createProfileIntent;
import static com.soundcloud.android.navigation.IntentFactory.createProfileLikesIntent;
import static com.soundcloud.android.navigation.IntentFactory.createProfilePlaylistsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createProfileRepostsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createProfileTracksIntent;
import static com.soundcloud.android.navigation.IntentFactory.createRecordIntent;
import static com.soundcloud.android.navigation.IntentFactory.createRecordPermissionIntent;
import static com.soundcloud.android.navigation.IntentFactory.createSearchIntent;
import static com.soundcloud.android.navigation.IntentFactory.createSettingsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createStationsInfoIntent;
import static com.soundcloud.android.navigation.IntentFactory.createStreamWithExpandedPlayerIntent;
import static com.soundcloud.android.navigation.IntentFactory.createSystemPlaylistIntent;

import com.soundcloud.android.BuildConfig;
import com.soundcloud.android.PlaybackServiceController;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.deeplinks.AllGenresUriResolver;
import com.soundcloud.android.deeplinks.ChartDetails;
import com.soundcloud.android.deeplinks.ChartsUriResolver;
import com.soundcloud.android.deeplinks.DeepLink;
import com.soundcloud.android.deeplinks.UriResolveException;
import com.soundcloud.android.events.DeeplinkReportEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.olddiscovery.DefaultHomeScreenConfiguration;
import com.soundcloud.android.onboarding.auth.SignInOperations;
import com.soundcloud.android.payments.UpsellContext;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.stations.StationsUriResolver;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.utils.UriUtils;
import com.soundcloud.java.checks.Preconditions;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.rx.eventbus.EventBus;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.CheckResult;
import android.support.v4.content.ContextCompat;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"PMD.GodClass", "RestrictedApi"})
public class NavigationResolver {

    private final Context context;
    private final ResolveOperations resolveOperations;
    private final AccountOperations accountOperations;
    private final PlaybackServiceController serviceController;
    private final PlaybackInitiator playbackInitiator;
    private final PlayQueueManager playQueueManager;
    private final EventBus eventBus;
    private final FeatureOperations featureOperations;
    private final ChartsUriResolver chartsUriResolver;
    private final SignInOperations signInOperations;
    private final LocalEntityUriResolver localEntityUriResolver;
    private final StationsUriResolver stationsUriResolver;
    private final ApplicationProperties applicationProperties;
    private final EventTracker eventTracker;
    private final DefaultHomeScreenConfiguration defaultHomeScreenConfiguration;
    private final OfflineSettingsStorage offlineSettingsStorage;

    @Inject
    NavigationResolver(Context context,
                       ResolveOperations resolveOperations,
                       LocalEntityUriResolver localEntityUriResolver,
                       AccountOperations accountOperations,
                       PlaybackServiceController serviceController,
                       PlaybackInitiator playbackInitiator,
                       PlayQueueManager playQueueManager,
                       EventBus eventBus,
                       FeatureOperations featureOperations,
                       ChartsUriResolver chartsUriResolver,
                       SignInOperations signInOperations,
                       StationsUriResolver stationsUriResolver,
                       ApplicationProperties applicationProperties,
                       EventTracker eventTracker,
                       DefaultHomeScreenConfiguration defaultHomeScreenConfiguration,
                       OfflineSettingsStorage offlineSettingsStorage) {
        this.context = context;
        this.resolveOperations = resolveOperations;
        this.accountOperations = accountOperations;
        this.serviceController = serviceController;
        this.playbackInitiator = playbackInitiator;
        this.playQueueManager = playQueueManager;
        this.eventBus = eventBus;
        this.featureOperations = featureOperations;
        this.chartsUriResolver = chartsUriResolver;
        this.signInOperations = signInOperations;
        this.localEntityUriResolver = localEntityUriResolver;
        this.stationsUriResolver = stationsUriResolver;
        this.applicationProperties = applicationProperties;
        this.eventTracker = eventTracker;
        this.defaultHomeScreenConfiguration = defaultHomeScreenConfiguration;
        this.offlineSettingsStorage = offlineSettingsStorage;
    }

    @CheckResult
    Single<NavigationResult> resolveNavigationResult(NavigationTarget navigationTarget) {
        if (!navigationTarget.linkNavigationParameters().isPresent() || Strings.isNullOrEmpty(navigationTarget.linkNavigationParameters().get().target())) {
            try {
                if (navigationTarget.deeplink().isPresent()) {
                    return handleDeepLink(navigationTarget, navigationTarget.deeplink().get());
                } else {
                    return showHome(navigationTarget);
                }
            } catch (UriResolveException e) {
                return handleUriResolveException(e, showHome(navigationTarget));
            }
        } else {
            final Uri hierarchicalUri = UriUtils.convertToHierarchicalUri(Uri.parse(navigationTarget.linkNavigationParameters().get().target()));
            NavigationTarget newTarget = navigationTarget.withTarget(hierarchicalUri.toString());
            try {
                if (localEntityUriResolver.canResolveLocally(newTarget.linkNavigationParameters().get().target())) {
                    return resolveLocal(navigationTarget, newTarget);
                } else if (localEntityUriResolver.isKnownDeeplink(newTarget.linkNavigationParameters().get().target())) {
                    return resolveDeeplink(hierarchicalUri, newTarget);
                } else {
                    return resolveTarget(navigationTarget);
                }
            } catch (UriResolveException e) {
                return handleUriResolveException(e, resolveTarget(navigationTarget));
            }
        }
    }

    private Single<NavigationResult> handleUriResolveException(UriResolveException e, Single<NavigationResult> result) {
        final String msg = "Local resolve failed";
        if (applicationProperties.isDebuggableFlavor()) {
            return result.map(navigationResult -> navigationResult.withToast(msg));
        }
        ErrorUtils.handleSilentException(msg, e);
        return result;
    }

    @CheckResult
    private Single<NavigationResult> resolveDeeplink(Uri hierarchicalUri, NavigationTarget newTarget) throws UriResolveException {
        final DeepLink deepLink = DeepLink.fromUri(hierarchicalUri);
        if (shouldShowLogInMessage(deepLink, newTarget.referrer())) {
            return showOnboardingForDeeplink(newTarget);
        } else {
            return handleDeepLink(newTarget, deepLink);
        }
    }

    @CheckResult
    private Single<NavigationResult> resolveLocal(NavigationTarget navigationTarget, NavigationTarget newTarget) throws UriResolveException {
        return localEntityUriResolver.resolve(newTarget.linkNavigationParameters().get().target())
                                     .observeOn(AndroidSchedulers.mainThread())
                                     .flatMap(urn -> startActivityForResource(navigationTarget, urn));
    }

    @CheckResult
    private Single<NavigationResult> showOnboardingForDeeplink(NavigationTarget navigationTarget) {
        return showOnboardingForUri(navigationTarget)
                .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> handleResolveResult(ResolveResult resolveResult, NavigationTarget navigationTarget) {
        if (resolveResult.success() && localEntityUriResolver.canResolveLocally(resolveResult.urn().get())) {
            return startActivityForResource(navigationTarget, resolveResult.urn().get());
        } else {
            return handleUnsuccessfulResolve(navigationTarget, resolveResult);
        }
    }

    @CheckResult
    private Single<NavigationResult> handleUnsuccessfulResolve(NavigationTarget navigationTarget, ResolveResult resolveResult) {
        final Optional<String> errorUrl = resolveResult.uri().transform(Uri::toString);
        final NavigationTarget fallbackAwareTarget = navigationTarget.withFallback(navigationTarget.linkNavigationParameters().get().fallback().or(errorUrl));
        if (shouldRetryWithFallback(fallbackAwareTarget)) {
            final Exception e = resolveResult.exception().or(new UriResolveException("Resolve with fallback"));
            ErrorUtils.handleSilentException("Resolve uri " + navigationTarget.linkNavigationParameters().get().target() + " with fallback " + fallbackAwareTarget.linkNavigationParameters()
                                                                                                                                                                  .get()
                                                                                                                                                                  .fallback()
                                                                                                                                                                  .orNull(), e);
            return resolveNavigationResult(fallbackAwareTarget.withTarget(fallbackAwareTarget.linkNavigationParameters().get().fallback().get())
                                                                       .withFallback(Optional.absent()))
                    .map(result -> {
                        if (applicationProperties.isDebuggableFlavor()) {
                            return result.withToast("Retry resolve with fallback");
                        }
                        return result;
                    });
        } else {
            trackForegroundEvent(fallbackAwareTarget);
            Optional<Exception> exception = resolveResult.exception();
            if (exception.isPresent() && !ErrorUtils.isNetworkError(exception.get())) {
                ErrorUtils.handleSilentException("unable to load deeplink:" + errorUrl, exception.get());
                reportFailedToResolveDeeplink(fallbackAwareTarget);
            }
            return launchApplicationWithMessage(fallbackAwareTarget, R.string.error_unknown_navigation);
        }
    }

    @CheckResult
    private Single<NavigationResult> handleDeepLink(NavigationTarget navigationTarget, DeepLink deepLink) throws UriResolveException {
        switch (deepLink) {
            case HOME:
                return showHome(navigationTarget);
            case STREAM:
                return showStream(navigationTarget);
            case RECORD:
                return showRecordScreen(navigationTarget);
            case DISCOVERY:
                return showDiscoveryScreen(navigationTarget);
            case CHARTS:
                return showCharts(navigationTarget);
            case CHARTS_ALL_GENRES:
                return showAllGenresCharts(navigationTarget);
            case LIKED_STATIONS:
                return showLikedStations(navigationTarget);
            case STATION:
                return showStation(navigationTarget);
            case SEARCH:
                return showSearchScreen(navigationTarget);
            case SEARCH_AUTOCOMPLETE:
                return showSearchAutocompleteScreen(navigationTarget);
            case SEARCH_RESULTS_VIEW_ALL:
                return showSearchResultViewAllScreen(navigationTarget);
            case WEB_VIEW:
                return startWebView(navigationTarget);
            case SOUNDCLOUD_GO_PLUS_UPSELL:
                return showUpgradeScreen(navigationTarget);
            case SOUNDCLOUD_GO_BUY:
                return showMidTierCheckoutScreen(navigationTarget);
            case SOUNDCLOUD_GO_PLUS_BUY:
                return showHighTierCheckoutScreen(navigationTarget);
            case SOUNDCLOUD_GO_CHOICE:
                return showProductChoiceScreen(navigationTarget, Plan.MID_TIER);
            case SOUNDCLOUD_GO_PLUS_CHOICE:
                return showProductChoiceScreen(navigationTarget, Plan.HIGH_TIER);
            case OFFLINE_SETTINGS:
                return showOfflineSettingsScreen(navigationTarget);
            case NOTIFICATION_PREFERENCES:
                return showNotificationPreferencesScreen(navigationTarget);
            case COLLECTION:
                return showCollectionScreen(navigationTarget);
            case SHARE_APP:
                return shareApp(navigationTarget);
            case SYSTEM_SETTINGS:
                return showSystemSettings(navigationTarget);
            case REMOTE_SIGN_IN:
                return startWebViewForRemoteSignIn(navigationTarget);
            case THE_UPLOAD:
                return startTheUpload(navigationTarget);
            case UNKNOWN:
                return startExternal(navigationTarget);
            case ACTIVITIES:
                return showActivities(navigationTarget);
            case FOLLOWERS:
                return showFollowers(navigationTarget);
            case FOLLOWINGS:
                return showFollowings(navigationTarget);
            case AD_FULLSCREEN_VIDEO:
                return showFullscreenVideoAd(navigationTarget);
            case AD_PRESTITIAL:
                return showPrestitialAd(navigationTarget);
            case AD_CLICKTHROUGH:
                return showAdClickthrough(navigationTarget);
            case PROFILE:
                return showProfile(navigationTarget);
            case PROFILE_REPOSTS:
                return showProfileReposts(navigationTarget);
            case PROFILE_TRACKS:
                return showProfileTracks(navigationTarget);
            case PROFILE_LIKES:
                return showProfileLikes(navigationTarget);
            case PROFILE_ALBUMS:
                return showProfileAlbums(navigationTarget);
            case PROFILE_PLAYLISTS:
                return showProfilePlaylists(navigationTarget);
            case SYSTEM_PLAYLIST:
                return showSystemPlaylist(navigationTarget);
            case PLAYLISTS_AND_ALBUMS_COLLECTION:
                return showPlaylistsAndAlbumsCollection(navigationTarget);
            case PLAYLISTS_COLLECTION:
                return showPlaylistsCollection(navigationTarget);
            case PLAYLISTS:
                return showPlaylist(navigationTarget);
            case HELP_CENTER:
                return showHelpCenter(navigationTarget);
            case LEGAL:
                return showLegal(navigationTarget);
            case BASIC_SETTINGS:
                return showBasicSettings(navigationTarget);
            default:
                return resolveTarget(navigationTarget);
        }
    }

    @CheckResult
    private Single<NavigationResult> showBasicSettings(NavigationTarget navigationTarget) {
        return Single.just(NavigationResult.create(navigationTarget, createSettingsIntent(context)))
                     .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> showLegal(NavigationTarget navigationTarget) {
        return Single.just(NavigationResult.create(navigationTarget, createLegalIntent(context)))
                     .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> showHelpCenter(NavigationTarget navigationTarget) {
        return Single.just(NavigationResult.create(navigationTarget, createHelpCenterIntent(context)))
                     .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> showProfile(NavigationTarget navigationTarget) {
        return showProfile(navigationTarget, navigationTarget.targetUrn().get());
    }

    @CheckResult
    private Single<NavigationResult> showProfile(NavigationTarget navigationTarget, Urn urn) {
        return Single.just(NavigationResult.create(navigationTarget, createProfileIntent(context,
                                                                                         urn,
                                                                                         Optional.of(navigationTarget.screen()),
                                                                                         navigationTarget.searchQuerySourceInfo(),
                                                                                         navigationTarget.referrer().transform(Referrer::fromOrigin)))
        ).doOnSuccess(__ -> {
            trackForegroundEvent(navigationTarget);
            trackNavigationEvent(navigationTarget.uiEvent());
        });
    }

    @CheckResult
    private Single<NavigationResult> showProfileReposts(NavigationTarget navigationTarget) {
        return Single.just(NavigationResult.create(navigationTarget,
                                                   createProfileRepostsIntent(context, navigationTarget.targetUrn().get(), navigationTarget.screen(), navigationTarget.searchQuerySourceInfo())))
                     .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> showProfileTracks(NavigationTarget navigationTarget) {
        return Single.just(NavigationResult.create(navigationTarget,
                                                   createProfileTracksIntent(context, navigationTarget.targetUrn().get(), navigationTarget.screen(), navigationTarget.searchQuerySourceInfo())))
                     .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> showProfileLikes(NavigationTarget navigationTarget) {
        return Single.just(NavigationResult.create(navigationTarget,
                                                   createProfileLikesIntent(context, navigationTarget.targetUrn().get(), navigationTarget.screen(), navigationTarget.searchQuerySourceInfo())))
                     .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> showProfileAlbums(NavigationTarget navigationTarget) {
        return Single.just(NavigationResult.create(navigationTarget,
                                                   createProfileAlbumsIntent(context, navigationTarget.targetUrn().get(), navigationTarget.screen(), navigationTarget.searchQuerySourceInfo())))
                     .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }


    @CheckResult
    private Single<NavigationResult> showProfilePlaylists(NavigationTarget navigationTarget) {
        return Single.just(NavigationResult.create(navigationTarget,
                                                   createProfilePlaylistsIntent(context, navigationTarget.targetUrn().get(), navigationTarget.screen(), navigationTarget.searchQuerySourceInfo())))
                     .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> startExternal(NavigationTarget navigationTarget) {
        trackForegroundEvent(navigationTarget);
        String target = navigationTarget.linkNavigationParameters().get().target();
        Preconditions.checkNotNull(target, "Covered by #resolve");
        Uri targetUri = Uri.parse(target);
        final String identifier = Optional.fromNullable(targetUri.getAuthority()).or(targetUri.getPath());
        if (ScTextUtils.isEmail(identifier)) {
            return Single.just(NavigationResult.create(navigationTarget, IntentFactory.createEmailIntent(identifier)));
        } else {
            return Single.just(NavigationResult.create(navigationTarget, IntentFactory.createViewIntent(navigationTarget.linkNavigationParameters().get().targetUri())));
        }
    }

    @CheckResult
    private Single<NavigationResult> showActivities(NavigationTarget navigationTarget) {
        return Single.just(NavigationResult.create(navigationTarget, createActivitiesIntent(context)))
                     .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> showFollowers(NavigationTarget navigationTarget) {
        return Single.just(NavigationResult.create(navigationTarget, createFollowersIntent(context, navigationTarget.targetUrn().get(), navigationTarget.searchQuerySourceInfo())))
                     .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> showFollowings(NavigationTarget navigationTarget) {
        return Single.just(NavigationResult.create(navigationTarget, createFollowingsIntent(context, navigationTarget.targetUrn().get(), navigationTarget.searchQuerySourceInfo())))
                     .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> showFullscreenVideoAd(NavigationTarget navigationTarget) {
        return Single.just(NavigationResult.create(navigationTarget, createFullscreenVideoAdIntent(context, navigationTarget.targetUrn().get())))
                     .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> showPrestitialAd(NavigationTarget navigationTarget) {
        return Single.just(NavigationResult.create(navigationTarget, createPrestititalAdIntent(context)))
                     .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> showAdClickthrough(NavigationTarget navigationTarget) {
        return Single.just(NavigationResult.create(navigationTarget, createAdClickthroughIntent(Uri.parse(navigationTarget.deeplinkTarget().get()))))
                     .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> showSystemPlaylist(NavigationTarget navigationTarget) {
        return showSystemPlaylist(navigationTarget, navigationTarget.targetUrn().get());
    }

    @CheckResult
    private Single<NavigationResult> showSystemPlaylist(NavigationTarget navigationTarget, Urn urn) {
        return Single.just(NavigationResult.create(navigationTarget, createSystemPlaylistIntent(context, urn, navigationTarget.screen())))
                     .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> showPlaylistsAndAlbumsCollection(NavigationTarget navigationTarget) {
        return Single.just(NavigationResult.create(navigationTarget, createPlaylistsAndAlbumsCollectionIntent(context)))
                     .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> showPlaylistsCollection(NavigationTarget navigationTarget) {
        return Single.just(NavigationResult.create(navigationTarget, createPlaylistsCollectionIntent(context)))
                     .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> showPlaylist(NavigationTarget navigationTarget) {
        return showPlaylist(navigationTarget, navigationTarget.targetUrn().get());
    }

    @CheckResult
    private Single<NavigationResult> showPlaylist(NavigationTarget navigationTarget, Urn urn) {
        return Single.just(NavigationResult.create(navigationTarget, createPlaylistIntent(urn,
                                                                                          navigationTarget.screen(),
                                                                                          navigationTarget.searchQuerySourceInfo(),
                                                                                          navigationTarget.promotedSourceInfo())))
                     .doOnSuccess(__ -> {
                         trackForegroundEvent(navigationTarget);
                         trackNavigationEvent(navigationTarget.uiEvent());
                     });
    }

    @CheckResult
    private Single<NavigationResult> startWebViewForRemoteSignIn(NavigationTarget navigationTarget) {
        final Uri target;
        if (DeepLink.isWebScheme(navigationTarget.linkNavigationParameters().get().targetUri())) {
            target = signInOperations.generateRemoteSignInUri(navigationTarget.linkNavigationParameters().get().targetUri().getPath());
        } else {
            target = signInOperations.generateRemoteSignInUri();
        }

        return Single.just(NavigationResult.create(navigationTarget, IntentFactory.createRemoteSignInIntent(context, target)))
                     .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> startTheUpload(NavigationTarget navigationTarget) {
        return Single.just(NavigationResult.create(navigationTarget, createNewForYouIntent(context)))
                     .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> resolveTarget(NavigationTarget navigationTarget) {
        String target = navigationTarget.linkNavigationParameters().get().target();
        Preconditions.checkNotNull(target, "Covered by #resolve");
        return resolveOperations.resolve(target)
                                .observeOn(AndroidSchedulers.mainThread())
                                .flatMap(result -> handleResolveResult(result, navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> startWebView(NavigationTarget navigationTarget) {
        return Single.just(NavigationResult.create(navigationTarget, IntentFactory.createWebViewIntent(context, navigationTarget.linkNavigationParameters().get().targetUri())))
                     .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> showHome(NavigationTarget navigationTarget) {
        if (defaultHomeScreenConfiguration.isStreamHome()) {
            return showStream(navigationTarget);
        } else {
            return showDiscoveryScreen(navigationTarget);
        }
    }

    @CheckResult
    private Single<NavigationResult> showStream(NavigationTarget navigationTarget) {
        return Single.just(NavigationResult.create(navigationTarget, IntentFactory.createStreamIntent(navigationTarget.screen())))
                     .doOnSuccess(__ -> {
                         accountOperations.clearCrawler();
                         trackForegroundEvent(navigationTarget);
                     });
    }

    @CheckResult
    private Single<NavigationResult> showDiscoveryScreen(NavigationTarget navigationTarget) {
        return Single.just(NavigationResult.create(navigationTarget, IntentFactory.createDiscoveryIntent(navigationTarget.screen())))
                     .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> showCharts(NavigationTarget navigationTarget) {
        return Single.<NavigationResult>create(emitter -> {
            Optional<NavigationTarget.ChartsMetaData> chartsMetaData = navigationTarget.chartsMetaData();
            final ChartDetails chartDetails;
            if (chartsMetaData.isPresent()) {
                chartDetails = chartsMetaData.get().chartDetails().get();
            } else {
                chartDetails = chartsUriResolver.resolveUri(navigationTarget.linkNavigationParameters().get().targetUri());
            }
            Intent chartsIntent = createChartsIntent(context, chartDetails);
            emitter.onSuccess(NavigationResult.create(navigationTarget, chartsIntent));
        }).doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> showAllGenresCharts(NavigationTarget navigationTarget) {
        return Single.<NavigationResult>create(emitter -> {
            Optional<NavigationTarget.ChartsMetaData> chartsMetaData = navigationTarget.chartsMetaData();
            final ChartCategory category;
            if (chartsMetaData.isPresent()) {
                category = chartsMetaData.get().category().orNull();
            } else {
                category = AllGenresUriResolver.resolveUri(navigationTarget.linkNavigationParameters().get().targetUri());
            }
            emitter.onSuccess(NavigationResult.create(navigationTarget, createAllGenresIntent(context, category)));
        }).doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> showLikedStations(NavigationTarget navigationTarget) {
        return Single.just(createLikedStationsIntent(context))
                     .map(intent -> NavigationResult.create(navigationTarget, intent))
                     .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> showStation(NavigationTarget navigationTarget) throws UriResolveException {
        Optional<NavigationTarget.StationsInfoMetaData> stationsInfoMetaData = navigationTarget.stationsInfoMetaData();
        if (stationsInfoMetaData.isPresent()) {
            return showStation(navigationTarget, navigationTarget.targetUrn().get(), stationsInfoMetaData.get().seedTrack());
        } else {
            Optional<Urn> urn = stationsUriResolver.resolve(navigationTarget.linkNavigationParameters().get().targetUri());
            if (urn.isPresent()) {
                return showStation(navigationTarget, urn.get(), Optional.absent());
            } else {
                throw new UriResolveException("Station " + navigationTarget.linkNavigationParameters().get().target() + " could not be resolved locally");
            }
        }
    }

    @CheckResult
    private Single<NavigationResult> showStation(NavigationTarget navigationTarget, Urn urn, Optional<Urn> seedTrack) {
        return Single.just(createStationsInfoIntent(context, urn, seedTrack, navigationTarget.discoverySource().or(Optional.of(DiscoverySource.DEEPLINK))))
                     .map(intent -> NavigationResult.create(navigationTarget, intent))
                     .doOnSuccess(__ -> {
                         trackForegroundEvent(navigationTarget);
                         trackNavigationEvent(navigationTarget.uiEvent());
                     });
    }

    @CheckResult
    private Single<NavigationResult> showSearchScreen(NavigationTarget navigationTarget) {
        Intent intent = IntentFactory.createSearchActionIntent(context, navigationTarget.linkNavigationParameters().get().targetUri(), navigationTarget.screen());
        return Single.just(NavigationResult.create(navigationTarget, intent))
                     .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> showSearchAutocompleteScreen(NavigationTarget navigationTarget) {
        return Single.just(NavigationResult.create(navigationTarget, createSearchIntent(context)))
                     .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> showSearchResultViewAllScreen(NavigationTarget navigationTarget) {
        return Single.just(NavigationResult.create(navigationTarget, IntentFactory.createSearchViewAllIntent(context, navigationTarget.topResultsMetaData().get(), navigationTarget.queryUrn())))
                     .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> showRecordScreen(NavigationTarget navigationTarget) {
        return Single.<NavigationResult>create(emitter -> {
            if (hasMicrophonePermission(context)) {
                emitter.onSuccess(NavigationResult.create(navigationTarget, createRecordIntent(context, navigationTarget.recording(), navigationTarget.screen())));
            } else {
                emitter.onSuccess(NavigationResult.create(navigationTarget, createRecordPermissionIntent(context, navigationTarget.recording(), navigationTarget.screen())));
            }
        }).doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    private boolean hasMicrophonePermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    @CheckResult
    private Single<NavigationResult> showUpgradeScreen(NavigationTarget navigationTarget) {
        if (featureOperations.upsellHighTier()) {
            List<Intent> taskStack = Collections.singletonList(createHomeIntent(context));
            Intent intent = createConversionIntent(context, UpsellContext.DEFAULT);
            return Single.just(NavigationResult.create(navigationTarget, intent, taskStack))
                         .doOnSuccess(__ -> trackForegroundEvent(navigationTarget.withScreen(Screen.CONVERSION)));
        } else {
            return showHome(navigationTarget);
        }
    }

    @CheckResult
    private Single<NavigationResult> showProductChoiceScreen(NavigationTarget navigationTarget, Plan plan) {
        if (featureOperations.getCurrentPlan().isGoPlan()) {
            return showHome(navigationTarget)
                    .map(result -> result.withToast(context.getString(R.string.product_choice_error_already_subscribed)));
        } else if (featureOperations.upsellBothTiers()) {
            return Single.just(NavigationResult.create(navigationTarget, createProductChoiceIntent(context, plan), Collections.singletonList(createHomeIntent(context))))
                         .doOnSuccess(__ -> trackForegroundEvent(navigationTarget.withScreen(Screen.CONVERSION)));
        } else {
            return showHome(navigationTarget);
        }
    }

    @CheckResult
    private Single<NavigationResult> showMidTierCheckoutScreen(NavigationTarget navigationTarget) {
        if (featureOperations.getCurrentPlan().isGoPlan()) {
            return showHome(navigationTarget)
                    .map(result -> result.withToast(context.getString(R.string.product_choice_error_already_subscribed)));
        } else if (featureOperations.upsellBothTiers()) {
            return Single.just(NavigationResult.create(navigationTarget, IntentFactory.createDirectCheckoutIntent(context, Plan.MID_TIER)))
                         .doOnSuccess(__ -> trackForegroundEvent(navigationTarget.withScreen(Screen.CHECKOUT)));
        } else {
            return showHome(navigationTarget);
        }
    }

    @CheckResult
    private Single<NavigationResult> showHighTierCheckoutScreen(NavigationTarget navigationTarget) {
        if (Plan.HIGH_TIER == featureOperations.getCurrentPlan()) {
            return showHome(navigationTarget)
                    .map(result -> result.withToast(context.getString(R.string.product_choice_error_already_subscribed)));
        } else if (featureOperations.upsellHighTier()) {
            return Single.just(NavigationResult.create(navigationTarget, IntentFactory.createDirectCheckoutIntent(context, Plan.HIGH_TIER)))
                         .doOnSuccess(__ -> trackForegroundEvent(navigationTarget.withScreen(Screen.CHECKOUT)));
        } else {
            return showHome(navigationTarget);
        }
    }

    @CheckResult
    private Single<NavigationResult> showOfflineSettingsScreen(NavigationTarget navigationTarget) {
        if (featureOperations.isOfflineContentEnabled()) {
            return Single.create(emitter -> {
                if (navigationTarget.offlineSettingsMetaData().isPresent() &&
                        navigationTarget.offlineSettingsMetaData().get().showOnboarding() &&
                        !offlineSettingsStorage.hasSeenOfflineSettingsOnboarding()) {
                    trackForegroundEvent(navigationTarget.withScreen(Screen.SETTINGS_AUTOMATIC_SYNC_ONBOARDING));
                    emitter.onSuccess(NavigationResult.create(navigationTarget, createOfflineSettingsOnboardingIntent(context)));
                } else {
                    trackForegroundEvent(navigationTarget.withScreen(Screen.SETTINGS_OFFLINE));
                    emitter.onSuccess(NavigationResult.create(navigationTarget, createOfflineSettingsIntent(context)));
                }
            });
        } else {
            return showHome(navigationTarget);
        }
    }

    @CheckResult
    private Single<NavigationResult> showNotificationPreferencesScreen(NavigationTarget navigationTarget) {
        return Single.<NavigationResult>create(emitter -> {
            if (navigationTarget.notificationPreferencesMetaData().isPresent() && navigationTarget.notificationPreferencesMetaData().get().isNavigationDeeplink()) {
                emitter.onSuccess(NavigationResult.create(navigationTarget, IntentFactory.createNotificationPreferencesIntent(context)));
            } else {
                emitter.onSuccess(NavigationResult.create(navigationTarget, IntentFactory.createNotificationPreferencesFromDeeplinkIntent(context)));
            }
        }).doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> showCollectionScreen(NavigationTarget navigationTarget) {
        return Single.just(NavigationResult.create(navigationTarget, createCollectionIntent()))
                     .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<NavigationResult> shareApp(NavigationTarget navigationTarget) {
        final Uri uri = Uri.parse(navigationTarget.linkNavigationParameters().get().target());
        final String title = uri.getQueryParameter("title");
        final String text = uri.getQueryParameter("text");
        final String path = uri.getQueryParameter("path");
        if (!title.isEmpty() && !text.isEmpty() && !path.isEmpty()) {
            final String textToShare = text + " " + path;
            final Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TITLE, title);
            intent.putExtra(Intent.EXTRA_SUBJECT, title);
            intent.putExtra(Intent.EXTRA_TEXT, textToShare);
            intent.setType("message/rfc822");
            return Single.just(NavigationResult.create(navigationTarget, Intent.createChooser(intent, title)));
        } else {
            return showHome(navigationTarget);
        }
    }

    @CheckResult
    private Single<NavigationResult> showSystemSettings(NavigationTarget navigationTarget) {
        final Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        return Single.just(NavigationResult.create(navigationTarget, intent));
    }

    @CheckResult
    private Single<NavigationResult> startActivityForResource(NavigationTarget navigationTarget, Urn urn) {
        if (isCrawler(navigationTarget.referrer())) {
            loginCrawler();
        }

        final Single<NavigationResult> resultSingle;
        if (accountOperations.isUserLoggedIn()) {
            resultSingle = navigateToResource(navigationTarget, urn);
        } else {
            resultSingle = showOnboardingForUri(navigationTarget);
        }

        return resultSingle
                .doOnSuccess(__ -> {
                    trackForegroundEventForResource(navigationTarget, urn);
                    reportSuccessfulDeeplink(navigationTarget);
                });
    }

    @CheckResult
    private Single<NavigationResult> navigateToResource(NavigationTarget navigationTarget, final Urn urn) {
        if (urn.isTrack()) {
            return startPlayback(navigationTarget, urn);
        } else if (urn.isUser()) {
            return showProfile(navigationTarget, urn);
        } else if (urn.isPlaylist()) {
            return showPlaylist(navigationTarget, urn);
        } else if (urn.isSystemPlaylist()) {
            return showSystemPlaylist(navigationTarget, urn);
        } else if (urn.isArtistStation() || urn.isTrackStation()) {
            return showStation(navigationTarget, urn, Optional.absent());
        } else {
            ErrorUtils.handleSilentException(new IllegalArgumentException("Trying to navigate to unsupported urn: " + urn + " in version: " + BuildConfig.VERSION_CODE));
            return Single.never();
        }
    }

    @CheckResult
    private Single<NavigationResult> startPlayback(NavigationTarget navigationTarget, Urn urn) {
        return playbackInitiator.startPlayback(urn, navigationTarget.screen())
                                .observeOn(AndroidSchedulers.mainThread())
                                .flatMap(playbackResult -> {
                                    if (navigationTarget.screen() == Screen.DEEPLINK) {
                                        if (playbackResult.isSuccess()) {
                                            return Single.just(NavigationResult.create(navigationTarget, createStreamWithExpandedPlayerIntent(navigationTarget.screen())));
                                        } else {
                                            return launchApplicationWithMessage(navigationTarget, R.string.error_loading_url);
                                        }
                                    } else {
                                        return Single.just(NavigationResult.create(navigationTarget, playbackResult));
                                    }
                                });
    }

    @CheckResult
    private Single<NavigationResult> showOnboardingForUri(NavigationTarget navigationTarget) {
        return Single.just(NavigationResult.create(navigationTarget,
                                                   createOnboardingIntent(context, navigationTarget.screen(), Uri.parse(navigationTarget.linkNavigationParameters().get().target()))))
                     .map(result -> result.withToast(context.getString(R.string.error_toast_user_not_logged_in)));
    }

    @CheckResult
    private Single<NavigationResult> launchApplicationWithMessage(NavigationTarget navigationTarget, int messageId) {
        if (navigationTarget.screen() == Screen.DEEPLINK) {
            return Single.just(NavigationResult.create(navigationTarget, createLauncherIntent(context)).withToast(context.getString(messageId)));
        } else {
            return Single.just(NavigationResult.error(navigationTarget));
        }
    }

    private boolean isCrawler(Optional<String> referrer) {
        return Referrer.GOOGLE_CRAWLER.value().equals(referrer.orNull());
    }

    private void loginCrawler() {
        accountOperations.loginCrawlerUser();
        serviceController.resetPlaybackService();
        playQueueManager.clearAll(); // do not leave previous played tracks visible for crawlers
    }

    private boolean shouldShowLogInMessage(DeepLink deepLink, Optional<String> referrer) {
        if (!deepLink.requiresLoggedInUser()) {
            return false;
        }
        if (deepLink.requiresResolve()) {
            return false;
        } else if (isCrawler(referrer)) {
            loginCrawler();
            return false;
        } else if (!accountOperations.isUserLoggedIn()) {
            return true;
        }
        return false;
    }

    private boolean shouldRetryWithFallback(NavigationTarget navigationTarget) {
        return navigationTarget.linkNavigationParameters().isPresent() &&
                navigationTarget.linkNavigationParameters().get().fallback().isPresent() &&
                !navigationTarget.linkNavigationParameters().get().fallback().get().equals(navigationTarget.linkNavigationParameters().get().target());
    }

    private void trackForegroundEventForResource(NavigationTarget navigationTarget, Urn urn) {
        navigationTarget.referrer().ifPresent(referrer -> trackForegroundEvent(ForegroundEvent.open(navigationTarget.screen(), referrer, urn)));
    }

    private void trackForegroundEvent(NavigationTarget navigationTarget) {
        navigationTarget.referrer().ifPresent(referrer -> trackForegroundEvent(ForegroundEvent.open(navigationTarget.screen(), referrer)));
    }

    private void reportSuccessfulDeeplink(NavigationTarget navigationTarget) {
        navigationTarget.referrer().ifPresent(referrer -> eventBus.publish(EventQueue.TRACKING, DeeplinkReportEvent.forResolvedDeeplink(referrer)));
    }

    private void reportFailedToResolveDeeplink(NavigationTarget navigationTarget) {
        navigationTarget.referrer().ifPresent(referrer -> eventBus.publish(EventQueue.TRACKING, DeeplinkReportEvent.forResolutionFailure(referrer)));
    }

    private void trackForegroundEvent(ForegroundEvent event) {
        eventBus.publish(EventQueue.TRACKING, event);
    }

    private void trackNavigationEvent(Optional<UIEvent> event) {
        event.ifPresent(eventTracker::trackNavigation);
    }
}
