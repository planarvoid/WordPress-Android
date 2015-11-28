package com.soundcloud.android.discovery;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.checks.Preconditions;
import com.soundcloud.java.strings.Strings;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import javax.inject.Inject;

class SearchIntentResolver {

    interface DeepLinkListener {
        void onDeepLinkExecuted(String searchQuery);
    }

    @VisibleForTesting
    static final String ACTION_PLAY_FROM_SEARCH = "android.media.action.MEDIA_PLAY_FROM_SEARCH";

    private static final String INTENT_URL_HOST = "soundcloud.com";
    private static final String INTENT_URL_QUERY_PARAM = "q";
    private static final String INTENT_URI_SEARCH_PATH = "/search";

    private final Navigator navigator;
    private final SearchTracker tracker;

    private DeepLinkListener deepLinkListener;

    @Inject
    SearchIntentResolver(Navigator navigator, SearchTracker tracker) {
        this.navigator = navigator;
        this.tracker = tracker;
    }

    void handle(Context context, Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())
                || ACTION_PLAY_FROM_SEARCH.equals(intent.getAction())
                || Actions.PERFORM_SEARCH.equals(intent.getAction())) {
            searchFromDeepLink(intent.getStringExtra(SearchManager.QUERY));
        } else if (isInterceptedSearchUrl(intent)) {
            searchFromDeepLink(intent.getData().getQueryParameter(INTENT_URL_QUERY_PARAM));
        } else if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null
                && !intent.getData().getPath().equals(INTENT_URI_SEARCH_PATH)) {
            handleUri(context, intent);
        } else {
            tracker.trackScreenEvent();
        }
    }

    void setDeepLinkListener(DeepLinkListener deepLinkListener) {
        Preconditions.checkNotNull(deepLinkListener);
        this.deepLinkListener = deepLinkListener;
    }

    private boolean isInterceptedSearchUrl(Intent intent) {
        final Uri uri = intent.getData();
        return uri != null
                && (uri.getHost().equals(INTENT_URL_HOST) || Urn.SOUNDCLOUD_SCHEME.equals(uri.getScheme()))
                && Strings.isNotBlank(uri.getQueryParameter(INTENT_URL_QUERY_PARAM));
    }

    private void handleUri(Context context, Intent intent) {
        final Content content = Content.match(intent.getData());
        if (content == Content.SEARCH_ITEM) {
            searchFromDeepLink(Uri.decode(intent.getData().getLastPathSegment()));
        } else if (content != Content.UNKNOWN) {
            navigator.openSystemSearch(context, intent.getData());
        }
    }

    private void searchFromDeepLink(String query) {
        if (deepLinkListener != null) {
            deepLinkListener.onDeepLinkExecuted(query.trim());
        }
    }
}
