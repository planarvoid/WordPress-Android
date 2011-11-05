package com.soundcloud.android.streaming;


import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.util.Arrays;

@RunWith(DefaultTestRunner.class)
public class StreamItemTest {
    @Test
    public void shouldGetHashKey() throws Exception {
        StreamItem item = new StreamItem("http://asdf.com");
        expect(item.urlHash).toEqual("b0ecbe2bc0fd8e426395c81ee96f81cf");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRequireURL() throws Exception {
        new StreamItem((String) null);
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
        StreamItem item = new StreamItem("foo", 1543, null);
        expect(item.byteRange()).toEqual(Range.from(0, 1543));
    }

    @Test
    public void testChunkRange() throws Exception {
        StreamItem item = new StreamItem("foo", 1543, null);
        expect(item.chunkRange(128)).toEqual(Range.from(0, 13));
    }

    @Test
    public void shouldWriteAndReadMetadata() throws Exception {
        StreamItem md = new StreamItem("foo", 100, "etag");
        md.downloadedChunks.addAll(Arrays.asList(1, 2, 3, 4, 5));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        md.write(new DataOutputStream(bos));

        StreamItem md_ = StreamItem.read(
                new DataInputStream(new ByteArrayInputStream(bos.toByteArray())));

        expect(md.getContentLength()).toEqual(md_.getContentLength());
        expect(md.etag()).toEqual(md_.etag());
        expect(md.downloadedChunks).toEqual(md_.downloadedChunks);
    }

    @Test
    public void shouldGenerateEtagFromFile() throws Exception {
        StreamItem fred = new StreamItem("fred", new File(getClass().getResource("fred.mp3").getFile()));
        expect(fred.etag()).toEqual("\"ecc03db74b5b8cdb8d64c1cc4d04f68a\"");
    }
}
