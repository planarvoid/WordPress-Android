package com.soundcloud.android.streaming;


import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class StreamItemTest {
     @Test
    public void shouldGetHashKey() throws Exception {
        StreamItem item = new StreamItem("http://asdf.com");
        expect(item.getURLHash()).toEqual("b0ecbe2bc0fd8e426395c81ee96f81cf");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRequireURL() throws Exception {
        new StreamItem((String)null);
    }

    @Test
    public void shouldHaveEqualsAndHashBasedOnUrl() throws Exception {
        StreamItem a = new StreamItem("http://a.com");
        StreamItem b = new StreamItem("http://b.com");

        expect(a).toEqual(new StreamItem("http://a.com"));
        expect(a).not.toEqual(b);
    }

    @Test
    public void testByteRange() throws Exception {
        StreamItem item = new StreamItem("foo", 1543);
        expect(item.byteRange()).toEqual(Range.from(0, 1543));
    }

    @Test
    public void testChunkRange() throws Exception {
        StreamItem item = new StreamItem("foo", 1543);
        expect(item.chunkRange(128)).toEqual(Range.from(0, 13));
    }
}
