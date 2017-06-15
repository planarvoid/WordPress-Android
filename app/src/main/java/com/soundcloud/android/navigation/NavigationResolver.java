package com.soundcloud.android.navigation;

import static com.soundcloud.android.navigation.IntentFactory.createActivitiesIntent;
import static com.soundcloud.android.navigation.IntentFactory.createFollowersIntent;
import static com.soundcloud.android.navigation.IntentFactory.createFollowingsIntent;
import static com.soundcloud.android.navigation.IntentFactory.createProfileIntent;

import com.soundcloud.android.BuildConfig;
import com.soundcloud.android.PlaybackServiceController;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.Referrer;
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
import com.soundcloud.android.onboarding.auth.SignInOperations;
import com.soundcloud.android.payments.UpsellContext;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.stations.StartStationHandler;
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

import android.content.Context;
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
    private final StartStationHandler startStationHandler;
    private final StationsUriResolver stationsUriResolver;
    private final ApplicationProperties applicationProperties;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final EventTracker eventTracker;

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
                       StartStationHandler startStationHandler,
                       StationsUriResolver stationsUriResolver,
                       ApplicationProperties applicationProperties,
                       Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                       EventTracker eventTracker) {
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
        this.startStationHandler = startStationHandler;
        this.stationsUriResolver = stationsUriResolver;
        this.applicationProperties = applicationProperties;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.eventTracker = eventTracker;
    }

    @CheckResult
    public Single<NavigationResult> resolveNavigationResult(NavigationTarget navigationTarget) {
        String target = navigationTarget.target();
        if (Strings.isNullOrEmpty(target)) {
            try {
                if (navigationTarget.deeplink().isPresent()) {
                    return handleDeepLink(navigationTarget, navigationTarget.deeplink().get()).map(action -> NavigationResult.create(navigationTarget, action));
                } else {
                    return showStream(navigationTarget).map(action -> NavigationResult.create(navigationTarget, action));
                }
            } catch (UriResolveException e) {
                handleUriResolveException(navigationTarget, e);
                return showStream(navigationTarget).map(action -> NavigationResult.create(navigationTarget, action));
            }
        } else {
            final Uri hierarchicalUri = UriUtils.convertToHierarchicalUri(Uri.parse(navigationTarget.target()));
            NavigationTarget newTarget = navigationTarget.withTarget(hierarchicalUri.toString());
            try {
                if (localEntityUriResolver.canResolveLocally(newTarget.target())) {
                    return resolveLocal(navigationTarget, newTarget);
                } else if (localEntityUriResolver.isKnownDeeplink(newTarget.target())) {
                    return resolveDeeplink(hierarchicalUri, newTarget);
                } else {
                    return resolveTarget(navigationTarget).map(action -> NavigationResult.create(navigationTarget, action));
                }
            } catch (UriResolveException e) {
                handleUriResolveException(navigationTarget, e);
                return resolveTarget(navigationTarget).map(action -> NavigationResult.create(navigationTarget, action));
            }
        }
    }

    private void handleUriResolveException(NavigationTarget navigationTarget, UriResolveException e) {
        final String msg = "Local resolve failed";
        if (applicationProperties.isDebuggableFlavor()) {
            AndroidUtils.showToast(navigationTarget.activity(), msg);
        }
        ErrorUtils.handleSilentException(msg, e);
    }

    @CheckResult
    private Single<NavigationResult> resolveDeeplink(Uri hierarchicalUri, NavigationTarget newTarget) throws UriResolveException {
        final DeepLink deepLink = DeepLink.fromUri(hierarchicalUri);
        if (shouldShowLogInMessage(deepLink, newTarget.referrer())) {
            return showOnboardingForDeeplink(newTarget).map(action -> NavigationResult.create(newTarget, action));
        } else {
            return handleDeepLink(newTarget, deepLink).map(action -> NavigationResult.create(newTarget, action));
        }
    }

    @CheckResult
    private Single<NavigationResult> resolveLocal(NavigationTarget navigationTarget, NavigationTarget newTarget) throws UriResolveException {
        return localEntityUriResolver.resolve(newTarget.target())
                                     .observeOn(AndroidSchedulers.mainThread())
                                     .flatMap(urn -> startActivityForResource(navigationTarget, urn).map(action -> NavigationResult.create(navigationTarget, action, urn)));
    }

    @CheckResult
    private Single<Action> showOnboardingForDeeplink(NavigationTarget navigationTarget) {
        return showOnboardingForUri(navigationTarget)
                .doOnSuccess(ignore -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<Action> handleResolveResult(ResolveResult resolveResult, NavigationTarget navigationTarget) {
        if (resolveResult.success() && localEntityUriResolver.canResolveLocally(resolveResult.urn().get())) {
            return startActivityForResource(navigationTarget, resolveResult.urn().get());
        } else {
            return handleUnsuccessfulResolve(navigationTarget, resolveResult);
        }
    }

    @CheckResult
    private Single<Action> handleUnsuccessfulResolve(NavigationTarget navigationTarget, ResolveResult resolveResult) {
        final Optional<String> errorUrl = resolveResult.uri().transform(Uri::toString);
        final NavigationTarget fallbackAwareTarget = navigationTarget.withFallback(navigationTarget.fallback().or(errorUrl));
        if (shouldRetryWithFallback(fallbackAwareTarget)) {
            if (applicationProperties.isDebuggableFlavor()) {
                AndroidUtils.showToast(navigationTarget.activity(), "Retry resolve with fallback");
            }
            final Exception e = resolveResult.exception().or(new UriResolveException("Resolve with fallback"));
            ErrorUtils.handleSilentException("Resolve uri " + navigationTarget.target() + " with fallback " + fallbackAwareTarget.fallback().orNull(), e);
            return resolveNavigationResult(fallbackAwareTarget.withTarget(fallbackAwareTarget.fallback().get())
                                                              .withFallback(Optional.absent()))
                    .map(NavigationResult::action);
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
    private Single<Action> handleDeepLink(NavigationTarget navigationTarget, DeepLink deepLink) throws UriResolveException {
        switch (deepLink) {
            case HOME:
            case STREAM:
                return showStream(navigationTarget);
            case RECORD:
                return showRecordScreen(navigationTarget);
            case DISCOVERY:
                return showDiscoveryScreen(navigationTarget);
            case TRACK_RECOMMENDATIONS:
                return showTrackRecommendationsScreen(navigationTarget);
            case CHARTS:
                return showCharts(navigationTarget);
            case CHARTS_ALL_GENRES:
                return showAllGenresCharts(navigationTarget);
            case STATION:
                return showStation(navigationTarget);
            case SEARCH:
                return showSearchScreen(navigationTarget);
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
            case PROFILE:
                return showProfile(navigationTarget);
            default:
                return resolveTarget(navigationTarget);
        }
    }

    private Single<Action> showProfile(NavigationTarget navigationTarget) {
        return showProfile(navigationTarget, navigationTarget.targetUrn().get());
    }

    private Single<Action> showProfile(NavigationTarget navigationTarget, Urn urn) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            trackNavigationEvent(navigationTarget.uiEvent());
            Context context = navigationTarget.activity();
            context.startActivity(createProfileIntent(context,
                                                      urn,
                                                      Optional.of(navigationTarget.screen()),
                                                      navigationTarget.searchQuerySourceInfo(),
                                                      navigationTarget.referrer().transform(Referrer::fromOrigin)));
        });
    }

    @CheckResult
    private Single<Action> startExternal(NavigationTarget navigationTarget) {
        trackForegroundEvent(navigationTarget);
        String target = navigationTarget.target();
        Preconditions.checkNotNull(target, "Covered by #resolve");
        Uri targetUri = Uri.parse(target);
        final String identifier = Optional.fromNullable(targetUri.getAuthority()).or(targetUri.getPath());
        if (ScTextUtils.isEmail(identifier)) {
            return Single.just(() -> navigationExecutor.openEmail(navigationTarget.activity(), identifier));
        } else {
            return Single.just(() -> navigationExecutor.openExternal(navigationTarget.activity(), navigationTarget.targetUri()));
        }
    }

    @CheckResult
    private Single<Action> showActivities(NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            Context context = navigationTarget.activity();
            context.startActivity(createActivitiesIntent(context));
        });
    }

    @CheckResult
    private Single<Action> showFollowers(NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            navigationTarget.activity().startActivity(createFollowersIntent(navigationTarget.activity(), navigationTarget.targetUrn().get(), navigationTarget.searchQuerySourceInfo()));
        });
    }

    @CheckResult
    private Single<Action> showFollowings(NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            Intent followingsIntent = createFollowingsIntent(navigationTarget.activity(), navigationTarget.targetUrn().get(), navigationTarget.searchQuerySourceInfo());
            navigationTarget.activity().startActivity(followingsIntent);
        });
    }

    @CheckResult
    private Single<Action> startWebViewForRemoteSignIn(NavigationTarget navigationTarget) {
        final Uri target;
        if (DeepLink.isWebScheme(navigationTarget.targetUri())) {
            target = signInOperations.generateRemoteSignInUri(navigationTarget.targetUri().getPath());
        } else {
            target = signInOperations.generateRemoteSignInUri();
        }

        return Single.<Action>just(() -> navigationExecutor.openRemoteSignInWebView(navigationTarget.activity(), target))
                .doOnSuccess(t -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<Action> startTheUpload(NavigationTarget navigationTarget) {
        return Single.<Action>just(() -> navigationExecutor.openNewForYou(navigationTarget.activity()))
                .doOnSuccess(__ -> trackForegroundEvent(navigationTarget));
    }

    @CheckResult
    private Single<Action> resolveTarget(NavigationTarget navigationTarget) {
        String target = navigationTarget.target();
        Preconditions.checkNotNull(target, "Covered by #resolve");
        return resolveOperations.resolve(target)
                                .observeOn(AndroidSchedulers.mainThread())
                                .flatMap(result -> handleResolveResult(result, navigationTarget));
    }

    @CheckResult
    private Single<Action> startWebView(NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            navigationExecutor.openWebView(navigationTarget.activity(), navigationTarget.targetUri());
        });
    }

    @CheckResult
    private Single<Action> showStream(NavigationTarget navigationTarget) {
        return Single.just(() -> {
            accountOperations.clearCrawler();
            trackForegroundEvent(navigationTarget);
            navigationExecutor.openStream(navigationTarget.activity(), navigationTarget.screen());
        });
    }

    @CheckResult
    private Single<Action> showDiscoveryScreen(NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            navigationExecutor.openDiscovery(navigationTarget.activity(), navigationTarget.screen());
        });
    }

    @CheckResult
    private Single<Action> showTrackRecommendationsScreen(NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            navigationExecutor.openViewAllRecommendations(navigationTarget.activity());
        });
    }

    @CheckResult
    private Single<Action> showCharts(NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            ChartDetails chartDetails = chartsUriResolver.resolveUri(navigationTarget.targetUri());
            navigationExecutor.openChart(navigationTarget.activity(), chartDetails.genre(), chartDetails.type(), chartDetails.category(), chartDetails.title().or(""));
        });
    }

    @CheckResult
    private Single<Action> showAllGenresCharts(NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            navigationExecutor.openAllGenres(navigationTarget.activity(), AllGenresUriResolver.resolveUri(navigationTarget.targetUri()));
        });
    }

    @CheckResult
    private Single<Action> showStation(NavigationTarget navigationTarget) throws UriResolveException {
        Optional<Urn> urn = stationsUriResolver.resolve(navigationTarget.targetUri());
        if (urn.isPresent()) {
            return showStation(navigationTarget, urn.get());
        } else {
            throw new UriResolveException("Station " + navigationTarget.target() + " could not be resolved locally");
        }
    }

    @CheckResult
    private Single<Action> showStation(NavigationTarget navigationTarget, Urn urn) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            startStationHandler.startStation(navigationTarget.activity(),
                                             urn,
                                             navigationTarget.discoverySource().or(DiscoverySource.DEEPLINK)); // TODO (REC-1302): Shall this be part of `forDeeplink` already?
        });
    }

    @CheckResult
    private Single<Action> showSearchScreen(NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            navigationExecutor.openSearch(navigationTarget.activity(), navigationTarget.targetUri(), navigationTarget.screen());
        });
    }

    @CheckResult
    private Single<Action> showSearchResultViewAllScreen(NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            navigationExecutor.openSearchViewAll(navigationTarget);
        });
    }

    @CheckResult
    private Single<Action> showRecordScreen(NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            navigationExecutor.openRecord(navigationTarget.activity(), navigationTarget.screen());
        });
    }

    @CheckResult
    private Single<Action> showUpgradeScreen(NavigationTarget navigationTarget) {
        if (featureOperations.upsellHighTier()) {
            return Single.just(() -> {
                trackForegroundEvent(navigationTarget.withScreen(Screen.CONVERSION));
                navigationExecutor.openUpgradeOnMain(navigationTarget.activity(), UpsellContext.DEFAULT);
            });
        } else {
            return openFallback(navigationTarget);
        }
    }

    @CheckResult
    private Single<Action> showProductChoiceScreen(NavigationTarget navigationTarget, Plan plan) {
        if (featureOperations.getCurrentPlan().isGoPlan()) {
            return openFallback(navigationTarget)
                    .doOnSuccess(__ -> AndroidUtils.showToast(navigationTarget.activity(), R.string.product_choice_error_already_subscribed, Toast.LENGTH_SHORT));
        } else if (featureOperations.upsellBothTiers()) {
            return Single.just(() -> {
                trackForegroundEvent(navigationTarget.withScreen(Screen.CONVERSION));
                navigationExecutor.openProductChoiceOnMain(navigationTarget.activity(), plan);
            });
        } else {
            return openFallback(navigationTarget);
        }
    }

    @CheckResult
    private Single<Action> showMidTierCheckoutScreen(NavigationTarget navigationTarget) {
        if (featureOperations.getCurrentPlan().isGoPlan()) {
            return openFallback(navigationTarget)
                    .doOnSuccess(__ -> AndroidUtils.showToast(navigationTarget.activity(), R.string.product_choice_error_already_subscribed, Toast.LENGTH_SHORT));
        } else if (featureOperations.upsellBothTiers()) {
            return Single.just(() -> {
                trackForegroundEvent(navigationTarget.withScreen(Screen.CHECKOUT));
                navigationExecutor.openDirectCheckout(navigationTarget.activity(), Plan.MID_TIER);
            });
        } else {
            return openFallback(navigationTarget);
        }
    }

    @CheckResult
    private Single<Action> showHighTierCheckoutScreen(NavigationTarget navigationTarget) {
        if (Plan.HIGH_TIER == featureOperations.getCurrentPlan()) {
            return openFallback(navigationTarget)
                    .doOnSuccess(__ -> AndroidUtils.showToast(navigationTarget.activity(), R.string.product_choice_error_already_subscribed, Toast.LENGTH_SHORT));
        } else if (featureOperations.upsellHighTier()) {
            return Single.just(() -> {
                trackForegroundEvent(navigationTarget.withScreen(Screen.CHECKOUT));
                navigationExecutor.openDirectCheckout(navigationTarget.activity(), Plan.HIGH_TIER);
            });
        } else {
            return openFallback(navigationTarget);
        }
    }

    @CheckResult
    private Single<Action> showOfflineSettingsScreen(NavigationTarget navigationTarget) {
        if (featureOperations.isOfflineContentEnabled()) {
            return Single.just(() -> {
                trackForegroundEvent(navigationTarget.withScreen(Screen.SETTINGS_OFFLINE));
                navigationExecutor.openOfflineSettings(navigationTarget.activity());
            });
        } else {
            return openFallback(navigationTarget);
        }
    }

    @CheckResult
    private Single<Action> openFallback(NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            navigationExecutor.openStream(navigationTarget.activity(), navigationTarget.screen());
        });
    }

    @CheckResult
    private Single<Action> showNotificationPreferencesScreen(NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            navigationExecutor.openNotificationPreferencesFromDeeplink(navigationTarget.activity());
        });
    }

    @CheckResult
    private Single<Action> showCollectionScreen(NavigationTarget navigationTarget) {
        return Single.just(() -> {
            trackForegroundEvent(navigationTarget);
            navigationExecutor.openCollection(navigationTarget.activity());
        });
    }

    @CheckResult
    private Single<Action> shareApp(NavigationTarget navigationTarget) {
        final Uri uri = Uri.parse(navigationTarget.target());
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
            return Single.just(() -> navigationTarget.activity().startActivity(Intent.createChooser(intent, title)));
        } else {
            return openFallback(navigationTarget);
        }
    }

    @CheckResult
    private Single<Action> showSystemSettings(NavigationTarget navigationTarget) {
        final Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setData(Uri.parse("package:" + navigationTarget.activity().getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        return Single.just(() -> navigationTarget.activity().startActivity(intent));
    }

    @CheckResult
    private Single<Action> startActivityForResource(NavigationTarget navigationTarget, Urn urn) {
        if (isCrawler(navigationTarget.referrer())) {
            loginCrawler();
        }

        final Single<Action> resultSingle;
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
    private Single<Action> navigateToResource(NavigationTarget navigationTarget, final Urn urn) {
        if (urn.isTrack()) {
            return startPlayback(navigationTarget, urn);
        } else if (urn.isUser()) {
            return showProfile(navigationTarget, urn);
        } else if (urn.isPlaylist()) {
            return Single.just(() -> navigationExecutor.legacyOpenPlaylist(navigationTarget.activity(), urn, navigationTarget.screen()));
        } else if (urn.isSystemPlaylist()) {
            return Single.just(() -> navigationExecutor.openSystemPlaylist(navigationTarget.activity(), urn, navigationTarget.screen()));
        } else if (urn.isArtistStation() || urn.isTrackStation()) {
            return showStation(navigationTarget, urn);
        } else {
            ErrorUtils.handleSilentException(new IllegalArgumentException("Trying to navigate to unsupported urn: " + urn + " in version: " + BuildConfig.VERSION_CODE));
            return Single.never();
        }
    }

    @CheckResult
    private Single<Action> startPlayback(NavigationTarget navigationTarget, Urn urn) {
        return playbackInitiator.startPlayback(urn, navigationTarget.screen())
                                .observeOn(AndroidSchedulers.mainThread())
                                .flatMap(playbackResult -> {
                                    if (navigationTarget.screen() == Screen.DEEPLINK) {
                                        if (playbackResult.isSuccess()) {
                                            return Single.just(() -> navigationExecutor.openStreamWithExpandedPlayer(navigationTarget.activity(), navigationTarget.screen()));
                                        } else {
                                            return launchApplicationWithMessage(navigationTarget, R.string.error_loading_url);
                                        }
                                    } else {
                                        return Single.just(() -> expandPlayerSubscriberProvider.get().onNext(playbackResult));
                                    }
                                });
    }

    @CheckResult
    private Single<Action> showOnboardingForUri(NavigationTarget navigationTarget) {
        return Single.just(() -> {
            AndroidUtils.showToast(navigationTarget.activity(), R.string.error_toast_user_not_logged_in);
            navigationExecutor.openOnboarding(navigationTarget.activity(), Uri.parse(navigationTarget.target()), navigationTarget.screen());
        });
    }

    @CheckResult
    private Single<Action> launchApplicationWithMessage(NavigationTarget navigationTarget, int messageId) {
        return Single.just(() -> {
            AndroidUtils.showToast(navigationTarget.activity(), messageId);
            if (navigationTarget.screen() == Screen.DEEPLINK) {
                navigationExecutor.openLauncher(navigationTarget.activity());
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
        return navigationTarget.fallback().isPresent() && !navigationTarget.fallback().get().equals(navigationTarget.target());
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
