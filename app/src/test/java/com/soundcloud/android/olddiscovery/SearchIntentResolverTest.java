package com.soundcloud.android.olddiscovery;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.deeplinks.ReferrerResolver;
import com.soundcloud.android.main.NavigationDelegate;
import com.soundcloud.android.main.NavigationTarget;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.search.SearchTracker;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.SyncConfig;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;

public class SearchIntentResolverTest extends AndroidUnitTest {

    public static final String SEARCH_QUERY = "searchQuery";

    private SearchIntentResolver intentResolver;
    private Intent intent;

    @Mock private SearchTracker tracker;
    @Mock private SearchIntentResolver.DeepLinkListener listener;
    @Mock private NavigationDelegate navigationDelegate;
    @Mock private ReferrerResolver referrerResolver;

    @Before
    public void setUp() {
        intentResolver = new SearchIntentResolver(listener, navigationDelegate, referrerResolver, tracker);
        intent = new Intent();
    }

    @Test
    public void shouldResolvePlayFromSearch() {
        intent.setAction(SearchIntentResolver.ACTION_PLAY_FROM_SEARCH);
        intent.putExtra(SearchManager.QUERY, SEARCH_QUERY);

        intentResolver.handle(activity(), intent);

        verify(listener).onDeepLinkExecuted(SEARCH_QUERY);
    }

    @Test
    public void shouldResolveActionSearch() {
        intent.setAction(Intent.ACTION_SEARCH);
        intent.putExtra(SearchManager.QUERY, SEARCH_QUERY);

        intentResolver.handle(activity(), intent);

        verify(listener).onDeepLinkExecuted(SEARCH_QUERY);
    }

    @Test
    public void shouldInterceptSearchUrlWithQueryParam() {
        intent.setData(Uri.parse("https://soundcloud.com/search/people?q=" + SEARCH_QUERY));

        intentResolver.handle(activity(), intent);

        verify(listener).onDeepLinkExecuted(SEARCH_QUERY);
    }

    @Test
    public void shouldLaunchSystemSearch() {
        when(referrerResolver.getReferrerFromIntent(eq(intent), any())).thenReturn(Referrer.STREAM_NOTIFICATION.value());
        final Uri uri = Content.TRACK.uri;
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);

        intentResolver.handle(activity(), intent);

        final ArgumentCaptor<NavigationTarget> navigationTargetArgumentCaptor = ArgumentCaptor.forClass(NavigationTarget.class);
        verify(navigationDelegate).navigateTo(navigationTargetArgumentCaptor.capture());
        final NavigationTarget resultNavigationTarget = navigationTargetArgumentCaptor.getValue();
        assertThat(resultNavigationTarget.screen()).isEqualTo(Screen.DEEPLINK);
        assertThat(resultNavigationTarget.referrer()).isEqualTo(Optional.of(Referrer.STREAM_NOTIFICATION.value()));
        assertThat(resultNavigationTarget.target()).isEqualTo("content://" + SyncConfig.AUTHORITY + "/tracks/#");
    }

    @Test
    public void shouldTrackScreen() {
        intentResolver.handle(activity(), intent);

        verify(tracker).trackMainScreenEvent();
    }
}
