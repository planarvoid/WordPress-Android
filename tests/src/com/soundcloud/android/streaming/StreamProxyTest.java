package com.soundcloud.android.streaming;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

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

    @Test
    public void testCreateHeader() throws Exception {
        StreamProxy proxy = new StreamProxy(DefaultTestRunner.application, 0);

        Map<String, String> h = proxy.headerForItem("http://foo.com/naz");

        expect(h.containsKey("Server")).toBeTrue();
        expect(h.containsKey("Content-Type")).toBeTrue();
        expect(h.get("Content-Type")).toEqual("audio/mpeg");
    }
}
