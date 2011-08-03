package com.soundcloud.android.task;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.adapter.EventsAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.adapter.UserlistAdapter;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.api.Request;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


@RunWith(DefaultTestRunner.class)
// XXX should extend ApiTests and test against AndroidCloudAPI
public class AppendTaskTest {
    @Test
    public void shouldDeserializeTracks() throws Exception {
        AppendTask task = new AppendTask(DefaultTestRunner.application);
        LazyEndlessAdapter adapter = new LazyEndlessAdapter(null, new TracklistAdapter(null, null, Track.class), null);

        task.setAdapter(adapter);

        Robolectric.addPendingHttpResponse(200, slurp("tracks.json"));
        task.doInBackground(Request.to("http://foo.com"));

        assertThat(task.newItems, not(nullValue()));
        assertThat(task.newItems.size(), equalTo(11));

        Track t = (Track) task.newItems.get(0);
        assertThat(t.description, is(nullValue()));
        assertThat(t.id, is(10853436L));
        assertThat(t.user_id, is(3135930L));
        assertThat(t.waveform_url, not(nullValue()));
        assertThat(t.stream_url, not(nullValue()));
        assertThat(t.artwork_url, is(nullValue()));
    }

    @Test
    public void shouldDeserializeUsers() throws Exception {
        AppendTask task = new AppendTask(DefaultTestRunner.application);

        LazyEndlessAdapter adapter = new LazyEndlessAdapter(null, new UserlistAdapter(null, null, User.class), null);

        task.setAdapter(adapter);

        Robolectric.addPendingHttpResponse(200, slurp("users.json"));
        task.doInBackground(Request.to("http://foo.com"));

        assertThat(task.newItems, not(nullValue()));
        assertThat(task.newItems.size(), equalTo(1));

        User u = (User) task.newItems.get(0);
        assertThat(u.description, is(nullValue()));
        assertThat(u.id, is(3135930L));
    }

    @Test
    public void shouldDeserializeEvents() throws Exception {
        AppendTask task = new AppendTask(DefaultTestRunner.application);
        LazyEndlessAdapter adapter = new LazyEndlessAdapter(null, new EventsAdapter(null, null, false, Event.class), null);

        task.setAdapter(adapter);

        Robolectric.addPendingHttpResponse(200, slurp("events.json"));
        task.doInBackground(Request.to("http://foo.com"));

        assertThat(task.newItems, not(nullValue()));
        assertThat(task.newItems.size(), equalTo(50));

        Event e = (Event) task.newItems.get(0);
        assertThat(e.created_at, not(nullValue()));
        // origin=comment
        assertThat(e.comment.id, is(21266451L));
        // origin=favoriting
        Event e2 = (Event) task.newItems.get(2);
        assertThat(e2.track.id, is(12725662L));
        // origin=track_sharing
        Event e3 = (Event) task.newItems.get(3);
        assertThat(e3.track.id, is(19321606L));
        // origin=track
        Event e4 = (Event) task.newItems.get(4);
        assertThat(e4.track.id, is(19318826L));
    }

    private String slurp(String res) throws IOException {
        StringBuilder sb = new StringBuilder(65536);
        int n;
        byte[] buffer = new byte[8192];
        InputStream is = getClass().getResourceAsStream(res);
        if (is == null) {
            throw new FileNotFoundException("readFilesInBytes: File " + res
                    + " does not exist");
        }
        while ((n = is.read(buffer)) != -1) sb.append(new String(buffer, 0, n));
        return sb.toString();
    }
}
