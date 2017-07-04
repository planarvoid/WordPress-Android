package com.soundcloud.android.navigation;

import static com.soundcloud.android.navigation.IntentFactory.createActivitiesIntent;
import static com.soundcloud.android.navigation.IntentFactory.createAdClickthroughIntent;
import static com.soundcloud.android.navigation.IntentFactory.createAllGenresIntent;
import static com.soundcloud.android.navigation.IntentFactory.createChartsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createFollowersIntent;
import static com.soundcloud.android.navigation.IntentFactory.createFollowingsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createFullscreenVideoAdIntent;
import static com.soundcloud.android.navigation.IntentFactory.createLikedStationsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createPlaylistIntent;
import static com.soundcloud.android.navigation.IntentFactory.createPlaylistsAndAlbumsCollectionIntent;
import static com.soundcloud.android.navigation.IntentFactory.createPlaylistsCollectionIntent;
import static com.soundcloud.android.navigation.IntentFactory.createPrestititalAdIntent;
import static com.soundcloud.android.navigation.IntentFactory.createProfileAlbumsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createProfileIntent;
import static com.soundcloud.android.navigation.IntentFactory.createProfileLikesIntent;
import static com.soundcloud.android.navigation.IntentFactory.createProfilePlaylistsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createProfileRepostsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createProfileTracksIntent;
import static com.soundcloud.android.navigation.IntentFactory.createSearchIntent;
import static com.soundcloud.android.navigation.IntentFactory.createStationsInfoIntent;
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
import com.soundcloud.android.olddiscovery.DefaultHomeScreenConfiguration;
import com.soundcloud.android.onboarding.auth.SignInOperations;
import com.soundcloud.android.payments.UpsellContext;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.stations.StationsUriResolver;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.utils.UriUtils;
import com.soundcloud.java.checks.Preconditions;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.rx.eventbus.EventBus;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.CheckResult;
import android.widget.Toast;

import javax.inject.Inject;
import javax.inject.Provider;

@SuppressWarnings({"PMD.GodClass", "RestrictedApi"})
public class NavigationResolver {

    private final ResolveOperations resolveOperations;
    private final AccountOperations accountOperations;
    private final PlaybackServiceController serviceController;
    private final PlaybackInitiator playbackInitiator;
    private final PlayQueueManager playQueueManager;
    private final EventBus eventBus;
    private final NavigationExecutor navigationExecutor;
    private final FeatureOperations featureOperations;
    private final ChartsUriResolver chartsUriResolver;
    private final SignInOperations signInOperations;
    private final LocalEntityUriResolver localEntityUriResolver;
    private final StationsUriResolver stationsUriResolver;
    private final ApplicationProperties applicationProperties;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final EventTracker eventTracker;
    private final DefaultHomeScreenConfiguration defaultHomeScreenConfiguration;

    @Inject
    NavigationResolver(ResolveOperations resolveOperations,
                       LocalEntityUriResolver localEntityUriResolver,
                       AccountOperations accountOperations,
                       PlaybackServiceController serviceController,
                       PlaybackInitiator playbackInitiator,
                       PlayQueueManager playQueueManager,
                       EventBus eventBus,
                       NavigationExecutor navigationExecutor,
                       FeatureOperations featureOperations,
                       ChartsUriResolver chartsUriResolver,
                       SignInOperations signInOperations,
                       StationsUriResolver stationsUriResolver,
                       ApplicationProperties applicationProperties,
                       Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                       EventTracker eventTracker,
                       DefaultHomeScreenConfiguration defaultHomeScreenConfiguration) {
        this.resolveOperations = resolveOperations;
        this.accountOperations = accountOperations;
        this.serviceController = serviceController;
        this.playbackInitiator = playbackInitiator;
        this.playQueueManager = playQueueManager;
        this.eventBus = eventBus;
        this.navigationExecutor = navigationExecutor;
        this.featureOperations = featureOperations;
        this.chartsUriResolver = chartsUriResolver;
        this.signInOperations = signInOperations;
        this.localEntityUriResolver = localEntityUriResolver;
        this.stationsUriResolver = stationsUriResolver;
        this.applicationProperties = applicationProperties;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.eventTracker = eventTracker;
        this.defaultHomeScreenConfiguration = defaultHomeScreenConfiguration;
    }

