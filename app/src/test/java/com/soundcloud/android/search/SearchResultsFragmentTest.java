package com.soundcloud.android.search;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

@RunWith(SoundCloudTestRunner.class)
public class SearchResultsFragmentTest {

    SearchResultsFragment fragment;

    @Mock
    SearchOperations searchOperations;

    @Mock
    SearchResultsAdapter adapter;

    @Before
    public void setUp() throws Exception {
        fragment = new SearchResultsFragment(searchOperations, adapter);
        Robolectric.shadowOf(fragment).setActivity(mock(FragmentActivity.class));
    }

    @Test
    public void shouldGetSearchResultsOnCreate() throws Exception {
        Bundle arguments = new Bundle();
        arguments.putString("query", "a query");
        fragment.setArguments(arguments);

        fragment.onCreate(null);

        verify(searchOperations).getSearchResults("a query");
    }
}
