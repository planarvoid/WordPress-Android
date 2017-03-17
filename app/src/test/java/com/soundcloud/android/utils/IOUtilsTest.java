package com.soundcloud.android.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import android.content.Context;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

public class IOUtilsTest extends AndroidUnitTest {

    private static final long MB = 1024 * 1024;

    @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void createExternalStorageDir() throws IOException {
        File file1 = tempFolder.newFile("file1");
        Context context = mock(Context.class);

        when(context.getExternalFilesDirs(null)).thenReturn(null);
        assertThat(IOUtils.createExternalStorageDir(context, "external")).isNull();

        when(context.getExternalFilesDirs(null)).thenReturn(new File[]{});
        assertThat(IOUtils.createExternalStorageDir(context, "external")).isNull();

        when(context.getExternalFilesDirs(null)).thenReturn(new File[]{file1});
        assertThat(IOUtils.createExternalStorageDir(context, "external")).isNotNull();
    }

    @Test
    public void createSDCardDir() throws IOException {
        File file1 = tempFolder.newFile("file1");
        File file2 = tempFolder.newFile("file2");
        Context context = mock(Context.class);

        when(context.getExternalFilesDirs(null)).thenReturn(null);
        assertThat(IOUtils.createSDCardDir(context, "sdcard")).isNull();

        when(context.getExternalFilesDirs(null)).thenReturn(new File[]{});
        assertThat(IOUtils.createSDCardDir(context, "sdcard")).isNull();

        when(context.getExternalFilesDirs(null)).thenReturn(new File[]{file1});
        assertThat(IOUtils.createSDCardDir(context, "sdcard")).isNull();

        when(context.getExternalFilesDirs(null)).thenReturn(new File[]{file1, file2});
        assertThat(IOUtils.createSDCardDir(context, "sdcard")).isNotNull();
    }

    @Test
    public void getExternalStorageDir() throws IOException {
        File file1 = tempFolder.newFile("file1");
        Context context = mock(Context.class);

        when(context.getExternalFilesDirs(null)).thenReturn(null);
        assertThat(IOUtils.getExternalStorageDir(context)).isNull();

        when(context.getExternalFilesDirs(null)).thenReturn(new File[]{});
        assertThat(IOUtils.getExternalStorageDir(context)).isNull();

        when(context.getExternalFilesDirs(null)).thenReturn(new File[]{file1});
        assertThat(IOUtils.getExternalStorageDir(context)).isEqualTo(file1);
    }

    @Test
    public void isSDCardMounted() throws IOException {
        Context context = mock(Context.class);

        when(context.getExternalFilesDirs(null)).thenReturn(null);
        assertThat(IOUtils.isSDCardMounted(context)).isFalse();
    }

    @Test
    public void getExternalStorageFreeSpace() throws IOException {
        File file1 = tempFolder.newFile("file1");
        Context context = mock(Context.class);

        when(context.getExternalFilesDirs(null)).thenReturn(null);
        assertThat(IOUtils.getExternalStorageFreeSpace(context)).isEqualTo(0);

        when(context.getExternalFilesDirs(null)).thenReturn(new File[]{file1});
        assertThat(IOUtils.getExternalStorageFreeSpace(context)).isGreaterThan(0);
    }

    @Test
    public void getExternalStorageTotalSpace() throws IOException {
        File file1 = tempFolder.newFile("file1");
        Context context = mock(Context.class);

        when(context.getExternalFilesDirs(null)).thenReturn(null);
        assertThat(IOUtils.getExternalStorageTotalSpace(context)).isEqualTo(0);

        when(context.getExternalFilesDirs(null)).thenReturn(new File[]{file1});
        assertThat(IOUtils.getExternalStorageTotalSpace(context)).isGreaterThan(0);
    }

    @Test
    public void getSDCardFreeSpace() throws IOException {
        File file1 = tempFolder.newFile("file1");
        File file2 = tempFolder.newFile("file2");
        Context context = mock(Context.class);

        when(context.getExternalFilesDirs(null)).thenReturn(null);
        assertThat(IOUtils.getSDCardStorageFreeSpace(context)).isEqualTo(0);

        when(context.getExternalFilesDirs(null)).thenReturn(new File[]{file1, file2});
        assertThat(IOUtils.getSDCardStorageFreeSpace(context)).isGreaterThan(0);
    }

    @Test
    public void getSDCardTotalSpace() throws IOException {
        File file1 = tempFolder.newFile("file1");
        File file2 = tempFolder.newFile("file2");
        Context context = mock(Context.class);

        when(context.getExternalFilesDirs(null)).thenReturn(null);
        assertThat(IOUtils.getSDCardStorageTotalSpace(context)).isEqualTo(0);

        when(context.getExternalFilesDirs(null)).thenReturn(new File[]{file1, file2});
        assertThat(IOUtils.getSDCardStorageTotalSpace(context)).isGreaterThan(0);
    }

    @Test
    public void maxUsableSpaceIsCappedBySpaceLeft() {
        long spaceLeft = 20 * MB;
        long maxSpace = 60 * MB;

        assertThat(IOUtils.getMaxUsableSpace(spaceLeft, maxSpace)).isEqualTo(20 * MB);
    }