    @CheckResult
    public Single<NavigationResult> resolveNavigationResult(Activity activity, NavigationTarget navigationTarget) {
        if (!navigationTarget.linkNavigationParameters().isPresent() || Strings.isNullOrEmpty(navigationTarget.linkNavigationParameters().get().target())) {
            try {
                if (navigationTarget.deeplink().isPresent()) {
                    return handleDeepLink(activity, navigationTarget, navigationTarget.deeplink().get()).map(action -> NavigationResult.create(navigationTarget, action));
                } else {
                    return showHome(activity, navigationTarget).map(action -> NavigationResult.create(navigationTarget, action));
                }
            } catch (UriResolveException e) {
                handleUriResolveException(activity, e);
                return showHome(activity, navigationTarget).map(action -> NavigationResult.create(navigationTarget, action));
            }
        } else {
            final Uri hierarchicalUri = UriUtils.convertToHierarchicalUri(Uri.parse(navigationTarget.linkNavigationParameters().get().target()));
            NavigationTarget newTarget = navigationTarget.withTarget(hierarchicalUri.toString());
            try {
                if (localEntityUriResolver.canResolveLocally(newTarget.linkNavigationParameters().get().target())) {
                    return resolveLocal(activity, navigationTarget, newTarget);
                } else if (localEntityUriResolver.isKnownDeeplink(newTarget.linkNavigationParameters().get().target())) {
                    return resolveDeeplink(activity, hierarchicalUri, newTarget);
                } else {
                    return resolveTarget(activity, navigationTarget).map(action -> NavigationResult.create(navigationTarget, action));
                }
            } catch (UriResolveException e) {
                handleUriResolveException(activity, e);
                return resolveTarget(activity, navigationTarget).map(action -> NavigationResult.create(navigationTarget, action));
            }
        }
    }

    private void handleUriResolveException(Activity activity, UriResolveException e) {
        final String msg = "Local resolve failed";
        if (applicationProperties.isDebuggableFlavor()) {
            AndroidUtils.showToast(activity, msg);
        }
        ErrorUtils.handleSilentException(msg, e);
    }

    @CheckResult
    private Single<NavigationResult> resolveDeeplink(Activity activity, Uri hierarchicalUri, NavigationTarget newTarget) throws UriResolveException {
        final DeepLink deepLink = DeepLink.fromUri(hierarchicalUri);
        if (shouldShowLogInMessage(deepLink, newTarget.referrer())) {
            return showOnboardingForDeeplink(activity, newTarget).map(action -> NavigationResult.create(newTarget, action));
        } else {
            return handleDeepLink(activity, newTarget, deepLink).map(action -> NavigationResult.create(newTarget, action));
        }
    }

    @CheckResult
    private Single<NavigationResult> resolveLocal(Activity activity, NavigationTarget navigationTarget, NavigationTarget newTarget) throws UriResolveException {
        return localEntityUriResolver.resolve(newTarget.linkNavigationParameters().get().target())
                                     .observeOn(AndroidSchedulers.mainThread())
                                     .flatMap(urn -> startActivityForResource(activity, navigationTarget, urn).map(action -> NavigationResult.create(navigationTarget, action, urn)));
    }

