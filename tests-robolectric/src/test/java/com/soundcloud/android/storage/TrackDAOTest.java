package com.soundcloud.android.storage;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiTrackTest;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.testsupport.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;

public class TrackDAOTest extends AbstractDAOTest<TrackDAO> {

    public TrackDAOTest() {
        super(new TrackDAO(Robolectric.application.getContentResolver()));
    }

    @Test
    public void shouldPersistAndLoadCorrectly() throws Exception {
        TestHelper.setUserId(100L);

        PublicApiTrack t = TestHelper.getObjectMapper().readValue(
                PublicApiTrackTest.class.getResourceAsStream("track.json"),
                PublicApiTrack.class);

        long id = getDAO().create(t);

        expect(id).toEqual(t.getId());
        expect(Content.TRACKS).toHaveCount(1);

        PublicApiTrack t2 = getDAO().queryById(id);
        expect(t2).not.toBeNull();

        compareTracks(t, t2);
        expect(t2.last_updated).toBeGreaterThan(t.last_updated);
        expect(t2.sharing).toEqual(t.sharing);
        expect(t2.state).toEqual(t.state);
    }

    private void compareTracks(PublicApiTrack t, PublicApiTrack t2) {
        expect(t2.getId()).toEqual(t.getId());
        expect(t2.title).toEqual(t.title);
        expect(t2.permalink).toEqual(t.permalink);
        expect(t2.duration).toBeGreaterThan(0L);
        expect(t2.duration).toEqual(t.duration);
        expect(t2.created_at).toEqual(t.created_at);
        expect(t2.tag_list).toEqual(t.tag_list);
        expect(t2.track_type).toEqual(t.track_type);
        expect(t2.permalink_url).toEqual(t.permalink_url);
        expect(t2.artwork_url).toEqual(t.artwork_url);
        expect(t2.waveform_url).toEqual(t.waveform_url);
        expect(t2.downloadable).toEqual(t.downloadable);
        expect(t2.download_url).toEqual(t.download_url);
        expect(t2.streamable).toEqual(t.streamable);
        expect(t2.stream_url).toEqual(t.stream_url);
        expect(t2.playback_count).toEqual(t.playback_count);
        expect(t2.download_count).toEqual(t.download_count);
        expect(t2.comment_count).toEqual(t.comment_count);
        expect(t2.likes_count).toEqual(t.likes_count);
        expect(t2.shared_to_count).toEqual(t.shared_to_count);
        expect(t2.user_id).toEqual(t.user_id);
        expect(t2.commentable).toEqual(t.commentable);
    }

    // tests from ScModelManager


    /*

    @Test
    public void shouldUpsertTrack() throws Exception {
        Track t1 = new Track();
        t1.id = 100L;
        t1.title = "testing";
        t1.user = new User();
        t1.user.id = 200L;
        t1.user.username = "Testor";

        Uri uri = t1.insert(resolver);

        expect(uri).not.toBeNull();
        Track t2 = manager.getTrack(uri);
        expect(t2).not.toBeNull();
        t2.title = "not interesting";

        t2.insert(resolver);

        Track t3 = manager.getTrack(uri);
        expect(t3.title).toEqual("not interesting");
    }



    @Test
    public void shouldInsertTrack() throws Exception {
        Track t1 = new Track();
        t1.id = 100L;
        t1.title = "testing";
        t1.user = new User();
        t1.user.id = 200L;
        t1.user.username = "Testor";

        Uri uri = t1.insert(resolver);

        expect(uri).not.toBeNull();
        Track t2 = manager.getTrack(uri);

        expect(t2).not.toBeNull();
        expect(t2.user).not.toBeNull();
        expect(t2.user.username).toEqual("Testor");
        expect(t1.title).toEqual(t2.title);
    }

    */


}
