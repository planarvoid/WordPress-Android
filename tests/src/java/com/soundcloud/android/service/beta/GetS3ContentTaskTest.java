package com.soundcloud.android.service.beta;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;


@RunWith(DefaultTestRunner.class)
public class GetS3ContentTaskTest {
    @Test
    public void testTask() throws Exception {
        Robolectric.addPendingHttpResponse(200, resource("bucket_contents.xml"));
        List<Beta> content = new GetS3ContentTask(new DefaultHttpClient()).doInBackground(BetaService.BETA_BUCKET);
        assertThat(content.size(), is(2));
    }

    @Test
    public void testTaskFailure() throws Exception {
        Robolectric.addPendingHttpResponse(503, "error");
        List<Beta> content = new GetS3ContentTask(new DefaultHttpClient()).doInBackground(BetaService.BETA_BUCKET);
        assertThat(content, nullValue());
    }

    protected String resource(String res) throws IOException {
        StringBuilder sb = new StringBuilder(65536);
        int n;
        byte[] buffer = new byte[8192];
        InputStream is = getClass().getResourceAsStream(res);
        while ((n = is.read(buffer)) != -1) sb.append(new String(buffer, 0, n));
        return sb.toString();
    }
}
