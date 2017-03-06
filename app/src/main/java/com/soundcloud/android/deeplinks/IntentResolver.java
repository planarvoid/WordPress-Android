package com.soundcloud.android.deeplinks;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.PlaybackServiceController;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.events.DeeplinkReportEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.UriUtils;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.rx.eventbus.EventBus;
import rx.android.schedulers.AndroidSchedulers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

import javax.inject.Inject;

public class IntentResolver {

    private final ResolveOperations resolveOperations;
    private final AccountOperations accountOperations;
    private final PlaybackServiceController serviceController;
    private final PlaybackInitiator playbackInitiator;
    private final PlayQueueManager playQueueManager;
    private final ReferrerResolver referrerResolver;
    private final EventBus eventBus;
    private final Navigator navigator;
    private final FeatureOperations featureOperations;
    private final ChartsUriResolver chartsUriResolver;

    @Inject
    IntentResolver(ResolveOperations resolveOperations,
                   AccountOperations accountOperations,
                   PlaybackServiceController serviceController,
                   PlaybackInitiator playbackInitiator,
                   PlayQueueManager playQueueManager,
                   ReferrerResolver referrerResolver,
                   EventBus eventBus,
                   Navigator navigator,
                   FeatureOperations featureOperations,
                   ChartsUriResolver chartsUriResolver) {
        this.resolveOperations = resolveOperations;
        this.accountOperations = accountOperations;
        this.serviceController = serviceController;
        this.playbackInitiator = playbackInitiator;
        this.playQueueManager = playQueueManager;
        this.referrerResolver = referrerResolver;
        this.eventBus = eventBus;
        this.navigator = navigator;
        this.featureOperations = featureOperations;
        this.chartsUriResolver = chartsUriResolver;
    }

    void handleIntent(Intent intent, Context context) {
        final Uri uri = intent.getData();
        final String referrer = getReferrer(context, intent);
        if (uri == null || Strings.isBlank(uri.toString())) {
            showHomeScreen(context, referrer);
        } else {
            final Uri hierarchicalUri = UriUtils.convertToHierarchicalUri(uri);
            final DeepLink deepLink = DeepLink.fromUri(hierarchicalUri);
            if (shouldShowLogInMessage(deepLink, referrer)) {
                trackForegroundEvent(referrer);
                showOnboardingForUri(context, hierarchicalUri);
            } else {
                handleDeepLink(context, hierarchicalUri, deepLink, referrer);
            }
        }
    }

    private void handleDeepLink(Context context, Uri uri, DeepLink deepLink, String referrer) {
        switch (deepLink) {
            case HOME:
            case STREAM:
                showHomeScreen(context, referrer);
                break;
            case RECORD:
                showRecordScreen(context, referrer);
                break;
            case DISCOVERY:
                showDiscoveryScreen(context, referrer);
                break;
            case TRACK_RECOMMENDATIONS:
                showTrackRecommendationsScreen(context, referrer);
                break;
            case CHARTS:
                showCharts(context, uri, referrer);
                break;
            case CHARTS_ALL_GENRES:
                showAllGenresCharts(context, uri, referrer);
                break;
            case SEARCH:
                showSearchScreen(context, uri, referrer);
                break;
            case WEB_VIEW:
                startWebView(context, uri, referrer);
                break;
            case SOUNDCLOUD_GO_PLUS_UPSELL:
                showUpgradeScreen(context, referrer);
                break;
            case SOUNDCLOUD_GO_PLUS_BUY:
                showDirectCheckoutScreen(context, referrer);
                break;
            case SOUNDCLOUD_GO_CHOICE:
                showProductChoiceScreen(context, referrer, Plan.MID_TIER);
                break;
            case SOUNDCLOUD_GO_PLUS_CHOICE:
                showProductChoiceScreen(context, referrer, Plan.HIGH_TIER);
                break;
            case OFFLINE_SETTINGS:
                showOfflineSettingsScreen(context, referrer);
                break;
            case NOTIFICATION_PREFERENCES:
                showNotificationPreferencesScreen(context, referrer);
                break;
            case COLLECTION:
                showCollectionScreen(context, referrer);
                break;
            case SHARE_APP:
                shareApp(context, uri, referrer);
                break;
            case SYSTEM_SETTINGS:
                showSystemSettings(context);
                break;
            default:
                resolve(context, uri, referrer);
        }
    }

