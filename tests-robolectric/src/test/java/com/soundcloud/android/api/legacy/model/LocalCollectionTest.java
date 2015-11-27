package com.soundcloud.android.api.legacy.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;

@RunWith(SoundCloudTestRunner.class)
public class LocalCollectionTest {

    @Test
    public void shouldSupportEqualsAndHashcode() throws Exception {
        LocalCollection c1 = new LocalCollection(Uri.parse("foo"), 1, 1, 0, 0, null);
        LocalCollection c2 = new LocalCollection(Uri.parse("foo"), 1, 1, 0, 0, null);
        LocalCollection c3 = new LocalCollection(Uri.parse("foo"), 1, 1, 0, 0, null);
        c3.setId(100);
        expect(c1).toEqual(c2);
        expect(c2).not.toEqual(c3);
    }
}
