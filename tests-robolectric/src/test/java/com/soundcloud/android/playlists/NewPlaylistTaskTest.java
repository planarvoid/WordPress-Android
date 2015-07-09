package com.soundcloud.android.playlists;


import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.addPendingHttpResponse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.soundcloud.android.api.legacy.TempEndpoints;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.sync.ApiSyncServiceTest;
import com.soundcloud.android.testsupport.fixtures.JsonFixtures;
import com.soundcloud.android.api.legacy.Request;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class NewPlaylistTaskTest {
    @Test
    public void shouldReturnPlaylist() throws Exception {
        addPendingHttpResponse(new TestHttpResponse(201, JsonFixtures.resourceAsBytes(ApiSyncServiceTest.class, "playlist.json")));
        NewPlaylistTask task = new NewPlaylistTask(DefaultTestRunner.application.getCloudAPI());

        Request r = Request.to(TempEndpoints.PLAYLISTS).add("playlist[title]", "new playlist");
        final PublicApiPlaylist playlist = task.doInBackground(r);
        assertNotNull(playlist);
        expect(playlist.getId()).toBeGreaterThan(0l);
    }

    @Test
    public void shouldReturnNullPlaylistInFailureCase() throws Exception {
        addPendingHttpResponse(400, "Failz");
        NewPlaylistTask task = new NewPlaylistTask(DefaultTestRunner.application.getCloudAPI());
        Request r = Request.to(TempEndpoints.PLAYLISTS).add("playlist[title]", "new playlist");
        final PublicApiPlaylist playlist = task.doInBackground(r);
        assertNull(playlist);
    }
}
