package com.soundcloud.android.activity;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.soundcloud.android.robolectric.DefaultTestRunner;

import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


@RunWith(DefaultTestRunner.class)
public class UserBrowserTest {

    @Test
    public void testOnCreate() throws Exception {
        Robolectric.addPendingHttpResponse(404, "");

        UserBrowser browser = new UserBrowser();
        // TODO
        //browser.onCreate(null);
    }
}
