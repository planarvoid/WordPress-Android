package com.soundcloud.android.dao;

import android.net.Uri;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackTest;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;

import static com.soundcloud.android.Expect.expect;

public class TrackDAOTest extends BaseDAOTest<TrackDAO> {

    public TrackDAOTest() {
        super(new TrackDAO(Robolectric.application.getContentResolver()));
    }

    @Test
    public void shouldPersistAndLoadCorrectly() throws Exception {
        DefaultTestRunner.application.setCurrentUserId(100L);

        Track t = TestHelper.getObjectMapper().readValue(
                TrackTest.class.getResourceAsStream("track.json"),
                Track.class);

        Uri uri = TrackDAO.insert(t, resolver);
        expect(uri).not.toBeNull();
        expect(Content.TRACKS).toHaveCount(1);

        Track t2 = TrackDAO.fromUri(uri, resolver, false);

        compareTracks(t, t2);
        expect(t2.last_updated).toBeGreaterThan(t.last_updated);
        expect(t2.sharing).toEqual(t.sharing);
        expect(t2.state).toEqual(t.state);
    }

    private void compareTracks(Track t, Track t2) {
        expect(t2.id).toEqual(t.id);
        expect(t2.title).toEqual(t.title);
        expect(t2.permalink).toEqual(t.permalink);
        expect(t2.duration).toBeGreaterThan(0);
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
}