    private boolean shouldShowLogInMessage(DeepLink deepLink, String referrer) {
        if (deepLink.requiresLoggedInUser()) {
            if (deepLink.requiresResolve()) {
                return false;
            } else if (isCrawler(referrer)) {
                loginCrawler();
            } else if (!accountOperations.isUserLoggedIn()) {
                return true;
            }
        }
        return false;
    }

    private void resolve(Context context, Uri uri, String referrer) {
        resolveOperations.resolve(uri)
                         .observeOn(AndroidSchedulers.mainThread())
                         .subscribe(fetchSubscriber(context, uri, referrer));
    }

    private DefaultSubscriber<ResolveResult> fetchSubscriber(final Context context, final Uri uri, final String referrer) {
        return new DefaultSubscriber<ResolveResult>() {
            @Override
            public void onNext(ResolveResult resolveResult) {
                if (resolveResult.success()) {
                    startActivityForResource(context, resolveResult.urn().get(), uri, referrer);
                } else {
                    handleError(resolveResult);
                }
            }

            private void handleError(ResolveResult resolveResult) {
                Optional<Uri> errorUri = resolveResult.uri();
                Uri resolvedUri = errorUri.isPresent() ? errorUri.get() : uri;
                DeepLink deepLink = DeepLink.fromUri(resolvedUri);

                if (DeepLink.WEB_VIEW.equals(deepLink)) {
                    startWebView(context, resolvedUri, referrer);
                } else {
                    trackForegroundEvent(referrer);
                    launchApplicationWithMessage(context, R.string.error_loading_url);

                    Optional<Exception> exception = resolveResult.exception();
                    if (exception.isPresent() && !ErrorUtils.isNetworkError(exception.get())) {
                        ErrorUtils.handleSilentException("unable to load deeplink:" + errorUri.get(), exception.get());
                        reportFailedToResolveDeeplink(referrer);
                    }
                }
            }
        };
    }

    private void reportSuccessfulDeeplink(String referrer) {
        eventBus.publish(EventQueue.TRACKING, DeeplinkReportEvent.forResolvedDeeplink(referrer));
    }

    private void reportFailedToResolveDeeplink(String referrer) {
        eventBus.publish(EventQueue.TRACKING, DeeplinkReportEvent.forResolutionFailure(referrer));
    }

    private void startWebView(Context context, Uri uri, String referrer) {
        trackForegroundEvent(referrer);
        navigator.openWebView(context, uri);
    }

    private void showHomeScreen(Context context, String referrer) {
        accountOperations.clearCrawler();
        trackForegroundEvent(referrer);
        navigator.openStream(context, Screen.DEEPLINK);
    }

    private void showDiscoveryScreen(Context context, String referrer) {
        trackForegroundEvent(referrer);
        navigator.openDiscovery(context, Screen.DEEPLINK);
    }

    private void showTrackRecommendationsScreen(Context context, String referrer) {
        trackForegroundEvent(referrer);
        navigator.openViewAllRecommendations(context);
    }

    private void showCharts(Context context, Uri uri, String referrer) {
        trackForegroundEvent(referrer);
        ChartDetails chartDetails = chartsUriResolver.resolveUri(uri);
        navigator.openChart(context, chartDetails.genre(), chartDetails.type(), chartDetails.category(), chartDetails.title().or(""));
    }

    private void showAllGenresCharts(Context context, Uri uri, String referrer) {
        trackForegroundEvent(referrer);
        navigator.openAllGenres(context, AllGenresUriResolver.resolveUri(uri));
    }

    private void showSearchScreen(Context context, Uri uri, String referrer) {
        trackForegroundEvent(referrer);
        navigator.openSearch(context, uri, Screen.DEEPLINK);
    }

    private void showRecordScreen(Context context, String referrer) {
        trackForegroundEvent(referrer);
        navigator.openRecord(context, Screen.DEEPLINK);
    }

    private void showUpgradeScreen(Context context, String referrer) {
        if (featureOperations.upsellHighTier()) {
            trackForegroundEvent(referrer, Screen.CONVERSION);
            navigator.openUpgradeOnMain(context);
        } else {
            openFallback(context, referrer);
        }
    }

    private void showProductChoiceScreen(Context context, String referrer, Plan plan) {
        if (featureOperations.getCurrentPlan().isGoPlan()) {
            Toast.makeText(context, R.string.product_choice_error_already_subscribed, Toast.LENGTH_SHORT).show();
            openFallback(context, referrer);
        } else if (featureOperations.upsellBothTiers()) {
            trackForegroundEvent(referrer, Screen.CONVERSION);
            navigator.openProductChoiceOnMain(context, plan);
        } else {
            openFallback(context, referrer);
        }
    }

