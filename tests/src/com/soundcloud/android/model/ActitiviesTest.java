package com.soundcloud.android.model;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Map;

@RunWith(DefaultTestRunner.class)
public class ActitiviesTest {

    @Test
    public void testIsEmpty() throws Exception {
        Activities activities = new Activities();
        assertThat(activities.isEmpty(), is(true));
        assertThat(activities.size(), is(0));
    }

    @Test
    public void testFull() throws Exception {
        Activities activities = new Activities(new Event(), new Event());
        assertThat(activities.isEmpty(), is(false));
        assertThat(activities.size(), is(2));
    }

    @Test
    public void testGetUniqueUsers() throws Exception {
        Activities activities = new Activities();
        assertThat(activities.getUniqueUsers().size(), is(0));
        Event e1 = new Event() { public User getUser() { return new User() { { id = 1; } }; } };
        Event e2 = new Event() { public User getUser() { return new User() { { id = 1; } }; } };
        Event e3 = new Event() { public User getUser() { return new User() { { id = 3; } }; } };
        activities = new Activities(e1, e2, e3);
        assertThat(activities.getUniqueUsers().size(), is(2));
    }

    @Test
    public void testGetUniqueTracks() throws Exception {
        Activities activities = new Activities();
        assertThat(activities.getUniqueTracks().size(), is(0));
        Event e1 = new Event() { public Track getTrack() { return new Track() { { id = 1; } }; } };
        Event e2 = new Event() { public Track getTrack() { return new Track() { { id = 1; } }; } };
        Event e3 = new Event() { public Track getTrack() { return new Track() { { id = 3; } }; } };
        activities = new Activities(e1, e2, e3);
        assertThat(activities.getUniqueTracks().size(), is(2));
    }

    @Test
    public void testFromJSON() throws Exception {
        Activities a = getActivities();
        assertThat(a.size(), is(42));
        assertThat(a.getUniqueTracks().size(), is(19));
        assertThat(a.getUniqueUsers().size(), is(29));
    }

    @Test
    public void testFavoritings() throws Exception {
        Activities favoritings = getActivities().favoritings();
        assertThat(favoritings.size(), is(27));
    }

    @Test
    public void testTracks() throws Exception {
        Activities tracks = getActivities().tracks();
        assertThat(tracks.size(), is(0));
    }

    @Test
    public void testSharings() throws Exception {
        Activities sharings = getActivities().sharings();
        assertThat(sharings.size(), is(0));
    }

    @Test
    public void testComments() throws Exception {
        Activities comments = getActivities().comments();
        assertThat(comments.size(), is(15));
    }

    @Test
    public void testGroupedByTrack() throws Exception {
        Map<Track,Activities> grouped = getActivities().groupedByTrack();
        assertThat(grouped.size(), is(19));
        for (Map.Entry<Track,Activities> entry : grouped.entrySet()) {
            assertThat(entry.getKey(), notNullValue());
            assertThat(entry.getValue().isEmpty(), is(false));
        }
    }

    @Test
    public void testOriginIsSetOnAllActivities() throws Exception {
        for (Event e : getActivities()) {
            assertThat(e.origin, not(CoreMatchers.<Object>nullValue()));
        }
    }

    @Test
    public void testGetCursor() throws Exception {
        Activities activities = new Activities();
        assertThat(activities.getCursor(), is(nullValue()));
        activities.next_href = "http://foo.com?cursor=dada";
        assertThat(activities.getCursor(), equalTo("dada"));
    }

    private Activities getActivities() throws IOException {
        return Activities.fromJSON(getClass().getResourceAsStream("activities.json"));
    }

    @Test
    public void testSubTypes() throws Exception {
        for (Event a : getActivities()) {
            if (a.isFavoriting()) {
                assertTrue(a.origin instanceof Favoriting);
            } else if (a.isComment()) {
                assertTrue(a.origin instanceof Comment);
            } else if (a.isTrack()) {
                assertTrue(a.origin instanceof Track);
            } else if (a.isTrackSharing()) {
                assertTrue(a.origin instanceof TrackSharing);
            }
        }
    }

    @Test
    public void testToJson() throws Exception {
        Activities initial = getActivities();
        assertThat(initial.future_href, not(nullValue()));
        assertThat(initial.next_href, not(CoreMatchers.<Object>nullValue()));

        String json = initial.toJSON();
        Activities other = Activities.fromJSON(json);
        for (int i=0; i<initial.size(); i++) {
            assertThat(initial.get(i), equalTo(other.get(i)));
        }
        assertThat(initial.size(), equalTo(other.size()));
        assertThat(initial.future_href, equalTo(other.future_href));
        assertThat(initial.next_href, equalTo(other.next_href));
    }
}
