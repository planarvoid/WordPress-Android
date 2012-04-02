package com.soundcloud.android.activity;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Connection;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Upload;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.service.record.CloudCreateService;
import com.soundcloud.api.Params;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.annotation.DisableStrictI18n;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ComponentName;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

@RunWith(DefaultTestRunner.class)
public class ScUploadTest  {
    ScUpload create;

    @Before
    public void setup() {
        Robolectric.shadowOf(Robolectric.application).setComponentNameAndServiceForBindService(
                new ComponentName(Robolectric.application, CloudCreateService.class),
                new CloudCreateService().getBinder()
        );

        create = new ScUpload();
        create.onCreate(null);
        create.onStart();
    }

    private Upload upload() throws Exception {
        return upload(false);
    }

    private Upload upload(boolean share) throws Exception {
        // 14:31:01, 15/02/2011
        File f = File.createTempFile("upload-test", "test");
        Calendar c = Calendar.getInstance();
        //noinspection MagicConstant
        c.set(2001, 1, 15, 14, 31, 1);
        //noinspection ResultOfMethodCallIgnored
        f.setLastModified(c.getTimeInMillis());

        Recording r = new Recording(f);
        create.setRecording(r);

        if (share) {
            Connection c1 = new Connection();
            c1.service = "twitter";
            c1.post_publish = true;
            c1.id = 1000;
            create.mConnectionList.getAdapter().setConnections(Arrays.asList(c1));
        }
        // don't actually try to upload
        Robolectric.getBackgroundScheduler().pause();
        return create.startUpload();
    }

    @Test
    public void shouldOnlyGenerateSharingNoteWhenSharingPublicly() throws Exception {
        Upload upload = upload();
        expect(upload).not.toBeNull();
        expect(upload.sharing_note).toBeNull();
    }

    @Test @DisableStrictI18n
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

        final Upload upload = upload();
        expect(upload).not.toBeNull();
        Map args = upload.toTrackMap();
        expect(args.get(Params.Track.POST_TO) instanceof List).toBeTrue();
        List ids = (List) args.get(Params.Track.POST_TO);

        expect(ids.size()).toEqual(2);
        expect(ids.get(0).toString()).toEqual("1000");
        expect(ids.get(1).toString()).toEqual("1001");
    }
}
