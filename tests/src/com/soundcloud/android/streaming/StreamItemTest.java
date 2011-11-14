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
        StreamItem item = new StreamItem("https://api.soundcloud.com/tracks/1/stream");
        expect(item.urlHash).toEqual("c26bb316a6122e91fb249866dd0b46e9");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRequireURL() throws Exception {
        new StreamItem((String) null);
    }

    @Test
    public void shouldHaveEqualsAndHashBasedOnUrl() throws Exception {
        StreamItem a = new StreamItem("https://api.soundcloud.com/tracks/1/stream");
        StreamItem b = new StreamItem("https://api.soundcloud.com/tracks/2/stream");

        expect(a).toEqual(new StreamItem("https://api.soundcloud.com/tracks/1/stream"));
        expect(a).not.toEqual(b);
    }

    @Test
    public void testByteRange() throws Exception {
        StreamItem item = new StreamItem("https://api.soundcloud.com/tracks/1/stream", 1543, null);
        expect(item.byteRange()).toEqual(Range.from(0, 1543));
    }

    @Test
    public void testChunkRange() throws Exception {
        StreamItem item = new StreamItem("https://api.soundcloud.com/tracks/1/stream", 1543, null);
        expect(item.chunkRange(128)).toEqual(Range.from(0, 13));
    }

    @Test
    public void shouldWriteAndReadMetadata() throws Exception {
        StreamItem md = new StreamItem("https://api.soundcloud.com/tracks/1/stream", 100, "etag");
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
        StreamItem fred = new StreamItem("https://api.soundcloud.com/tracks/1/stream",
                new File(getClass().getResource("fred.mp3").getFile()));
        expect(fred.etag()).toEqual("\"ecc03db74b5b8cdb8d64c1cc4d04f68a\"");
    }

    @Test
    public void testGetTrackId() throws Exception {
        expect(StreamItem.getTrackId("http://api.soundcloud.com/tracks/10853436/stream")).toEqual(10853436l);
        expect(StreamItem.getTrackId("https://api.soundcloud.com/tracks/blargh/stream")).toEqual(-1l);
    }
}