    @CheckResult
    private Single<Action> showOnboardingForDeeplink(Activity activity, NavigationTarget navigationTarget) {
        return showOnboardingForUri(activity, navigationTarget)
                .doOnSuccess(ignore -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<Action> handleResolveResult(Activity activity, ResolveResult resolveResult, NavigationTarget navigationTarget) {
        if (resolveResult.success() && localEntityUriResolver.canResolveLocally(resolveResult.urn().get())) {
            return startActivityForResource(activity, navigationTarget, resolveResult.urn().get());
        } else {
            return handleUnsuccessfulResolve(activity, navigationTarget, resolveResult);
        }
    }

    @CheckResult
    private Single<Action> handleUnsuccessfulResolve(Activity activity, NavigationTarget navigationTarget, ResolveResult resolveResult) {
        final Optional<String> errorUrl = resolveResult.uri().transform(Uri::toString);
        final NavigationTarget fallbackAwareTarget = navigationTarget.withFallback(navigationTarget.linkNavigationParameters().get().fallback().or(errorUrl));
        if (shouldRetryWithFallback(fallbackAwareTarget)) {
            if (applicationProperties.isDebuggableFlavor()) {
                AndroidUtils.showToast(activity, "Retry resolve with fallback");
            }
            final Exception e = resolveResult.exception().or(new UriResolveException("Resolve with fallback"));
            ErrorUtils.handleSilentException("Resolve uri " + navigationTarget.linkNavigationParameters().get().target() + " with fallback " + fallbackAwareTarget.linkNavigationParameters()
                                                                                                                                                                  .get()
                                                                                                                                                                  .fallback()
                                                                                                                                                                  .orNull(), e);
            return resolveNavigationResult(activity, fallbackAwareTarget.withTarget(fallbackAwareTarget.linkNavigationParameters().get().fallback().get())
                                                              .withFallback(Optional.absent()))
                    .map(NavigationResult::action);
        } else {
            trackForegroundEvent(fallbackAwareTarget);
            Optional<Exception> exception = resolveResult.exception();
            if (exception.isPresent() && !ErrorUtils.isNetworkError(exception.get())) {
                ErrorUtils.handleSilentException("unable to load deeplink:" + errorUrl, exception.get());
                reportFailedToResolveDeeplink(fallbackAwareTarget);
            }
            return launchApplicationWithMessage(activity, fallbackAwareTarget, R.string.error_unknown_navigation);
        }
    }

    @CheckResult
    private Single<Action> handleDeepLink(Activity activity, NavigationTarget navigationTarget, DeepLink deepLink) throws UriResolveException {
        switch (deepLink) {
            case HOME:
                return showHome(activity, navigationTarget);
            case STREAM:
                return showStream(activity, navigationTarget);
            case RECORD:
                return showRecordScreen(activity, navigationTarget);
            case DISCOVERY:
                return showDiscoveryScreen(activity, navigationTarget);
            case CHARTS:
                return showCharts(activity, navigationTarget);
            case CHARTS_ALL_GENRES:
                return showAllGenresCharts(activity, navigationTarget);
            case LIKED_STATIONS:
                return showLikedStations(activity, navigationTarget);
            case STATION:
                return showStation(activity, navigationTarget);
            case SEARCH:
                return showSearchScreen(activity, navigationTarget);
            case SEARCH_AUTOCOMPLETE:
                return showSearchAutocompleteScreen(activity, navigationTarget);
            case SEARCH_RESULTS_VIEW_ALL:
                return showSearchResultViewAllScreen(activity, navigationTarget);
            case WEB_VIEW:
                return startWebView(activity, navigationTarget);
            case SOUNDCLOUD_GO_PLUS_UPSELL:
                return showUpgradeScreen(activity, navigationTarget);
            case SOUNDCLOUD_GO_BUY:
                return showMidTierCheckoutScreen(activity, navigationTarget);
            case SOUNDCLOUD_GO_PLUS_BUY:
                return showHighTierCheckoutScreen(activity, navigationTarget);
            case SOUNDCLOUD_GO_CHOICE:
                return showProductChoiceScreen(activity, navigationTarget, Plan.MID_TIER);
            case SOUNDCLOUD_GO_PLUS_CHOICE:
                return showProductChoiceScreen(activity, navigationTarget, Plan.HIGH_TIER);
            case OFFLINE_SETTINGS:
                return showOfflineSettingsScreen(activity, navigationTarget);
            case NOTIFICATION_PREFERENCES:
                return showNotificationPreferencesScreen(activity, navigationTarget);
            case COLLECTION:
                return showCollectionScreen(activity, navigationTarget);
            case SHARE_APP:
                return shareApp(activity, navigationTarget);
            case SYSTEM_SETTINGS:
                return showSystemSettings(activity);
            case REMOTE_SIGN_IN:
                return startWebViewForRemoteSignIn(activity, navigationTarget);
            case THE_UPLOAD:
                return startTheUpload(activity, navigationTarget);
            case UNKNOWN:
                return startExternal(activity, navigationTarget);
            case ACTIVITIES:
                return showActivities(activity, navigationTarget);
            case FOLLOWERS:
                return showFollowers(activity, navigationTarget);
            case FOLLOWINGS:
                return showFollowings(activity, navigationTarget);
            case AD_FULLSCREEN_VIDEO:
                return showFullscreenVideoAd(activity, navigationTarget);
            case AD_PRESTITIAL:
                return showPrestitialAd(activity, navigationTarget);
            case AD_CLICKTHROUGH:
                return showAdClickthrough(activity, navigationTarget);
            case PROFILE:
                return showProfile(activity, navigationTarget);
            case PROFILE_REPOSTS:
                return showProfileReposts(activity, navigationTarget);
            case PROFILE_TRACKS:
                return showProfileTracks(activity, navigationTarget);
            case PROFILE_LIKES:
                return showProfileLikes(activity, navigationTarget);
            case PROFILE_ALBUMS:
                return showProfileAlbums(activity, navigationTarget);
            case PROFILE_PLAYLISTS:
                return showProfilePlaylists(activity, navigationTarget);
            case SYSTEM_PLAYLIST:
                return showSystemPlaylist(activity, navigationTarget);
            case PLAYLISTS_AND_ALBUMS_COLLECTION:
                return showPlaylistsAndAlbumsCollection(activity, navigationTarget);
            case PLAYLISTS_COLLECTION:
                return showPlaylistsCollection(activity, navigationTarget);
            case PLAYLISTS:
                return showPlaylist(activity, navigationTarget);
            default:
                return resolveTarget(activity, navigationTarget);
        }
    }

    @CheckResult
    private Single<Action> showProfile(Activity activity, NavigationTarget navigationTarget) {
        return showProfile(activity, navigationTarget, navigationTarget.targetUrn().get());
    }

    @CheckResult
    private Single<Action> showProfile(Activity activity, NavigationTarget navigationTarget, Urn urn) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            trackNavigationEvent(navigationTarget.uiEvent());
            activity.startActivity(createProfileIntent(activity,
                                                       urn,
                                                       Optional.of(navigationTarget.screen()),
                                                       navigationTarget.searchQuerySourceInfo(),
                                                       navigationTarget.referrer().transform(Referrer::fromOrigin)));
        });
    }

    @CheckResult
    private Single<Action> showProfileReposts(Activity activity, NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            activity.startActivity(createProfileRepostsIntent(activity, navigationTarget.targetUrn().get(), navigationTarget.screen(), navigationTarget.searchQuerySourceInfo()));
        });
    }

    @CheckResult
    private Single<Action> showProfileTracks(Activity activity, NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            activity.startActivity(createProfileTracksIntent(activity, navigationTarget.targetUrn().get(), navigationTarget.screen(), navigationTarget.searchQuerySourceInfo()));
        });
    }

    @CheckResult
    private Single<Action> showProfileLikes(Activity activity, NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            activity.startActivity(createProfileLikesIntent(activity, navigationTarget.targetUrn().get(), navigationTarget.screen(), navigationTarget.searchQuerySourceInfo()));
        });
    }

    @CheckResult
    private Single<Action> showProfileAlbums(Activity activity, NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            activity.startActivity(createProfileAlbumsIntent(activity, navigationTarget.targetUrn().get(), navigationTarget.screen(), navigationTarget.searchQuerySourceInfo()));
        });
    }


    @CheckResult
    private Single<Action> showProfilePlaylists(Activity activity, NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            activity.startActivity(createProfilePlaylistsIntent(activity, navigationTarget.targetUrn().get(), navigationTarget.screen(), navigationTarget.searchQuerySourceInfo()));
        });
    }

    @CheckResult
    private Single<Action> startExternal(Activity activity, NavigationTarget navigationTarget) {
        trackForegroundEvent(navigationTarget);
        String target = navigationTarget.linkNavigationParameters().get().target();
        Preconditions.checkNotNull(target, "Covered by #resolve");
        Uri targetUri = Uri.parse(target);
        final String identifier = Optional.fromNullable(targetUri.getAuthority()).or(targetUri.getPath());
        if (ScTextUtils.isEmail(identifier)) {
            return Single.just(() -> navigationExecutor.openEmail(activity, identifier));
        } else {
            return Single.just(() -> navigationExecutor.openExternal(activity, navigationTarget.linkNavigationParameters().get().targetUri()));
        }
    }

    @CheckResult
    private Single<Action> showActivities(Activity activity, NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            activity.startActivity(createActivitiesIntent(activity));
        });
    }

    @CheckResult
    private Single<Action> showFollowers(Activity activity, NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            activity.startActivity(createFollowersIntent(activity, navigationTarget.targetUrn().get(), navigationTarget.searchQuerySourceInfo()));
        });
    }

    @CheckResult
    private Single<Action> showFollowings(Activity activity, NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            Intent followingsIntent = createFollowingsIntent(activity, navigationTarget.targetUrn().get(), navigationTarget.searchQuerySourceInfo());
            activity.startActivity(followingsIntent);
        });
    }

    @CheckResult
    private Single<Action> showFullscreenVideoAd(Activity activity, NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            activity.startActivity(createFullscreenVideoAdIntent(activity, navigationTarget.targetUrn().get()));
        });
    }

    @CheckResult
    private Single<Action> showPrestitialAd(Activity activity, NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            activity.startActivity(createPrestititalAdIntent(activity));
        });
    }

    @CheckResult
    private Single<Action> showAdClickthrough(Activity activity, NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            activity.startActivity(createAdClickthroughIntent(Uri.parse(navigationTarget.deeplinkTarget().get())));
        });
    }

    @CheckResult
    private Single<Action> showSystemPlaylist(Activity activity, NavigationTarget navigationTarget) {
        return showSystemPlaylist(activity, navigationTarget, navigationTarget.targetUrn().get());
    }

    @CheckResult
    private Single<Action> showSystemPlaylist(Activity activity, NavigationTarget navigationTarget, Urn urn) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            activity.startActivity(createSystemPlaylistIntent(activity, urn, navigationTarget.screen()));
        });
    }

    @CheckResult
    private Single<Action> showPlaylistsAndAlbumsCollection(Activity activity, NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            activity.startActivity(createPlaylistsAndAlbumsCollectionIntent(activity));
        });
    }

    @CheckResult
    private Single<Action> showPlaylistsCollection(Activity activity, NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            activity.startActivity(createPlaylistsCollectionIntent(activity));
        });
    }

    @CheckResult
    private Single<Action> showPlaylist(Activity activity, NavigationTarget navigationTarget) {
        return showPlaylist(activity, navigationTarget, navigationTarget.targetUrn().get());
    }

    @CheckResult
    private Single<Action> showPlaylist(Activity activity, NavigationTarget navigationTarget, Urn urn) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            trackNavigationEvent(navigationTarget.uiEvent());
            activity.startActivity(createPlaylistIntent(urn,
                                                                           navigationTarget.screen(),
                                                                           navigationTarget.searchQuerySourceInfo(),
                                                                           navigationTarget.promotedSourceInfo()));
        });
    }

    @CheckResult
    private Single<Action> startWebViewForRemoteSignIn(Activity activity, NavigationTarget navigationTarget) {
        final Uri target;
        if (DeepLink.isWebScheme(navigationTarget.linkNavigationParameters().get().targetUri())) {
            target = signInOperations.generateRemoteSignInUri(navigationTarget.linkNavigationParameters().get().targetUri().getPath());
        } else {
            target = signInOperations.generateRemoteSignInUri();
        }

        return Single.<Action>just(() -> navigationExecutor.openRemoteSignInWebView(activity, target))
                .doOnSuccess(t -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<Action> startTheUpload(Activity activity, NavigationTarget navigationTarget) {
        return Single.<Action>just(() -> navigationExecutor.openNewForYou(activity))
                .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<Action> resolveTarget(Activity activity, NavigationTarget navigationTarget) {
        String target = navigationTarget.linkNavigationParameters().get().target();
        Preconditions.checkNotNull(target, "Covered by #resolve");
        return resolveOperations.resolve(target)
                                .observeOn(AndroidSchedulers.mainThread())
                                .flatMap(result -> handleResolveResult(activity, result, navigationTarget));
    }

    @CheckResult
    private Single<Action> startWebView(Activity activity, NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            navigationExecutor.openWebView(activity, navigationTarget.linkNavigationParameters().get().targetUri());
        });
    }

    @CheckResult
    private Single<Action> showHome(Activity activity, NavigationTarget navigationTarget) {
        if (defaultHomeScreenConfiguration.isStreamHome()) {
            return showStream(activity, navigationTarget);
        } else {
            return showDiscoveryScreen(activity, navigationTarget);
        }
    }

    @CheckResult
    private Single<Action> showStream(Activity activity, NavigationTarget navigationTarget) {
        return Single.just(() -> {
            accountOperations.clearCrawler();
            trackForegroundEvent(navigationTarget);
            navigationExecutor.openStream(activity, navigationTarget.screen());
        });
    }

    @CheckResult
    private Single<Action> showDiscoveryScreen(Activity activity, NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            navigationExecutor.openDiscovery(activity, navigationTarget.screen());
        });
    }

    @CheckResult
    private Single<Action> showTrackRecommendationsScreen(Activity activity, NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            navigationExecutor.openViewAllRecommendations(activity);
        });
    }

    @CheckResult
    private Single<Action> showCharts(Activity activity, NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            Optional<NavigationTarget.ChartsMetaData> chartsMetaData = navigationTarget.chartsMetaData();
            final ChartDetails chartDetails;
            if (chartsMetaData.isPresent()) {
                chartDetails = chartsMetaData.get().chartDetails().get();
            } else {
                chartDetails = chartsUriResolver.resolveUri(navigationTarget.linkNavigationParameters().get().targetUri());
            }
            activity.startActivity(createChartsIntent(activity, chartDetails));
        });
    }

    @CheckResult
    private Single<Action> showAllGenresCharts(Activity activity, NavigationTarget navigationTarget) {
            return Single.just(() -> {
                trackForegroundEvent(navigationTarget);
                Optional<NavigationTarget.ChartsMetaData> chartsMetaData = navigationTarget.chartsMetaData();
                ChartCategory category;
                if (chartsMetaData.isPresent()) {
                    category = chartsMetaData.get().category().orNull();
                } else {
                    category = AllGenresUriResolver.resolveUri(navigationTarget.linkNavigationParameters().get().targetUri());
                }
                activity.startActivity(createAllGenresIntent(activity, category));
            });
    }

    @CheckResult
    private Single<Action> showLikedStations(Activity activity, NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            activity.startActivity(createLikedStationsIntent(activity));
        });
    }

    @CheckResult
    private Single<Action> showStation(Activity activity, NavigationTarget navigationTarget) throws UriResolveException {
        Optional<NavigationTarget.StationsInfoMetaData> stationsInfoMetaData = navigationTarget.stationsInfoMetaData();
        if (stationsInfoMetaData.isPresent()) {
            return showStation(activity, navigationTarget, navigationTarget.targetUrn().get(), stationsInfoMetaData.get().seedTrack());
        } else {
            Optional<Urn> urn = stationsUriResolver.resolve(navigationTarget.linkNavigationParameters().get().targetUri());
            if (urn.isPresent()) {
                return showStation(activity, navigationTarget, urn.get(), Optional.absent());
            } else {
                throw new UriResolveException("Station " + navigationTarget.linkNavigationParameters().get().target() + " could not be resolved locally");
            }
        }
    }

    @CheckResult
    private Single<Action> showStation(Activity activity, NavigationTarget navigationTarget, Urn urn, Optional<Urn> seedTrack) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            trackNavigationEvent(navigationTarget.uiEvent());
            activity.startActivity(createStationsInfoIntent(activity, urn, seedTrack, navigationTarget.discoverySource().or(Optional.of(DiscoverySource.DEEPLINK))));
        });
    }

    @CheckResult
    private Single<Action> showSearchScreen(Activity activity, NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            navigationExecutor.openSearch(activity, navigationTarget.linkNavigationParameters().get().targetUri(), navigationTarget.screen());
        });
    }

    @CheckResult
    private Single<Action> showSearchAutocompleteScreen(Activity activity, NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            activity.startActivity(createSearchIntent(activity));
        });
    }

    @CheckResult
    private Single<Action> showSearchResultViewAllScreen(Activity activity, NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            navigationExecutor.openSearchViewAll(activity, navigationTarget);
        });
    }

    @CheckResult
    private Single<Action> showRecordScreen(Activity activity, NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            navigationExecutor.openRecord(activity, navigationTarget.screen());
        });
    }

    @CheckResult
    private Single<Action> showUpgradeScreen(Activity activity, NavigationTarget navigationTarget) {
        if (featureOperations.upsellHighTier()) {
            return Single.just(() -> {
                trackForegroundEvent(navigationTarget.withScreen(Screen.CONVERSION));
                navigationExecutor.openUpgradeOnMain(activity, UpsellContext.DEFAULT);
            });
        } else {
            return showHome(activity, navigationTarget);
        }
    }

    @CheckResult
    private Single<Action> showProductChoiceScreen(Activity activity, NavigationTarget navigationTarget, Plan plan) {
        if (featureOperations.getCurrentPlan().isGoPlan()) {
            return showHome(activity, navigationTarget)
                    .doOnSuccess(__ -> AndroidUtils.showToast(activity, R.string.product_choice_error_already_subscribed, Toast.LENGTH_SHORT));
        } else if (featureOperations.upsellBothTiers()) {
            return Single.just(() -> {
                trackForegroundEvent(navigationTarget.withScreen(Screen.CONVERSION));
                navigationExecutor.openProductChoiceOnMain(activity, plan);
            });
        } else {
            return showHome(activity, navigationTarget);
        }
    }

    @CheckResult
    private Single<Action> showMidTierCheckoutScreen(Activity activity, NavigationTarget navigationTarget) {
        if (featureOperations.getCurrentPlan().isGoPlan()) {
            return showHome(activity, navigationTarget)
                    .doOnSuccess(__ -> AndroidUtils.showToast(activity, R.string.product_choice_error_already_subscribed, Toast.LENGTH_SHORT));
        } else if (featureOperations.upsellBothTiers()) {
            return Single.just(() -> {
                trackForegroundEvent(navigationTarget.withScreen(Screen.CHECKOUT));
                navigationExecutor.openDirectCheckout(activity, Plan.MID_TIER);
            });
        } else {
            return showHome(activity, navigationTarget);
        }
    }

    @CheckResult
    private Single<Action> showHighTierCheckoutScreen(Activity activity, NavigationTarget navigationTarget) {
        if (Plan.HIGH_TIER == featureOperations.getCurrentPlan()) {
            return showHome(activity, navigationTarget)
                    .doOnSuccess(__ -> AndroidUtils.showToast(activity, R.string.product_choice_error_already_subscribed, Toast.LENGTH_SHORT));
        } else if (featureOperations.upsellHighTier()) {
            return Single.just(() -> {
                trackForegroundEvent(navigationTarget.withScreen(Screen.CHECKOUT));
                navigationExecutor.openDirectCheckout(activity, Plan.HIGH_TIER);
            });
        } else {
            return showHome(activity, navigationTarget);
        }
    }

    @CheckResult
    private Single<Action> showOfflineSettingsScreen(Activity activity, NavigationTarget navigationTarget) {
        if (featureOperations.isOfflineContentEnabled()) {
            return Single.just(() -> {
                trackForegroundEvent(navigationTarget.withScreen(Screen.SETTINGS_OFFLINE));
                navigationExecutor.openOfflineSettings(activity);
            });
        } else {
            return showHome(activity, navigationTarget);
        }
    }

    @CheckResult
    private Single<Action> showNotificationPreferencesScreen(Activity activity, NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            navigationExecutor.openNotificationPreferencesFromDeeplink(activity);
        });
    }

    @CheckResult
    private Single<Action> showCollectionScreen(Activity activity, NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            navigationExecutor.openCollection(activity);
        });
    }

    @CheckResult
    private Single<Action> shareApp(Activity activity, NavigationTarget navigationTarget) {
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
            return Single.just(() -> activity.startActivity(Intent.createChooser(intent, title)));
        } else {
            return showHome(activity, navigationTarget);
        }
    }

    @CheckResult
    private Single<Action> showSystemSettings(Activity activity) {
        final Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setData(Uri.parse("package:" + activity.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        return Single.just(() -> activity.startActivity(intent));
    }

    @CheckResult
    private Single<Action> startActivityForResource(Activity activity, NavigationTarget navigationTarget, Urn urn) {
        if (isCrawler(navigationTarget.referrer())) {
            loginCrawler();
        }

        final Single<Action> resultSingle;
        if (accountOperations.isUserLoggedIn()) {
            resultSingle = navigateToResource(activity, navigationTarget, urn);
        } else {
            resultSingle = showOnboardingForUri(activity, navigationTarget);
        }

        return resultSingle
                .doOnSuccess(__ -> {
                    trackForegroundEventForResource(navigationTarget, urn);
                    reportSuccessfulDeeplink(navigationTarget);
                });
    }

    @CheckResult
    private Single<Action> navigateToResource(Activity activity, NavigationTarget navigationTarget, final Urn urn) {
        if (urn.isTrack()) {
            return startPlayback(activity, navigationTarget, urn);
        } else if (urn.isUser()) {
            return showProfile(activity, navigationTarget, urn);
        } else if (urn.isPlaylist()) {
            return showPlaylist(activity, navigationTarget, urn);
        } else if (urn.isSystemPlaylist()) {
            return showSystemPlaylist(activity, navigationTarget, urn);
        } else if (urn.isArtistStation() || urn.isTrackStation()) {
            return showStation(activity, navigationTarget, urn, Optional.absent());
        } else {
            ErrorUtils.handleSilentException(new IllegalArgumentException("Trying to navigate to unsupported urn: " + urn + " in version: " + BuildConfig.VERSION_CODE));
            return Single.never();
        }
    }

    @CheckResult
    private Single<Action> startPlayback(Activity activity, NavigationTarget navigationTarget, Urn urn) {
        return playbackInitiator.startPlayback(urn, navigationTarget.screen())
                                .observeOn(AndroidSchedulers.mainThread())
                                .flatMap(playbackResult -> {
                                    if (navigationTarget.screen() == Screen.DEEPLINK) {
                                        if (playbackResult.isSuccess()) {
                                            return Single.just(() -> navigationExecutor.openStreamWithExpandedPlayer(activity, navigationTarget.screen()));
                                        } else {
                                            return launchApplicationWithMessage(activity, navigationTarget, R.string.error_loading_url);
                                        }
                                    } else {
                                        return Single.just(() -> expandPlayerSubscriberProvider.get().onNext(playbackResult));
                                    }
                                });
    }

    @CheckResult
    private Single<Action> showOnboardingForUri(Activity activity, NavigationTarget navigationTarget) {
        return Single.just(() -> {
            AndroidUtils.showToast(activity, R.string.error_toast_user_not_logged_in);
            navigationExecutor.openOnboarding(activity, Uri.parse(navigationTarget.linkNavigationParameters().get().target()), navigationTarget.screen());
        });
    }

    @CheckResult
    private Single<Action> launchApplicationWithMessage(Activity activity, NavigationTarget navigationTarget, int messageId) {
        return Single.just(() -> {
            AndroidUtils.showToast(activity, messageId);
            if (navigationTarget.screen() == Screen.DEEPLINK) {
                navigationExecutor.openLauncher(activity);
            }
        });
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
