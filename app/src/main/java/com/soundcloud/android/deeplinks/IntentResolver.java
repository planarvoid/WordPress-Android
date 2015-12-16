package com.soundcloud.android.deeplinks;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
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
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.java.strings.Strings;
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
    private final ServiceInitiator serviceInitiator;
    private final PlaybackInitiator playbackInitiator;
    private final PlayQueueManager playQueueManager;
    private final ReferrerResolver referrerResolver;
    private final EventBus eventBus;
    private final Navigator navigator;

    @Inject
    IntentResolver(ResolveOperations resolveOperations,
                   AccountOperations accountOperations,
                   ServiceInitiator serviceInitiator,
                   PlaybackInitiator playbackInitiator,
                   PlayQueueManager playQueueManager,
                   ReferrerResolver referrerResolver,
                   EventBus eventBus,
                   Navigator navigator) {
        this.resolveOperations = resolveOperations;
        this.accountOperations = accountOperations;
        this.serviceInitiator = serviceInitiator;
        this.playbackInitiator = playbackInitiator;
        this.playQueueManager = playQueueManager;
        this.referrerResolver = referrerResolver;
        this.eventBus = eventBus;
        this.navigator = navigator;
    }

    public void handleIntent(Intent intent, Context context) {
        Uri uri = intent.getData();
        Referrer referrer = getReferrer(context, intent);
        if (uri == null || Strings.isBlank(uri.toString())) {
            // fall back to home screen
            showHomeScreen(context, referrer);
        } else {
            Urn urn = new Urn(uri.toString());
            DeepLink deepLink = DeepLink.fromUri(uri);

            if (isSupportedEntityUrn(urn)) {
                // this is the case where we had previously resolved the link and are now
                // coming back from the login screen with the resolved URN
                startActivityForResource(context, urn, referrer);
            } else if (shouldShowLogInMessage(deepLink, referrer)) {
                trackForegroundEvent(referrer);
                showOnboardingForUrn(context, Urn.NOT_SET);
            } else {
                handleDeepLink(context, uri, deepLink, referrer);
            }
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

    private DefaultSubscriber<Urn> fetchSubscriber(final Context context, final Uri uri, final Referrer referrer) {
        return new DefaultSubscriber<Urn>() {
            @Override
            public void onNext(Urn urn) {
                startActivityForResource(context, urn, referrer);
            }

            @Override
            public void onError(Throwable e) {
                Uri resolvedUri = uriFromResolveException(e, uri);
                DeepLink deepLink = DeepLink.fromUri(resolvedUri);

                if (DeepLink.WEB_VIEW.equals(deepLink)) {
                    startWebView(context, resolvedUri, referrer);
                } else {
                    trackForegroundEvent(referrer);
                    launchApplicationWithMessage(context, R.string.error_loading_url);
                }
            }
        };
    }

    private void startWebView(Context context, Uri uri, Referrer referrer) {
        trackForegroundEvent(referrer);
        navigator.openWebView(context, uri);
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

    private void startActivityForResource(Context context, Urn urn, Referrer referrer) {
        if (isCrawler(referrer)) {
            loginCrawler();
        }

        trackForegroundEventForResource(urn, referrer);

        if (accountOperations.isUserLoggedIn()) {
            navigateToResource(context, urn);
        } else {
            showOnboardingForUrn(context, urn);
        }
    }

    private void navigateToResource(final Context context, final Urn urn) {
        if (urn.isTrack()) {
            playbackInitiator.startPlayback(urn, Screen.DEEPLINK)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new PlaybackSubscriber(context));
        } else if (urn.isUser()) {
            navigator.openProfile(context, urn, Screen.DEEPLINK);
        } else if (urn.isPlaylist()) {
            navigator.openPlaylist(context, urn, Screen.DEEPLINK);
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

    private boolean isSupportedEntityUrn(Urn urn) {
        return urn.isTrack() || urn.isUser() || urn.isPlaylist();
    }

    private void showOnboardingForUrn(Context context, Urn urn) {
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

    private void launchApplicationWithMessage(Context context, int messageId) {
        AndroidUtils.showToast(context, messageId);
        navigator.openLauncher(context);
    }

    private boolean isCrawler(Referrer referrer) {
        return Referrer.GOOGLE_CRAWLER.equals(referrer);
    }

    private void loginCrawler() {
        accountOperations.loginCrawlerUser();
        serviceInitiator.resetPlaybackService();
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
