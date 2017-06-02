package com.soundcloud.android.olddiscovery;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.Actions;
import com.soundcloud.android.deeplinks.DeepLink;
import com.soundcloud.android.deeplinks.ReferrerResolver;
import com.soundcloud.android.deeplinks.UriResolveException;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.search.SearchTracker;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.strings.Strings;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;

@AutoFactory(allowSubclasses = true)
class SearchIntentResolver {

    @VisibleForTesting
    static final String ACTION_PLAY_FROM_SEARCH = "android.media.action.MEDIA_PLAY_FROM_SEARCH";

    private static final String INTENT_URL_HOST = "soundcloud.com";
    private static final String INTENT_URL_QUERY_PARAM = "q";
    private static final String INTENT_URI_SEARCH_PATH = "/search";

    private final DeepLinkListener listener;
    private final Navigator navigator;
    private final ReferrerResolver referrerResolver;
    private final SearchTracker tracker;

    SearchIntentResolver(DeepLinkListener listener, @Provided Navigator navigator, @Provided ReferrerResolver referrerResolver, @Provided SearchTracker tracker) {
        this.listener = listener;
        this.navigator = navigator;
        this.referrerResolver = referrerResolver;
        this.tracker = tracker;
    }

    void handle(Activity activity, Intent intent) throws UriResolveException {
        if (isInterceptedSearchAction(intent)) {
            searchFromDeepLink(intent.getStringExtra(SearchManager.QUERY));
        } else if (isInterceptedSearchUrl(intent)) {
            searchFromDeepLink(intent.getData().getQueryParameter(INTENT_URL_QUERY_PARAM));
        } else if (isInterceptedUri(intent)) {
            handleUri(activity, intent);
        } else {
            tracker.trackMainScreenEvent();
        }
    }

    private boolean isInterceptedSearchAction(Intent intent) {
        return Intent.ACTION_SEARCH.equals(intent.getAction())
                || ACTION_PLAY_FROM_SEARCH.equals(intent.getAction())
                || Actions.PERFORM_SEARCH.equals(intent.getAction());
    }

    private boolean isInterceptedSearchUrl(Intent intent) {
        final Uri uri = intent.getData();
        return uri != null
                && (uri.getHost().contains(INTENT_URL_HOST) || DeepLink.SOUNDCLOUD_SCHEME.equals(uri.getScheme()))
                && Strings.isNotBlank(uri.getQueryParameter(INTENT_URL_QUERY_PARAM));
    }

    private boolean isInterceptedUri(Intent intent) {
        return Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null
                && !intent.getData().getPath().equals(INTENT_URI_SEARCH_PATH);
    }

    private void handleUri(Activity activity, Intent intent) throws UriResolveException {
        final Content content = Content.match(intent.getData());
        if (content == Content.SEARCH_ITEM) {
            searchFromDeepLink(Uri.decode(intent.getData().getLastPathSegment()));
        } else if (content != Content.UNKNOWN) {
            final String referrer = referrerResolver.getReferrerFromIntent(intent, activity.getResources());
            navigator.navigateTo(NavigationTarget.forExternalDeeplink(activity, intent.getDataString(), referrer));
        }
    }

    private void searchFromDeepLink(String query) {
        listener.onDeepLinkExecuted(query.trim());
    }

    interface DeepLinkListener {
        void onDeepLinkExecuted(String searchQuery);
    }
}
