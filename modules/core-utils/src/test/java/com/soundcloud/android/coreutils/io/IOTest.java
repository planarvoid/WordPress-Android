package com.soundcloud.android.coreutils.io;

import static java.lang.System.nanoTime;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public class IOTest {

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    private IO io;
    @Mock private Context context;

    @Before
    public void setUp() {
        initMocks(this);
        when(context.getApplicationContext()).thenReturn(context);
        io = new IO(context);
    }


    @Test
    public void shouldReturnTrueIfFileExistsInPrivateDirectory () throws IOException {
        File tempFile = createTempFile();
        when(context.getFilesDir()).thenReturn(tempFile.getParentFile());
        assertThat(io.fileExistsInPrivateDirectory(tempFile.getName()), is(true));

    }

    @Test
    public void shouldReturnFalseIfFileDoesNotExistInPrivateDirectory() throws IOException {
        when(context.getFilesDir()).thenReturn(createTempFile());
        assertThat(io.fileExistsInPrivateDirectory("nonexistantfile"), is(false));
    }

    @Test
    public void shouldReturnFileInPrivateDirectoryWithoutCreatingItIfItExists() throws IOException {
        File tempFile = createTempFile();
        when(context.getFilesDir()).thenReturn(tempFile.getParentFile());
        File privateFile = io.createFileInPrivateDirectory("temp");
        assertThat(privateFile.exists(), is(true));
    }

    @Test
    public void shouldCreateNewFileInPrivateDirectoryIfItDoesNotExists() throws IOException {
        when(context.getFilesDir()).thenReturn(new File(TEMP_DIR));
        File privateFile = io.createFileInPrivateDirectory("temp");
        assertThat(privateFile.exists(), is(true));
    }

    @Test(expected = IOException.class)
    public void shouldThrowExceptionIfCannotCreateFileInPrivateDirectory() throws IOException {
        when(context.getFilesDir()).thenReturn(new File("/"));
        io.createFileInPrivateDirectory("temp");
    }

    @Test
    public void shouldCopyAllBytesFromInputToOutput() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream("Hello World".getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long bytesCopied = io.copy(in, out);
        assertThat(out.toString(), is("Hello World"));
        assertThat(out.size(), is(11));
        assertThat(bytesCopied, equalTo(11L));
    }

    @Test
    public void shouldCopyOneByteFromInputToOutput() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[]{-127});
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long bytesCopied = io.copy(in, out);
        assertThat(out.toByteArray(), equalTo(new byte[]{-127}));
        assertThat(out.size(), is(1));
        assertThat(bytesCopied, equalTo(1L));
    }

    @Test
    public void shouldBeAbleToHandleEmptyInputStreamsWhenCopying() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[]{});
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long bytesCopied = io.copy(in, out);
        assertThat(out.size(), is(0));
        assertThat(bytesCopied, equalTo(0L));
    }

    @Test
    public void shouldCloseAllCloseables() throws IOException {
        Closeable c1 = mock(Closeable.class);
        Closeable c2 = mock(Closeable.class);
        io.closeQuietly(c1,c2);

        verify(c1).close();
        verify(c2).close();
    }

    @Test
    public void shouldSwallowExceptionIfOneIsThrownWhenClosing() throws IOException {
        Closeable c1 = mock(Closeable.class);
        doThrow(IOException.class).when(c1).close();
        io.closeQuietly(c1);

    }

    @Test
    public void shouldReturnTheStringInTheBuffer() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream("Hello World".getBytes());
        String str = io.toString(in);
        assertThat(str, is("Hello World"));
    }

    @Test
    public void shouldHandleEmptyInputBuffers() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[]{});
        String str = io.toString(in);
        assertThat(str, is(""));
    }


    private File createTempFile() throws IOException {
        File tempFile = File.createTempFile("temp",Long.toString(nanoTime()));
        tempFile.deleteOnExit();
        return tempFile;
    }


}