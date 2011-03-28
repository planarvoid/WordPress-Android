package com.soundcloud.android.activity;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(DefaultTestRunner.class)
public class ScSearchTests {
    @Test
    public void shouldSearch() throws Exception {
        Robolectric.pauseMainLooper();
        ScSearch search = new ScSearch();
        search.onCreate(null);
        search.doSearch("Testing");
        // TODO add real tests here
    }
}
