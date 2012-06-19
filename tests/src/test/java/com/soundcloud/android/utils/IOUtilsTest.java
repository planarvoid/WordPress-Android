package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;

import org.junit.Test;

import java.io.File;

public class IOUtilsTest {
    @Test
    public void testMD5() throws Exception {
        expect(IOUtils.md5("foo")).toEqual("acbd18db4cc2f85cedef654fccc4a4d8");
        expect(IOUtils.md5("000012345")).toEqual("4748cdb4de48635e843db0670e1ad47a");
        expect(IOUtils.md5("00001234588888")).toEqual("1dff78cccd58a9a316d872a9d6d08db2");
    }

    @Test
    public void shouldGetUsableSpace() throws Exception {
        final long mb = 1024*1024;
        long usedSpace = 3 * mb;
        long spaceLeft = 200 * mb;
        long maxSpace = 10 * mb;
        double maxPct = 0.1d;

        expect(IOUtils.getUsableSpace(usedSpace, spaceLeft, maxSpace, maxPct)).toEqual(maxSpace);

        spaceLeft = 5 * mb;
        expect(IOUtils.getUsableSpace(usedSpace, spaceLeft, maxSpace, maxPct)).toEqual(838860L);

        maxPct = 0.01d;
        expect(IOUtils.getUsableSpace(usedSpace, spaceLeft, maxSpace, maxPct)).toEqual(83886L);
    }

    @Test
    public void shouldGetMBFormatted() throws Exception {
        expect(IOUtils.inMbFormatted(1024*1024)).toEqual("1");
        expect(IOUtils.inMbFormatted(2.3d * 1024*1024)).toEqual("2.3");
    }

    @Test
    public void shouldAppendToFilename() throws Exception {
        File f = new File("/foo/bar/test.ogg");
        expect(IOUtils.appendToFilename(f, "_processed").getAbsolutePath()).toEqual("/foo/bar/test_processed.ogg");

        File g = new File("/foo/bar/test");
        expect(IOUtils.appendToFilename(g, "_processed").getAbsolutePath()).toEqual("/foo/bar/test_processed");
    }

    @Test
    public void shouldGetExtension() throws Exception {
        expect(IOUtils.extension(new File("foo.ogg"))).toEqual("ogg");
        expect(IOUtils.extension(new File("foo.baz.Ogg"))).toEqual("ogg");
        expect(IOUtils.extension(new File("foo."))).toBeNull();
        expect(IOUtils.extension(new File("foo"))).toBeNull();
    }

}
