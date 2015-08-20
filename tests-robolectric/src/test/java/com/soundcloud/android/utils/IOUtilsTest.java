package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

public class IOUtilsTest {

    private static final long MB = 1024 * 1024;

    @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void maxUsableSpaceIsCappedBySpaceLeft() {
        long spaceLeft = 20 * MB;
        long maxSpace = 60 * MB;

        expect(IOUtils.getMaxUsableSpace(spaceLeft, maxSpace)).toEqual(20 * MB);
    }

    @Test
    public void maxUsableSpaceIsCappedByMaxSpace() {
        long spaceLeft = 200 * MB;
        long maxSpace = 60 * MB;

        expect(IOUtils.getMaxUsableSpace(spaceLeft, maxSpace)).toEqual(60 * MB);
    }

    @Test
    public void getFolderSizeMBFormatted() {
        File folder1 = new File(getClass().getResource("io/folder1").getPath());
        File folder2 = new File(getClass().getResource("io/folder2").getPath());

        expect(IOUtils.inMbFormatted(folder1, folder2)).toEqual("8.8");
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

    @Test
    public void cleanDirShouldEmptyTheDirectory() throws IOException {
        tempFolder.newFile("file.txt");
        tempFolder.newFolder("folder1");
        tempFolder.newFile("folder1/file.txt");
        tempFolder.newFolder("folder1/subFolder");
        tempFolder.newFile("folder1/subFolder/file1.txt");
        tempFolder.newFile("folder1/subFolder/file2.txt");

        IOUtils.cleanDir(tempFolder.getRoot());

        expect(tempFolder.getRoot().exists()).toBeTrue();
        expect(tempFolder.getRoot().list().length).toBe(0);
    }

    @Test
    public void cleanDirsShouldEmptyEachDir() throws IOException {
        File dir1 = tempFolder.newFolder("folder1");
        File dir2 = tempFolder.newFolder("folder2");
        tempFolder.newFile("folder1/file.txt");
        tempFolder.newFile("folder2/file.txt");

        IOUtils.cleanDirs(dir1, dir2);

        expect(dir1.list().length).toBe(0);
        expect(dir2.list().length).toBe(0);
    }

    @Test
    public void consumeContentShouldConsumeResponsePayload() throws IOException {
        InputStream payload = new ByteArrayInputStream(new byte[]{1, 3, 3, 7});
        expect(payload.available()).toBe(4);

        HttpURLConnection connection = mock(HttpURLConnection.class);

        when(connection.getContentLength()).thenReturn(payload.available());
        when(connection.getInputStream()).thenReturn(payload);

        IOUtils.consumeStream(connection);

        expect(payload.available()).toBe(0);
    }

    @Test
    public void consumeContentShouldNotThrowForNullConnection() {
        IOUtils.consumeStream(null);
    }

    @Test
    public void consumeContentShouldNotThrowWhenPayloadEmpty() {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getContentLength()).thenReturn(0);

        IOUtils.consumeStream(connection);
    }
}
