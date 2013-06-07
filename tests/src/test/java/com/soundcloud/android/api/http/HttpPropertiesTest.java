package com.soundcloud.android.api.http;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class HttpPropertiesTest {

    @Test
    public void shouldDeobfuscateClientSecret() throws Exception {
        // live
        expect(new HttpProperties(Robolectric.application.getResources()).getClientSecret())
                .toEqual("26a5240f7ee0ee2d4fa9956ed80616c2");

    }
}
