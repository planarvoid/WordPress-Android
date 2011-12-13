package com.soundcloud.android.task;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;


@RunWith(DefaultTestRunner.class)
public class LoadCollectionTaskTest {
    /*@Test
    public void shouldDeserializeTracks() throws Exception {
        LoadCollectionTask task = new LoadCollectionTask(DefaultTestRunner.application, Track.class);

        List<Parcelable> items = new ArrayList<Parcelable>();
        task.getCollectionFromStream(getClass().getResourceAsStream("tracks.json"), items);

        assertThat(items, not(nullValue()));
        assertThat(items.size(), equalTo(11));

        Track t = (Track) items.get(0);
        assertThat(t.description, is(nullValue()));
        assertThat(t.id, is(10853436L));
        assertThat(t.user_id, is(3135930L));
        assertThat(t.waveform_url, not(nullValue()));
        assertThat(t.stream_url, not(nullValue()));
        assertThat(t.artwork_url, is(nullValue()));
    }

    @Test
    public void shouldDeserializeUsers() throws Exception {
        LoadCollectionTask task = new LoadCollectionTask(DefaultTestRunner.application, User.class, pageIndex, refresh);

        List<Parcelable> items = new ArrayList<Parcelable>();
        task.getCollectionFromStream(getClass().getResourceAsStream("users.json"), items);

        assertThat(items, not(nullValue()));
        assertThat(items.size(), equalTo(1));

        User u = (User) items.get(0);
        assertThat(u.description, is(nullValue()));
        assertThat(u.id, is(3135930L));
    }

    @Test
    public void shouldDeserializeEvents() throws Exception {
        LoadCollectionTask task = new LoadCollectionTask(DefaultTestRunner.application, Event.class, pageIndex, refresh);
        List<Parcelable> items = new ArrayList<Parcelable>();
        task.getCollectionFromStream(getClass().getResourceAsStream("events.json"), items);

        assertThat(items.size(), equalTo(50));

        Event e = (Event) items.get(0);
        assertThat(e.created_at, not(nullValue()));
        // origin=comment
        assertThat(e.getComment().id, is(21266451L));
        // origin=favoriting
        Event e2 = (Event) items.get(2);
        assertThat(e2.getTrack().id, is(12725662L));
        // origin=track_sharing
        Event e3 = (Event) items.get(3);
        assertThat(e3.getTrack().id, is(19321606L));
        // origin=track
        Event e4 = (Event) items.get(4);
        assertThat(e4.getTrack().id, is(19318826L));
    }*/
}
