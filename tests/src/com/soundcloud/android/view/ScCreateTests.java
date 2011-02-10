package com.soundcloud.android.view;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.LazyActivity;
import com.soundcloud.android.objects.Connection;
import com.soundcloud.android.service.ICloudCreateService;
import com.soundcloud.android.task.UploadTask;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.xtremelabs.robolectric.Robolectric.addPendingHttpResponse;
import static junit.framework.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class ScCreateTests {
    ScCreate create;
    ICloudCreateService service;

    @Before
    public void setup() {
        service = mock(ICloudCreateService.class);
        addPendingHttpResponse(401, "Error");

        final LazyActivity activity = new LazyActivity() {
            @Override
            public SoundCloudApplication getSoundCloudApplication() {
                return new SoundCloudApplication() {
                    { onCreate(); }
                    @Override public String getConsumerKey(boolean b)    { return "xxx"; }
                    @Override public String getConsumerSecret(boolean b) { return "xxx"; }
                };
            }
            @Override
            public ICloudCreateService getCreateService() {
                return service;
            }
        };

        create = new ScCreate(activity);
    }

    private Map upload() throws Exception  {
        create.startUpload();
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(service).uploadTrack(captor.capture());
        return captor.getValue();
    }


    @Test
    public void testUploadParams() throws Exception {
        Map arguments = upload();

        assertEquals("recording", arguments.get("track[track_type]"));
        assertNotNull(arguments.get("track[title]"));

        assertNotNull(arguments.get(UploadTask.Params.PCM_PATH));
        assertNotNull(arguments.get(UploadTask.Params.OGG_FILENAME));
        assertNull(arguments.get(UploadTask.Params.ARTWORK_PATH));
    }

    @Test
    public void connectionsAreSet() throws Exception {
        Connection c1 = new Connection();
        c1.type = "Twitter";
        c1.post_publish = true;
        c1.id = 1000;


        Connection c2 = new Connection();
        c2.type = "Facebook";
        c2.post_publish = true;
        c2.id = 1001;

        Connection c3 = new Connection();
        c3.type = "Myspace";
        c3.post_publish = false;
        c3.id = 1002;

        create.mConnectionList.getAdapter().setConnections(Arrays.asList(c1, c2, c3));

        Map args = upload();
        assertTrue(args.get("post_to[][id]") instanceof List);
        List ids = (List) args.get("post_to[][id]");

        assertEquals(2, ids.size());
        assertEquals("1000", ids.get(0).toString());
        assertEquals("1001", ids.get(1).toString());
    }
}
