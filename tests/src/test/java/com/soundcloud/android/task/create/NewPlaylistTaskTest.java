package com.soundcloud.android.task.create;


import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.addPendingHttpResponse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.soundcloud.android.TempEndpoints;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.sync.ApiSyncServiceTest;
import com.soundcloud.api.Request;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class NewPlaylistTaskTest {
    @Test
    public void shouldReturnPlaylist() throws Exception {
        addPendingHttpResponse(new TestHttpResponse(201, TestHelper.resourceAsBytes(ApiSyncServiceTest.class, "playlist.json")));
        NewPlaylistTask task = new NewPlaylistTask(DefaultTestRunner.application);

        Request r = Request.to(TempEndpoints.PLAYLISTS).add("playlist[title]", "new playlist");
        final Playlist playlist = task.doInBackground(r);
        assertNotNull(playlist);
        expect(playlist.id).toBeGreaterThan(0l);
    }

    @Test
    public void shouldReturnNullPlaylistInFailureCase() throws Exception {
        addPendingHttpResponse(400, "Failz");
        NewPlaylistTask task = new NewPlaylistTask(DefaultTestRunner.application);
        Request r = Request.to(TempEndpoints.PLAYLISTS).add("playlist[title]", "new playlist");
        final Playlist playlist = task.doInBackground(r);
        assertNull(playlist);
    }
}
