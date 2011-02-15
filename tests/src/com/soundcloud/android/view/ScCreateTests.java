package com.soundcloud.android.view;

import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.LazyActivity;
import com.soundcloud.android.objects.Connection;
import com.soundcloud.android.service.ICloudCreateService;
import com.soundcloud.android.task.UploadTask;
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
public class ScCreateTests
        implements CloudAPI.Params {
    ScCreate create;
    ICloudCreateService service;

    @Before
    public void setup() {
        service = mock(ICloudCreateService.class);
        addPendingHttpResponse(401, "Error");  // load connections

        final LazyActivity activity = new LazyActivity() {
            @Override
            public SoundCloudApplication getSoundCloudApplication() {
                return new SoundCloudApplication() {
                    {
                        onCreate();
                    }

                    @Override
                    public String getConsumerKey(boolean b) {
                        return "xxx";
                    }

                    @Override
                    public String getConsumerSecret(boolean b) {
                        return "xxx";
                    }
                };
            }

            @Override
            public ICloudCreateService getCreateService() {
                return service;
            }
        };

        create = new ScCreate(activity);
    }

    private Map upload() throws Exception {
        // 14:31:01, 15/02/2011
        create.mRecordingStarted.set(1, 31, 14, 15, 2, 2011);
        create.startUpload();
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(service).uploadTrack(captor.capture());
        return captor.getValue();
    }

    @Test
    public void shouldUseUserTitleIfPresent() throws Exception {
        create.mWhatText.setText("my soundz");

        Map args = upload();

        assertEquals("my soundz", args.get(TITLE));
        assertTrue(args.get(UploadTask.Params.OGG_FILENAME).toString().contains("my soundz"));
    }

    @Test
    public void shouldUseLocationIfPresent() throws Exception {
        create.mWhereText.setText("home");
        Map args = upload();
        assertEquals("home", args.get(TITLE));
        assertTrue(args.get(UploadTask.Params.OGG_FILENAME).toString().contains("home"));
    }

    @Test
    public void shouldUseTitleAndLocationIfPresent() throws Exception {
        create.mWhatText.setText("my soundz");
        create.mWhereText.setText("home");
        Map args = upload();
        assertEquals("my soundz at home", args.get(TITLE));
        assertTrue(args.get(UploadTask.Params.OGG_FILENAME).toString().contains("my soundz at home"));
    }

    @Test
    public void shouldGenerateANiceTitleIfNoUserInputPresent() throws Exception {
        Map args = upload();
        assertEquals("recording on null morning", args.get(TITLE));
    }

    @Test
    public void shouldGenerateASharingNote() throws Exception {
        Connection c1 = new Connection();
        c1.service = "twitter";
        c1.post_publish = true;
        c1.id = 1000;
        create.mConnectionList.getAdapter().setConnections(Arrays.asList(c1));

        Map args = upload();

        assertNotNull("A sharing note should be present", args.get(SHARING_NOTE));
        assertEquals("Sounds from null morning", args.get(SHARING_NOTE));


    }

    @Test
    public void shouldOnlyGenerateSharingNoteWhenSharingPublicly() throws Exception {
        Connection c1 = new Connection();
        c1.service = "twitter";
        c1.post_publish = false;
        c1.id = 1000;
        create.mConnectionList.getAdapter().setConnections(Arrays.asList(c1));
        Map args = upload();
        assertNull("A sharing note should not be present", args.get(SHARING_NOTE));
    }

    @Test
    public void shouldPassThroughAllRequiredTrackParams() throws Exception {
        Map arguments = upload();

        assertEquals("recording", arguments.get(TYPE));
        assertNotNull(arguments.get(TITLE));

        assertNull(arguments.get(POST_TO));
        assertNotNull(arguments.get(POST_TO_EMPTY));
        assertEquals("", arguments.get(POST_TO_EMPTY));
        assertEquals(PUBLIC, arguments.get(SHARING));

        assertNotNull(arguments.get(UploadTask.Params.PCM_PATH));
        assertNotNull(arguments.get(UploadTask.Params.OGG_FILENAME));
        assertNull(arguments.get(UploadTask.Params.ARTWORK_PATH));
    }

    @Test
    public void shouldSetAllEnabledConnections() throws Exception {
        Connection c1 = new Connection();
        c1.service = "twitter";
        c1.post_publish = true;
        c1.id = 1000;


        Connection c2 = new Connection();
        c2.service = "facebook_profile";
        c2.post_publish = true;
        c2.id = 1001;

        Connection c3 = new Connection();
        c3.service = "myspace";
        c3.post_publish = false;
        c3.id = 1002;

        create.mConnectionList.getAdapter().setConnections(Arrays.asList(c1, c2, c3));

        Map args = upload();
        assertTrue(args.get(POST_TO) instanceof List);
        List ids = (List) args.get(POST_TO);

        assertEquals(2, ids.size());
        assertEquals("1000", ids.get(0).toString());
        assertEquals("1001", ids.get(1).toString());
    }

    @Test
    public void shouldSetTheListOfEmailAddressesWithAccess() throws Exception {
        create.mRdoPrivate.performClick();
        create.mAccessList.getAdapter().setAccessList(Arrays.asList("foo@bar.com", "bla@example.com"));

        Map args = upload();

        assertTrue(args.get(SHARED_EMAILS) instanceof List);
        assertEquals(2, ((List) args.get(SHARED_EMAILS)).size());

        assertEquals("foo@bar.com", ((List) args.get(SHARED_EMAILS)).get(0));
        assertEquals("bla@example.com", ((List) args.get(SHARED_EMAILS)).get(1));
    }
}
