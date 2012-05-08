package com.soundcloud.android.activity;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.Actions;
import com.soundcloud.android.model.Connection;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Upload;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.service.record.ICloudCreateService;
import com.soundcloud.api.Params;
import com.xtremelabs.robolectric.annotation.DisableStrictI18n;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import android.content.Intent;
import android.net.Uri;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"ALL"})
@RunWith(DefaultTestRunner.class)
public class ScUploadTest implements Params.Track {
    ScUpload create;
    ICloudCreateService service;

    @Before
    public void setup() {
        service = mock(ICloudCreateService.class);
        create = new ScUpload();
        create.mCreateService = service;
        create.onCreate(null);
    }

    private Upload upload() throws Exception {
        return upload(false);
    }

    private Upload upload(boolean share) throws Exception {
        // 14:31:01, 15/02/2011
        File f = File.createTempFile("upload-test", "test");
        Calendar c = Calendar.getInstance();
        c.set(2001, 1, 15, 14, 31, 1);
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

        create.startUpload();

        ArgumentCaptor<Upload> captor = ArgumentCaptor.forClass(Upload.class);
        verify(service).startUpload(captor.capture());
        return captor.getValue();
    }


    @Test
    public void shouldOnlyGenerateSharingNoteWhenSharingPublicly() throws Exception {
        Upload upload = upload();
        expect(upload.sharing_note).toBeNull();
    }

    @Test
    public void shouldPassThroughAllRequiredTrackParams() throws Exception {
        Upload upload = upload();

        expect(upload.type).toEqual("recording");
        expect(upload.title).not.toBeNull();

        expect(upload.service_ids).toBeNull();
        expect(upload.post_to_empty).toEqual("");
        expect(PUBLIC).toEqual(upload.sharing);

        expect(upload.trackPath).not.toBeNull();
        expect(upload.artworkPath).toBeNull();
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

        Map args = upload().toTrackMap();
        expect(args.get(POST_TO) instanceof List).toBeTrue();
        List ids = (List) args.get(POST_TO);

        expect(2).toEqual(ids.size());
        expect("1000").toEqual(ids.get(0).toString());
        expect("1001").toEqual(ids.get(1).toString());
    }


    @Test
    public void shouldMapIntentToRecording() throws Exception {
        Intent i = new Intent(Actions.SHARE)
                .putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File("/tmp")))
                .putExtra(Actions.EXTRA_DESCRIPTION, "description")
                .putExtra(Actions.EXTRA_GENRE, "genre")
//                .putExtra(Actions.EXTRA_PUBLIC, false)
                .putExtra(Actions.EXTRA_TITLE, "title")
                .putExtra(Actions.EXTRA_WHERE, "where")
//                .putExtra(Actions.EXTRA_TAGS, new String[] { "tags" })
                ;

        Recording r = create.recordingFromIntent(i);
        expect(r).not.toBeNull();
        expect(r.description).toEqual("description");
        expect(r.genre).toEqual("genre");
//        assertThat(r.is_private, is(false));
        expect(r.where_text).toEqual("where");
        expect(r.what_text).toEqual("title");
//        assertThat(r.tags, equalTo(new String[] { "tags" } ));
    }
}
