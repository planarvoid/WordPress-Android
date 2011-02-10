package com.soundcloud.android.task;

import com.soundcloud.android.api.ApiTest;
import com.soundcloud.utils.http.ProgressListener;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.apache.http.entity.mime.content.ContentBody;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.omg.CORBA.NameValuePair;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.verify;

@SuppressWarnings({"unchecked"})
@RunWith(RobolectricTestRunner.class)
public class UploadTaskTests extends ApiTest{
    UploadTask task;

    @Before
    public void setup() {
        task = new UploadTask(api);
    }

    @Test
    public void testMultiParamsArePreserved() throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(UploadTask.Params.PCM_PATH, "/tmp/in");
        map.put(UploadTask.Params.OGG_FILENAME, "/tmp/out");
        map.put("foo", "bar");
        map.put("multi", Arrays.asList("1", "2", "3"));

        UploadTask.Params params = new UploadTask.Params(map);

        task.execute(params);
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);

        verify(api).upload(Matchers.<ContentBody>any(),
                Matchers.<ContentBody>any(),
                captor.capture(),
                Matchers.<ProgressListener>any());

        List<NameValuePair> pairs = captor.getValue();
        assertEquals(4, pairs.size());
    }
}
