package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.api.Params;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;


@RunWith(DefaultTestRunner.class)
public class UploadTest {
    Recording r;

    @Before
    public void setup() throws Exception {
        r = new Recording(new File("/tmp/foo"));
        // 14:31:01, 15/02/2011
        Calendar c = Calendar.getInstance();
        c.set(2001, 1, 15, 14, 31, 1);
        r.timestamp = c.getTimeInMillis();
        r.service_ids = "1,2,3";
        r.duration = 86 * 1000;
    }

    private Upload upload() {
        return new Upload(r, Robolectric.application.getResources());
    }

    @Test
    public void shouldAddTagsToUploadData() throws Exception {
        r.tags = new String[] { "foo baz", "bar", "baz" };
        String tags = String.valueOf(upload().tag_list);
        expect(tags).toEqual("\"foo baz\" bar baz soundcloud:source=android-record");

        r.private_user_id = 12;
        expect(tags).toEqual("\"foo baz\" bar baz soundcloud:source=android-record");
    }

    @Test
    public void shouldAddDedicatedTagIfPrivateMessage() throws Exception {
        r.private_user_id = 10;
        expect(String.valueOf(upload().tag_list)).toMatch("soundcloud:recording-type=dedicated");
    }

    @Test
    public void shouldAddDescriptionToUploadData() throws Exception {
        r.description = "foo";
        expect(upload().toTrackMap().get(Params.Track.DESCRIPTION).toString()).toEqual("foo");
    }

    @Test
    public void shouldAddGenreToUploadData() throws Exception {
        r.genre = "foo";
        expect(upload().toTrackMap().get(Params.Track.GENRE).toString()).toEqual("foo");
    }

    @Test
    public void shouldAddFoursquareMachineTags() throws Exception {
        r.four_square_venue_id = "abcdef";
        expect(upload().getTags()).toContain("foursquare:venue=abcdef");
    }

    @Test
    public void shouldSetADifferentMachineTagWhenDoing3rdPartyUpload() throws Exception {
        r.external_upload = true;
        String tags = String.valueOf(upload().toTrackMap().get(Params.Track.TAG_LIST));
        List<String> tagList = Arrays.asList(tags.split("\\s+"));
        expect(tagList).toContain("soundcloud:source=android-3rdparty-upload");
        expect(tagList).not.toContain("soundcloud:source=android-record");
    }

    @Test
    public void shouldSetSourceMachineTag() throws Exception {
        expect(upload().getTags()).toContain("soundcloud:source=android-record");
    }

    @Test
    public void shouldSetGeoMachineTags() throws Exception {
        r.longitude = 0.1d;
        r.latitude = 0.2d;
        List<String> tags = upload().getTags();
        expect(tags).toContain("geo:lon=0.1");
        expect(tags).toContain("geo:lat=0.2");
    }

    @Test
    public void shouldPassThroughAllRequiredTrackParams() throws Exception {
        Upload upload = upload();

        expect(upload).not.toBeNull();
        expect(upload.type).toEqual("recording");
        expect(upload.title).not.toBeNull();

        expect(upload.service_ids).toEqual("1,2,3");
        expect(upload.post_to_empty).toBeNull();
        expect(upload.sharing).toEqual(Params.Track.PUBLIC);

        expect(upload.soundFile).not.toBeNull();
        expect(upload.artworkFile).toBeNull();
    }


    @Test
    public void shouldOnlyGenerateSharingNoteWhenSharingPublicly() throws Exception {
        r.is_private = true;
        Upload upload = upload();
        expect(upload).not.toBeNull();
        expect(upload.sharing_note).toBeNull();
    }
}
