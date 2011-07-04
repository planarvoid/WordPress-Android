package com.soundcloud.android.activity;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xtremelabs.robolectric.Robolectric;

@RunWith(DefaultTestRunner.class)
public class ScSearchTest {
    @Test
    public void shouldSearch() throws Exception {
        // followings
        Robolectric.addPendingHttpResponse(404, "Not found");
        // actual search
        Robolectric.addPendingHttpResponse(404, "Not found");

        Robolectric.pauseMainLooper();
        Robolectric.application.onCreate();

        ScSearch search = new ScSearch();
        search.onCreate(null);
        search.doSearch("Testing");
        // TODO add real tests here
    }
}
