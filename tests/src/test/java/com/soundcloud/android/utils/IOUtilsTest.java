package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class IOUtilsTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

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

    @Test
    public void shouldChangeExtension() throws Exception {
        expect(IOUtils.changeExtension(new File("test.ogg"), "wav").getName()).toEqual("test.wav");
        expect(IOUtils.changeExtension(new File("test.ogg.baz"), "wav").getName()).toEqual("test.ogg.wav");
        expect(IOUtils.changeExtension(new File("test"), "wav").getName()).toEqual("test.wav");
    }

    @Test
    public void shouldGetDirSize() throws Exception {
        expect(IOUtils.getDirSize(new File("no-existo"))).toEqual(0l);
        expect(IOUtils.getDirSize(tempFolder.getRoot())).toEqual(0l);
        File f1 = tempFolder.newFile("aFile");
        File dir = new File(tempFolder.getRoot(), "aDir");
        expect(dir.mkdir()).toBeTrue();
        File f2 = new File(dir, "nestedFile");
        OutputStream os = new FileOutputStream(f1);
        os.write(new byte[8192]);
        os.close();
        OutputStream os2 = new FileOutputStream(f2);
        os2.write(new byte[1024]);
        os2.close();
        expect(IOUtils.getDirSize(tempFolder.getRoot())).toEqual(8192 + 1024l);
        tempFolder.delete();
    }

    @Test
    public void shouldRemoveExtension() throws Exception {
        expect(IOUtils.removeExtension(new File("foo.ogg")).getName()).toEqual("foo");
        expect(IOUtils.removeExtension(new File("foo.ogg.ogg")).getName()).toEqual("foo.ogg");
        expect(IOUtils.removeExtension(new File("foo")).getName()).toEqual("foo");
    }
}
