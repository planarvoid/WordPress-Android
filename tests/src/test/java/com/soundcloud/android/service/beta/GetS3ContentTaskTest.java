package com.soundcloud.android.service.beta;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;


@RunWith(DefaultTestRunner.class)
public class GetS3ContentTaskTest {
    @Test
    public void testTask() throws Exception {
        TestHelper.addCannedResponses(getClass(), "bucket_contents.xml");
        List<Beta> content = new GetS3ContentTask(new DefaultHttpClient()).doInBackground(BetaService.BETA_BUCKET);
        expect(content.size()).toEqual(2);
    }

    @Test
    public void testTaskFailure() throws Exception {
        Robolectric.addPendingHttpResponse(503, "error");
        List<Beta> content = new GetS3ContentTask(new DefaultHttpClient()).doInBackground(BetaService.BETA_BUCKET);
        expect(content).toBeNull();
    }
}
