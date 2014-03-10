package com.soundcloud.android.analytics.localytics;

import static org.mockito.Mockito.verify;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class LocalyticsAnalyticsProviderSearchTest {

    private LocalyticsAnalyticsProvider provider;

    @Mock
    private LocalyticsSession localyticsSession;

    @Before
    public void setUp() throws CreateModelException {
        provider = new LocalyticsAnalyticsProvider(localyticsSession, null);
    }

    @Test
    public void shouldTrackSearchSuggestions() {
        SearchEvent event = SearchEvent.searchSuggestion(Content.TRACK, true);
        provider.handleSearchEvent(event);
        verify(localyticsSession).tagEvent("Search suggestion", event.getAttributes());
    }

    @Test
    public void shouldTrackSearchSubmit() {
        SearchEvent event = SearchEvent.recentTagSearch("query");
        provider.handleSearchEvent(event);
        verify(localyticsSession).tagEvent("Search submit", event.getAttributes());
    }

    @Test
    public void shouldTrackSearchResults() {
        SearchEvent event = SearchEvent.tapTrackOnScreen(Screen.SEARCH_EVERYTHING);
        provider.handleSearchEvent(event);
        verify(localyticsSession).tagEvent("Search results", event.getAttributes());
    }

}
