package com.soundcloud.android.task;

import com.soundcloud.android.api.ApiTest;
import com.soundcloud.utils.http.Http;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.entity.mime.content.ContentBody;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.omg.CORBA.NameValuePair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked"})
@RunWith(RobolectricTestRunner.class)
public class UploadTaskTests extends ApiTest {
    UploadTask task;

    @Before
    public void setup() {
        task = new UploadTask(api);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenFileIsMissing() {

        Map<String, Object> map = new HashMap<String, Object>();
        UploadTask.Params params = new UploadTask.Params(map);

        task.execute(params);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenFileIsEmpty() throws IOException {
        File tmp = File.createTempFile("temp", ".ogg");

        Map<String, Object> map = new HashMap<String, Object>();
        map.put(UploadTask.Params.SOURCE_PATH, tmp.getAbsoluteFile());
        UploadTask.Params params = new UploadTask.Params(map);

        task.execute(params);
    }

    @Test
    public void shouldPreserveMultiparams() throws Exception {
        File tmp = getTestFile();

        Map<String, Object> map = new HashMap<String, Object>();
        map.put(UploadTask.Params.SOURCE_PATH, tmp.getAbsolutePath());
        map.put("foo", "bar");
        map.put("multi", Arrays.asList("1", "2", "3"));


        UploadTask.Params params = new UploadTask.Params(map);

        task.execute(params);
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);

        verify(api).upload(Matchers.<ContentBody>any(),
                Matchers.<ContentBody>any(),
                captor.capture(),
                Matchers.<Http.ProgressListener>any());

        List<NameValuePair> pairs = captor.getValue();
        assertEquals(4, pairs.size());
    }

    @Test
    public void shouldSucceedWhenUploadSucceeds() throws Exception {
        File tmp = getTestFile();

        Map<String, Object> map = new HashMap<String, Object>();
        map.put(UploadTask.Params.SOURCE_PATH,  tmp.getAbsolutePath());

        UploadTask.Params params = new UploadTask.Params(map);

        HttpResponse response = mock(HttpResponse.class);
        StatusLine status = mock(StatusLine.class);
        when(response.getStatusLine()).thenReturn(status);
        when(status.getStatusCode()).thenReturn(201);

        when(api.upload(Matchers.<ContentBody>any(),
                Matchers.<ContentBody>any(),
                anyList(), Matchers.<Http.ProgressListener>any())).thenReturn(response);

        task.execute(params);

        assertTrue(params.isSuccess());
    }

    private File getTestFile() throws IOException {
        File tmp = File.createTempFile("temp", ".ogg");

        PrintWriter pw = new PrintWriter(new FileOutputStream(tmp));
        pw.print("123");
        pw.close();
        return tmp;
    }


    @Test
    public void shouldRespectTheStatusCode() throws Exception {
        File tmp = getTestFile();

        Map<String, Object> map = new HashMap<String, Object>();
        map.put(UploadTask.Params.SOURCE_PATH, "/tmp/in");
        map.put(UploadTask.Params.OGG_FILENAME, tmp.getAbsolutePath());
        map.put(UploadTask.Params.ENCODE, "true");

        UploadTask.Params params = new UploadTask.Params(map);

        HttpResponse response = mock(HttpResponse.class);
        StatusLine status = mock(StatusLine.class);
        when(response.getStatusLine()).thenReturn(status);
        when(status.getStatusCode()).thenReturn(400);

        when(api.upload(Matchers.<ContentBody>any(),
                Matchers.<ContentBody>any(),
                anyList(), Matchers.<Http.ProgressListener>any())).thenReturn(response);

        task.execute(params);

        assertFalse(params.isSuccess());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRequireOggParameterWhenEncoding() throws Exception {
        File tmp = getTestFile();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(UploadTask.Params.SOURCE_PATH, tmp.getAbsolutePath());
        map.put(UploadTask.Params.ENCODE, "true");

        UploadTask.Params params = new UploadTask.Params(map);
        task.execute(params);
    }

    @Test
    public void shouldUploadOriginalFileWhenNotEncoding() throws Exception {
        File tmp = getTestFile();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(UploadTask.Params.SOURCE_PATH, tmp.getAbsolutePath());

        UploadTask.Params params = new UploadTask.Params(map);

        HttpResponse response = mock(HttpResponse.class);
        StatusLine status = mock(StatusLine.class);
        when(response.getStatusLine()).thenReturn(status);
        when(status.getStatusCode()).thenReturn(201);

        when(api.upload(Matchers.<ContentBody>any(),
                Matchers.<ContentBody>any(),
                anyList(), Matchers.<Http.ProgressListener>any())).thenReturn(response);

        task.execute(params);
    }
}
