package com.soundcloud.android.streaming;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class StreamProxyTest {

    @Test @Ignore
    public void testStartProxy() throws Exception {
        StreamProxy proxy = new StreamProxy(DefaultTestRunner.application, 0);
        proxy.loader.setForceOnline(true);
        proxy.init()
             .start()
             .join();
    }
}