    private void showDirectCheckoutScreen(Context context, String referrer) {
        if (featureOperations.upsellHighTier()) {
            trackForegroundEvent(referrer, Screen.CHECKOUT);
            navigator.openDirectCheckout(context);
        } else {
            openFallback(context, referrer);
        }
    }

    private void showOfflineSettingsScreen(Context context, String referrer) {
        if (featureOperations.isOfflineContentEnabled()) {
            trackForegroundEvent(referrer, Screen.SETTINGS_OFFLINE);
            navigator.openOfflineSettings(context);
        } else {
            openFallback(context, referrer);
        }
    }

    private void openFallback(Context context, String referrer) {
        trackForegroundEvent(referrer);
        navigator.openStream(context, Screen.DEEPLINK);
    }

    private void showNotificationPreferencesScreen(Context context, String referrer) {
        trackForegroundEvent(referrer);
        navigator.openNotificationPreferencesFromDeeplink(context);
    }


    private void showCollectionScreen(Context context, String referrer) {
        trackForegroundEvent(referrer);
        navigator.openCollection(context);
    }

    private void shareApp(Context context, Uri uri, String referrer) {
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
            context.startActivity(Intent.createChooser(intent, title));
        } else {
            openFallback(context, referrer);
        }
    }

    private void showSystemSettings(Context context) {
        final Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(intent);
    }

    private void startActivityForResource(Context context, Urn urn, Uri uri, String referrer) {
        if (isCrawler(referrer)) {
            loginCrawler();
        }

        if (accountOperations.isUserLoggedIn()) {
            navigateToResource(context, urn);
        } else {
            showOnboardingForUri(context, uri);
        }

        trackForegroundEventForResource(urn, referrer);
        reportSuccessfulDeeplink(referrer);
    }

    private void navigateToResource(final Context context, final Urn urn) {
        if (urn.isTrack()) {
            playbackInitiator.startPlayback(urn, Screen.DEEPLINK)
                             .observeOn(AndroidSchedulers.mainThread())
                             .subscribe(new PlaybackSubscriber(context));
        } else if (urn.isUser()) {
            navigator.legacyOpenProfile(context, urn, Screen.DEEPLINK);
        } else if (urn.isPlaylist()) {
            navigator.legacyOpenPlaylist(context, urn, Screen.DEEPLINK);
        } else {
            throw new IllegalArgumentException("Unrecognized resolved resource: " + urn);
        }
    }

    private class PlaybackSubscriber extends DefaultSubscriber<PlaybackResult> {
        private final Context context;

        PlaybackSubscriber(Context context) {
            this.context = context;
        }

        @Override
        public void onNext(PlaybackResult playbackResult) {
            if (playbackResult.isSuccess()) {
                navigator.openStreamWithExpandedPlayer(context, Screen.DEEPLINK);
            } else {
                launchApplicationWithMessage(context, R.string.error_loading_url);
            }
        }
    }

    private void showOnboardingForUri(Context context, Uri uri) {
        AndroidUtils.showToast(context, R.string.error_toast_user_not_logged_in);
        navigator.openOnboarding(context, uri, Screen.DEEPLINK);
    }

    private void launchApplicationWithMessage(Context context, int messageId) {
        AndroidUtils.showToast(context, messageId);
        navigator.openLauncher(context);
    }

    private boolean isCrawler(String referrer) {
        return Referrer.GOOGLE_CRAWLER.value().equals(referrer);
    }

    private void loginCrawler() {
        accountOperations.loginCrawlerUser();
        serviceController.resetPlaybackService();
        playQueueManager.clearAll(); // do not leave previous played tracks visible for crawlers
    }

    private String getReferrer(Context context, Intent intent) {
        return referrerResolver.getReferrerFromIntent(intent, context.getResources());
    }

    private void trackForegroundEventForResource(Urn urn, String referrer) {
        trackForegroundEvent(ForegroundEvent.open(Screen.DEEPLINK, referrer, urn));
    }

    private void trackForegroundEvent(String referrer) {
        trackForegroundEvent(referrer, Screen.DEEPLINK);
    }

    private void trackForegroundEvent(String referrer, Screen screen) {
        trackForegroundEvent(ForegroundEvent.open(screen, referrer));
    }

    private void trackForegroundEvent(ForegroundEvent event) {
        eventBus.publish(EventQueue.TRACKING, event);
    }
}