    @Test
    public void maxUsableSpaceIsCappedByMaxSpace() {
        long spaceLeft = 200 * MB;
        long maxSpace = 60 * MB;

        assertThat(IOUtils.getMaxUsableSpace(spaceLeft, maxSpace)).isEqualTo(60 * MB);
    }

    @Test
    public void getFolderSizeMBFormatted() {
        File folder1 = new File(getClass().getResource("io/folder1").getPath());
        File folder2 = new File(getClass().getResource("io/folder2").getPath());

        assertThat(IOUtils.inMbFormatted(folder1, folder2)).isEqualTo("8.8");
    }

    @Test
    public void shouldGetMBFormatted() throws Exception {
        assertThat(IOUtils.inMbFormatted(1024 * 1024)).isEqualTo("1");
        assertThat(IOUtils.inMbFormatted(2.3d * 1024 * 1024)).isEqualTo("2.3");
    }

    @Test
    public void shouldAppendToFilename() throws Exception {
        File f = new File("/foo/bar/test.ogg");
        assertThat(IOUtils.appendToFilename(f, "_processed")
                          .getAbsolutePath()).isEqualTo("/foo/bar/test_processed.ogg");

        File g = new File("/foo/bar/test");
        assertThat(IOUtils.appendToFilename(g, "_processed").getAbsolutePath()).isEqualTo("/foo/bar/test_processed");
    }

    @Test
    public void shouldGetExtension() throws Exception {
        assertThat(IOUtils.extension(new File("foo.ogg"))).isEqualTo("ogg");
        assertThat(IOUtils.extension(new File("foo.baz.Ogg"))).isEqualTo("ogg");
        assertThat(IOUtils.extension(new File("foo."))).isNull();
        assertThat(IOUtils.extension(new File("foo"))).isNull();
    }

    @Test
    public void shouldChangeExtension() throws Exception {
        assertThat(IOUtils.changeExtension(new File("test.ogg"), "wav").getName()).isEqualTo("test.wav");
        assertThat(IOUtils.changeExtension(new File("test.ogg.baz"), "wav").getName()).isEqualTo("test.ogg.wav");
        assertThat(IOUtils.changeExtension(new File("test"), "wav").getName()).isEqualTo("test.wav");
    }

    @Test
    public void shouldGetDirSize() throws Exception {
        assertThat(IOUtils.getDirSize(new File("no-existo"))).isEqualTo(0L);
        assertThat(IOUtils.getDirSize(tempFolder.getRoot())).isEqualTo(0L);
        File f1 = tempFolder.newFile("aFile");
        File dir = new File(tempFolder.getRoot(), "aDir");
        assertThat(dir.mkdir()).isTrue();
        File f2 = new File(dir, "nestedFile");
        OutputStream os = new FileOutputStream(f1);
        os.write(new byte[8192]);
        os.close();
        OutputStream os2 = new FileOutputStream(f2);
        os2.write(new byte[1024]);
        os2.close();
        assertThat(IOUtils.getDirSize(tempFolder.getRoot())).isEqualTo(8192 + 1024L);
        tempFolder.delete();
    }

    @Test
    public void shouldRemoveExtension() throws Exception {
        assertThat(IOUtils.removeExtension(new File("foo.ogg")).getName()).isEqualTo("foo");
        assertThat(IOUtils.removeExtension(new File("foo.ogg.ogg")).getName()).isEqualTo("foo.ogg");
        assertThat(IOUtils.removeExtension(new File("foo")).getName()).isEqualTo("foo");
    }

    @Test
    public void cleanDirShouldEmptyTheDirectory() throws IOException {
        tempFolder.newFile("file.txt");
        tempFolder.newFolder("folder1");
        tempFolder.newFile("folder1/file.txt");
        tempFolder.newFolder("folder1", "subFolder");
        tempFolder.newFile("folder1/subFolder/file1.txt");
        tempFolder.newFile("folder1/subFolder/file2.txt");

        IOUtils.cleanDir(tempFolder.getRoot());

        assertThat(tempFolder.getRoot().exists()).isTrue();
        assertThat(tempFolder.getRoot().list().length).isEqualTo(0);
    }

    @Test
    public void cleanDirsShouldEmptyEachDir() throws IOException {
        File dir1 = tempFolder.newFolder("folder1");
        File dir2 = tempFolder.newFolder("folder2");
        tempFolder.newFile("folder1/file.txt");
        tempFolder.newFile("folder2/file.txt");

        IOUtils.cleanDirs(dir1, dir2);

        assertThat(dir1.list().length).isEqualTo(0);
        assertThat(dir2.list().length).isEqualTo(0);
    }

    @Test
    public void consumeContentShouldConsumeResponsePayload() throws IOException {
        InputStream payload = new ByteArrayInputStream(new byte[]{1, 3, 3, 7});
        assertThat(payload.available()).isEqualTo(4);

        HttpURLConnection connection = mock(HttpURLConnection.class);

        when(connection.getContentLength()).thenReturn(payload.available());
        when(connection.getInputStream()).thenReturn(payload);

        IOUtils.consumeStream(connection);

        assertThat(payload.available()).isEqualTo(0);
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
