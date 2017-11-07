package com.soundcloud.android.search;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.search.TabbedSearchFragment.SearchPagerScreenListener;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentHostCallback;
import android.support.v4.view.ViewPager;
import android.view.View;

import java.lang.reflect.Field;

public class TabbedSearchFragmentTest extends AndroidUnitTest {

    private static final String SEARCH_QUERY = "query";

    private TabbedSearchFragment fragment;

    @Mock private FragmentHostCallback mockFragmentHost;
    @Mock private View mockLayout;
    @Mock private TabLayout mockTabLayout;
    @Mock private ViewPager mockViewPager;
    @Mock private SearchTracker searchTracker;

    @Before
    public void setUp() throws Exception {
        when(mockLayout.findViewById(R.id.pager)).thenReturn(mockViewPager);
        when(mockLayout.findViewById(R.id.tab_indicator)).thenReturn(mockTabLayout);

        fragment = new TabbedSearchFragment(resources(), searchTracker);
    }

    @Test
    public void shouldAddListenerToViewPagerForTrackingScreenEvents() {
        fragment.setArguments(new Bundle());
        setFragmentHost();

        fragment.onViewCreated(mockLayout, null);
        verify(mockViewPager).addOnPageChangeListener(isA(SearchPagerScreenListener.class));
    }

    @Test
    public void shouldTrackSearchAllScreenOnPageSelected() throws Exception {
        SearchPagerScreenListener listener = new SearchPagerScreenListener(searchTracker, SEARCH_QUERY);
        listener.onPageSelected(0);
        verify(searchTracker).trackResultsScreenEvent(SearchType.ALL, SEARCH_QUERY, SearchOperations.ContentType.NORMAL);
    }

    @Test
    public void shouldTrackSearchTracksScreenOnPageSelected() throws Exception {
        SearchPagerScreenListener listener = new SearchPagerScreenListener(searchTracker, SEARCH_QUERY);
        listener.onPageSelected(1);
        verify(searchTracker).trackResultsScreenEvent(SearchType.TRACKS, SEARCH_QUERY, SearchOperations.ContentType.NORMAL);
    }

    @Test
    public void shouldTrackSearchPeopleScreenOnPageSelected() throws Exception {
        SearchPagerScreenListener listener = new SearchPagerScreenListener(searchTracker, SEARCH_QUERY);
        listener.onPageSelected(2);
        verify(searchTracker).trackResultsScreenEvent(SearchType.USERS, SEARCH_QUERY, SearchOperations.ContentType.NORMAL);
    }

    @Test
    public void shouldTrackSearchAlbumsScreenOnPageSelected() throws Exception {
        SearchPagerScreenListener listener = new SearchPagerScreenListener(searchTracker, SEARCH_QUERY);
        listener.onPageSelected(3);
        verify(searchTracker).trackResultsScreenEvent(SearchType.ALBUMS, SEARCH_QUERY, SearchOperations.ContentType.NORMAL);
    }

    @Test
    public void shouldTrackSearchPlaylistsScreenOnPageSelected() throws Exception {
        SearchPagerScreenListener listener = new SearchPagerScreenListener(searchTracker, SEARCH_QUERY);
        listener.onPageSelected(4);
        verify(searchTracker).trackResultsScreenEvent(SearchType.PLAYLISTS, SEARCH_QUERY, SearchOperations.ContentType.NORMAL);
    }

    /**
     * When testing calls on the fragment lifecycle methods (such as onViewCreated), the FragmentManager
     * is accessed and will crash if no host is set. In the testing env, we can just override that and
     * set a mocked host instead - unfortunately there is no `set` method so we'll do it with reflection.
     */
    private void setFragmentHost() {
        try {
            Field hostField = fragment.getClass().getSuperclass().getDeclaredField("mHost");
            hostField.setAccessible(true);
            hostField.set(fragment, mockFragmentHost);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            fail("Failed to set fragment host");
        }
    }
}
