package com.soundcloud.android.deeplinks;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.rx.eventbus.EventBus;
import rx.android.schedulers.AndroidSchedulers;
import rx.exceptions.OnErrorThrowable;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import javax.inject.Inject;

public class IntentResolver {
    private final ResolveOperations resolveOperations;
    private final AccountOperations accountOperations;
    private final PlaybackOperations playbackOperations;
    private final PlayQueueManager playQueueManager;
    private final ReferrerResolver referrerResolver;
    private final EventBus eventBus;
    private final Navigator navigator;

    @Inject
    IntentResolver(ResolveOperations resolveOperations,
                   AccountOperations accountOperations,
                   PlaybackOperations playbackOperations,
                   PlayQueueManager playQueueManager,
                   ReferrerResolver referrerResolver,
                   EventBus eventBus,
                   Navigator navigator) {
        this.resolveOperations = resolveOperations;
        this.accountOperations = accountOperations;
        this.playbackOperations = playbackOperations;
        this.playQueueManager = playQueueManager;
        this.referrerResolver = referrerResolver;
        this.eventBus = eventBus;
        this.navigator = navigator;
    }

    public void handleIntent(Intent intent, Context context) {
        Uri uri = intent.getData();
        Referrer referrer = getReferrer(context, intent);
        DeepLink deepLink = DeepLink.fromUri(uri);

        if (shouldShowLogInMessage(deepLink, referrer)) {
            showOnboardingForUrn(context, Urn.NOT_SET, referrer);
        } else {
            handleDeepLink(context, uri, deepLink, referrer);
        }
    }

    private void handleDeepLink(Context context, Uri uri, DeepLink deepLink, Referrer referrer) {
        switch (deepLink) {
            case HOME:
            case STREAM:
                showHomeScreen(context, referrer);
                break;
            case EXPLORE:
                showExploreScreen(context, referrer);
                break;
            case RECORD:
                showRecordScreen(context, referrer);
                break;
            case SEARCH:
                showSearchScreen(context, uri, referrer);
                break;
            case WEB_VIEW:
                startWebView(context, uri, referrer);
                break;
            default:
                resolve(context, uri, referrer);
        }
    }

    private boolean shouldShowLogInMessage(DeepLink deepLink, Referrer referrer) {
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

    private void resolve(Context context, Uri uri, Referrer referrer) {
        resolveOperations.resolve(uri)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fetchSubscriber(context, uri, referrer));
    }

    private DefaultSubscriber<? super PublicApiResource> fetchSubscriber(final Context context, final Uri uri, final Referrer referrer) {
        return new DefaultSubscriber<PublicApiResource>() {
            @Override
            public void onNext(PublicApiResource resource) {
                startActivityForResource(context, resource, referrer);
            }

            @Override
            public void onError(Throwable e) {
                Uri resolvedUri = uriFromResolveException(e, uri);
                DeepLink deepLink = DeepLink.fromUri(resolvedUri);

                if (DeepLink.WEB_VIEW.equals(deepLink)) {
                    startWebView(context, resolvedUri, referrer);
                } else {
                    launchApplicationWithMessage(context, referrer, R.string.error_loading_url);
                }
            }
        };
    }

    private void startWebView(Context context, Uri uri, Referrer referrer) {
        trackForegroundEvent(referrer);
        navigator.openWebView(context, uri);
    }

    private boolean shouldLoadRelated(Referrer referrer) {
        return isCrawler(referrer)
                ? PlaybackOperations.WITHOUT_RELATED
                : PlaybackOperations.WITH_RELATED;
    }

    private void showHomeScreen(Context context, Referrer referrer) {
        accountOperations.clearCrawler();
        trackForegroundEvent(referrer);
        navigator.openStream(context, Screen.DEEPLINK);
    }

    private void showExploreScreen(Context context, Referrer referrer) {
        trackForegroundEvent(referrer);
        navigator.openExplore(context, Screen.DEEPLINK);
    }

    private void showSearchScreen(Context context, Uri uri, Referrer referrer) {
        trackForegroundEvent(referrer);
        navigator.openSearch(context, uri, Screen.DEEPLINK);
    }

    private void showRecordScreen(Context context, Referrer referrer) {
        trackForegroundEvent(referrer);
        navigator.openRecord(context, Screen.DEEPLINK);
    }

    private void startActivityForResource(Context context, PublicApiResource resource, Referrer referrer) {
        Urn urn = resource.getUrn();

        if (isActionableResource(resource)) {
            if (isCrawler(referrer)) {
                loginCrawler();
            }

            if (accountOperations.isUserLoggedIn()) {
                trackForegroundEventForResource(urn, referrer);
                navigateToResource(context, resource, referrer);
            } else {
                showOnboardingForUrn(context, urn, referrer);
            }
        } else {
            showHomeScreen(context, referrer);
        }
    }

    private void navigateToResource(Context context, PublicApiResource resource, Referrer referrer) {
        Urn urn = resource.getUrn();

        if (urn.isTrack()) {
            fireAndForget(playbackOperations.startPlayback((PublicApiTrack) resource, Screen.DEEPLINK, shouldLoadRelated(referrer)));
            navigator.openStreamWithExpandedPlayer(context, Screen.DEEPLINK);
        } else if (urn.isUser()) {
            navigator.openProfile(context, urn, Screen.DEEPLINK);
        } else if (urn.isPlaylist()) {
            navigator.openPlaylist(context, urn, Screen.DEEPLINK);
        } else {
            throw new IllegalArgumentException("Unrecognized resolved resource: " + urn);
        }
    }

    boolean isActionableResource(PublicApiResource resource) {
        Urn urn = resource.getUrn();
        return urn.isTrack() || urn.isUser() || urn.isPlaylist();
    }

    private void showOnboardingForUrn(Context context, Urn urn, Referrer referrer) {
        if (Urn.NOT_SET.equals(urn)) {
            trackForegroundEvent(referrer);
        } else {
            trackForegroundEventForResource(urn, referrer);
        }
        AndroidUtils.showToast(context, R.string.error_toast_user_not_logged_in);
        navigator.openOnboarding(context, urn, Screen.DEEPLINK);
    }

    private Uri uriFromResolveException(Throwable e, Uri fallbackUri) {
        if (e instanceof OnErrorThrowable.OnNextValue) {
            Object context = ((OnErrorThrowable.OnNextValue) e).getValue();
            if (context instanceof Uri) {
                return (Uri) context;
            }
        }
        return fallbackUri;
    }

    private void launchApplicationWithMessage(Context context, Referrer referrer, int messageId) {
        trackForegroundEvent(referrer);
        AndroidUtils.showToast(context, messageId);
        navigator.openLauncher(context);
    }

    private boolean isCrawler(Referrer referrer) {
        return Referrer.GOOGLE_CRAWLER.equals(referrer);
    }

    private void loginCrawler() {
        accountOperations.loginCrawlerUser();
        playbackOperations.resetService();
        playQueueManager.clearAll(); // do not leave previous played tracks visible for crawlers
    }

    private Referrer getReferrer(Context context, Intent intent) {
        return referrerResolver.getReferrerFromIntent(intent, context.getResources());
    }

    private void trackForegroundEventForResource(Urn urn, Referrer referrer) {
        trackForegroundEvent(ForegroundEvent.open(Screen.DEEPLINK, referrer, urn));
    }

    private void trackForegroundEvent(Referrer referrer) {
        trackForegroundEvent(ForegroundEvent.open(Screen.DEEPLINK, referrer));
    }

    private void trackForegroundEvent(ForegroundEvent event) {
        eventBus.publish(EventQueue.TRACKING, event);
    }
}
