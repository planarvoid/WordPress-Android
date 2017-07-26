package com.soundcloud.android.olddiscovery;

import static com.soundcloud.android.helpers.NavigationTargetMatcher.matchesNavigationTarget;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.deeplinks.ReferrerResolver;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.search.SearchTracker;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;

public class SearchIntentResolverTest extends AndroidUnitTest {

    public static final String SEARCH_QUERY = "searchQuery";

    private SearchIntentResolver intentResolver;
    private Intent intent;

    @Mock private SearchTracker tracker;
    @Mock private SearchIntentResolver.DeepLinkListener listener;
    @Mock private Navigator navigator;
    @Mock private ReferrerResolver referrerResolver;
    private AppCompatActivity activity;

    @Before
    public void setUp() {
        intentResolver = new SearchIntentResolver(listener, navigator, referrerResolver, tracker);
        intent = new Intent();
        activity = activity();
    }

    @Test
    public void shouldResolvePlayFromSearch() throws Exception {
        intent.setAction(SearchIntentResolver.ACTION_PLAY_FROM_SEARCH);
        intent.putExtra(SearchManager.QUERY, SEARCH_QUERY);

        intentResolver.handle(activity, intent);

        verify(listener).onDeepLinkExecuted(SEARCH_QUERY);
    }

    @Test
    public void shouldResolveActionSearch() throws Exception {
        intent.setAction(Intent.ACTION_SEARCH);
        intent.putExtra(SearchManager.QUERY, SEARCH_QUERY);

        intentResolver.handle(activity, intent);

        verify(listener).onDeepLinkExecuted(SEARCH_QUERY);
    }

    @Test
    public void shouldInterceptSearchUrlWithQueryParam() throws Exception {
        intent.setData(Uri.parse("https://soundcloud.com/search/people?q=" + SEARCH_QUERY));

        intentResolver.handle(activity, intent);

        verify(listener).onDeepLinkExecuted(SEARCH_QUERY);
    }

    @Test
    public void shouldLaunchSystemSearch() throws Exception {
        when(referrerResolver.getReferrerFromIntent(eq(intent), any())).thenReturn(Referrer.STREAM_NOTIFICATION.value());
        final Uri uri = Content.TRACK.uri;
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);

        intentResolver.handle(activity, intent);

        verify(navigator).navigateTo(argThat(matchesNavigationTarget(NavigationTarget.forExternalDeeplink(intent.getDataString(), Referrer.STREAM_NOTIFICATION.value()))));
    }

    @Test
    public void shouldTrackScreen() throws Exception {
        intentResolver.handle(activity, intent);

        verify(tracker).trackMainScreenEvent();
    }
}
