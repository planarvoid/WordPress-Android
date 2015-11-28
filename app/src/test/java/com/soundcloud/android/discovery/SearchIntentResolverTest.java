package com.soundcloud.android.discovery;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;

public class SearchIntentResolverTest extends AndroidUnitTest {

    public static final String SEARCH_QUERY = "searchQuery";

    private SearchIntentResolver intentResolver;
    private Intent intent;

    @Mock private Navigator navigator;
    @Mock private SearchTracker tracker;
    @Mock private SearchIntentResolver.DeepLinkListener listener;

    @Before
    public void setUp() {
        intentResolver = new SearchIntentResolver(navigator, tracker);
        intentResolver.setDeepLinkListener(listener);
        intent = new Intent();
    }

    @Test
    public void shouldResolvePlayFromSearch() {
        intent.setAction(SearchIntentResolver.ACTION_PLAY_FROM_SEARCH);
        intent.putExtra(SearchManager.QUERY, SEARCH_QUERY);

        intentResolver.handle(context(), intent);

        verify(listener).onDeepLinkExecuted(SEARCH_QUERY);
    }

    @Test
    public void shouldResolveActionSearch() {
        intent.setAction(Intent.ACTION_SEARCH);
        intent.putExtra(SearchManager.QUERY, SEARCH_QUERY);

        intentResolver.handle(context(), intent);

        verify(listener).onDeepLinkExecuted(SEARCH_QUERY);
    }

    @Test
    public void shouldInterceptSearchUrlWithQueryParam() {
        intent.setData(Uri.parse("https://soundcloud.com/search/people?q=" + SEARCH_QUERY));

        intentResolver.handle(context(), intent);

        verify(listener).onDeepLinkExecuted(SEARCH_QUERY);
    }

    @Test
    public void shouldLaunchSystemSearch() {
        final Uri uri = Content.COLLECTION.uri;
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);

        intentResolver.handle(context(), intent);

        verify(navigator).openSystemSearch(context(), uri);
    }

    @Test
    public void shouldTrackScreen() {
        intentResolver.handle(context(), intent);

        verify(tracker).trackScreenEvent();
    }
}
