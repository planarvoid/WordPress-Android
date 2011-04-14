package com.soundcloud.android.activity;

import static junit.framework.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.objects.Connection;
import com.soundcloud.android.objects.Recording;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.service.ICloudCreateService;
import com.soundcloud.android.task.UploadTask;
import com.soundcloud.api.CloudAPI;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"ALL"})
@RunWith(DefaultTestRunner.class)
public class ScUploadTests implements CloudAPI.TrackParams {
    ScUpload create;
    ICloudCreateService service;

    @Before
    public void setup() {
        service = mock(ICloudCreateService.class);
        create = new ScUpload();
        create.mCreateService = service;
        create.onCreate(null);
    }

    private Map upload() throws Exception {
        return upload(false);
    }

    private Map upload(boolean share) throws Exception {
        // 14:31:01, 15/02/2011
        File f = File.createTempFile("upload-test", "test");
        Calendar c = Calendar.getInstance();
        c.set(2001, 1, 15, 14, 31, 1);
        f.setLastModified(c.getTimeInMillis());

        create.setRecording(new Recording(f));

        if (share) {
            Connection c1 = new Connection();
            c1.service = "twitter";
            c1.post_publish = true;
            c1.id = 1000;
            create.mConnectionList.getAdapter().setConnections(Arrays.asList(c1));
        }
        create.startUpload();
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(service).uploadTrack(captor.capture());
        return captor.getValue();
    }

    // XXX
    // TODO: move tests to RecordingTests where appropriate

    @Test
    public void shouldUseUserTitleIfPresent() throws Exception {
        create.mWhatText.setText("my soundz");

        Map args = upload();

        assertEquals("my soundz", args.get(TITLE));
    }

    @Test
    public void shouldUseLocationIfPresent() throws Exception {
        create.mWhereText.setText("home");
        Map args = upload();
        assertEquals("Sounds from home", args.get(TITLE));
    }

    @Test
    public void shouldUseTitleAndLocationIfPresent() throws Exception {
        create.mWhatText.setText("my soundz");
        create.mWhereText.setText("home");
        Map args = upload();
        assertEquals("my soundz at home", args.get(TITLE));
    }

    @Test
    public void shouldGenerateANiceTitleIfNoUserInputPresent() throws Exception {
        Map args = upload();
        assertEquals("Sounds from Thursday afternoon", args.get(TITLE));
    }

    @Test
    public void shouldSetFoursquareVenueMachineTagIfPresent() throws Exception {
        create.setWhere("Foo", "123", 0.1, 0.2);
        Map args = upload();

        assertThat(args.get(TAG_LIST), not(is(nullValue())));

        List<String> tags = Arrays.asList(args.get(TAG_LIST).toString().split("\\s+"));

        assertThat(tags, hasItem("foursquare:venue=123"));
    }

    @Test
    public void shouldSetGeoMachineTags() throws Exception {
        create.setWhere("Foo", "123", 0.1, 0.2);
        Map args = upload();

        assertThat(args.get(TAG_LIST), not(is(nullValue())));

        List<String> tags = Arrays.asList(args.get(TAG_LIST).toString().split("\\s+"));
        assertThat(tags, hasItem("geo:lon=0.1"));
        assertThat(tags, hasItem("geo:lat=0.2"));
    }

    @Test
    public void shouldSetSourceMachineTag() throws Exception {
        Map args = upload();

        assertThat(args.get(TAG_LIST), not(is(nullValue())));
        List<String> tags = Arrays.asList(args.get(TAG_LIST).toString().split("\\s+"));
        assertThat(tags, hasItem("soundcloud:source=android-record"));
    }


    @Test
    public void shouldOnlyGenerateSharingNoteWhenSharingPublicly() throws Exception {
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

        assertNotNull(arguments.get(UploadTask.Params.SOURCE_PATH));
        //assertNotNull(arguments.get(UploadTask.Params.OGG_FILENAME));
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
}
